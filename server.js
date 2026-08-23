const express = require('express');
const http = require('http');
const { Server } = require("socket.io");
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const sqlite3 = require('sqlite3').verbose();
const cors = require('cors');

const app = express();
app.use(express.json());
app.use(cors());

const server = http.createServer(app);
const io = new Server(server, { cors: { origin: "*" } });

const SECRET_KEY = "ultra_secret_key_123";

// --- DATABASE SETUP ---
const db = new sqlite3.Database('./app_database.db', (err) => {
    if (err) console.error("DB Error:", err.message);
    else console.log("SQLite Connected ✅");
});

db.run(`CREATE TABLE IF NOT EXISTS users (
    email TEXT PRIMARY KEY, password TEXT, name TEXT, age TEXT, gender TEXT, is_vip INTEGER DEFAULT 0
)`);

// --- REAL-TIME PRESENCE DATA ---
// Isme hum track karenge ki kaunsa user kis mode par active hai
let onlineRegistry = {
    text: {},
    audio: {},
    video: {}
};

// --- ONLINE COUNTS API (For Home and Selection Page) ---
app.get('/online-counts', (req, res) => {
    const { mode, hwId } = req.query;
    const now = Date.now();

    // 1. Agar user Matching page par hai, toh uska timestamp update karo (Heartbeat)
    if (mode && hwId && onlineRegistry[mode]) {
        onlineRegistry[mode][hwId] = now;
    }

    // 2. Har mode ke liye purane users (jo 10 sec se inactive hain) unhe hatao aur fresh count nikalo
    const counts = {};
    ["text", "audio", "video"].forEach(m => {
        const activeUsers = Object.keys(onlineRegistry[m]).filter(id => {
            if (now - onlineRegistry[m][id] > 10000) { // 10 second timeout
                delete onlineRegistry[m][id];
                return false;
            }
            return true;
        });
        counts[m] = activeUsers.length;
    });

    // Jis mode ka pucha gaya hai uska count bhej do
    res.json({ count: counts[mode] || 0, all: counts });
});

// --- AUTH APIs (Signup/Login) ---
app.post('/signup', async (req, res) => {
    const { email, password, name, age, gender } = req.body;
    const hashed = await bcrypt.hash(password, 10);
    db.run(`INSERT INTO users (email, password, name, age, gender) VALUES (?, ?, ?, ?, ?)`,
        [email, hashed, name, age, gender], (err) => {
            if (err) return res.status(400).json({ msg: "Email already exists" });
            res.json({ token: jwt.sign({ email }, SECRET_KEY), user: { email, name, age, gender } });
        });
});

app.post('/login', async (req, res) => {
    const { email, password } = req.body;
    db.get(`SELECT * FROM users WHERE email = ?`, [email], async (err, user) => {
        if (!user || !(await bcrypt.compare(password, user.password))) 
            return res.status(401).json({ msg: "Invalid credentials" });
        res.json({ token: jwt.sign({ email }, SECRET_KEY), user: { email, name: user.name, gender: user.gender } });
    });
});

// --- UNIFIED MATCHING ENGINE ---
let queues = { audio: [], video: [], chat: [] };
let pairs = {}; 

io.on("connection", (socket) => {
    console.log("New User Connected:", socket.id);

    socket.on("register_user", (data) => {
        socket.userId = data.userId;
        socket.gender = data.gender || "Male";
    });

    socket.on("find_buddy", (data) => {
        const mode = data.mode; // 'audio', 'video', or 'chat'
        const pref = data.prefGender || "Any";
        
        Object.keys(queues).forEach(m => queues[m] = queues[m].filter(id => id !== socket.id));

        let partnerIndex = queues[mode].findIndex(id => {
            let u = io.sockets.sockets.get(id);
            return u && id !== socket.id && (pref === "Any" || u.gender === pref);
        });

        if (partnerIndex !== -1) {
            let partnerId = queues[mode].splice(partnerIndex, 1)[0];
            pairs[socket.id] = partnerId;
            pairs[partnerId] = socket.id;

            io.to(socket.id).emit("matched", { partnerId: partnerId, initiator: true });
            io.to(partnerId).emit("matched", { partnerId: socket.id, initiator: false });
            console.log(`Matched [${mode}]: ${socket.id} <-> ${partnerId}`);
        } else {
            if (!queues[mode].includes(socket.id)) queues[mode].push(socket.id);
            socket.emit("waiting");
        }
    });

    socket.on("webrtc_signal", (data) => {
        const partnerId = pairs[socket.id];
        if (partnerId) io.to(partnerId).emit("webrtc_signal", { signalData: data.signalData });
    });

    socket.on("send_chat", (data) => {
        const partnerId = pairs[socket.id];
        if (partnerId) io.to(partnerId).emit("receive_chat", { message: data.message });
    });

    socket.on("app_error_log", (data) => {
        console.log("\n!!! FATAL ERROR FROM APP !!!");
        console.log(`User/Socket ID: ${socket.id}`);
        console.log("Error Detail:", data.error);
        console.log("-----------------------------\n");
    });
    // --- REAL-TIME PAGE PRESENCE ---
    socket.on('join_page', (pageName) => {
        socket.join(pageName);
        const count = io.sockets.adapter.rooms.get(pageName)?.size || 0;
        io.to(pageName).emit('page_user_count', count);
    });

    socket.on('leave_page', (pageName) => {
        socket.leave(pageName);
        const count = io.sockets.adapter.rooms.get(pageName)?.size || 0;
        io.to(pageName).emit('page_user_count', count);
    });

    socket.on('disconnecting', () => {
        socket.rooms.forEach(room => {
            if (room.endsWith('_page')) {
                const count = (io.sockets.adapter.rooms.get(room)?.size || 1) - 1;
                io.to(room).emit('page_user_count', count);
            }
        });
    });
    socket.on("disconnect", () => {
        Object.keys(queues).forEach(m => queues[m] = queues[m].filter(id => id !== socket.id));
        let pId = pairs[socket.id];
        if (pId) {
            io.to(pId).emit("buddy_left");
            delete pairs[pId];
        }
        delete pairs[socket.id];
    });
});

server.listen(3000, '0.0.0.0', () => console.log("Master Server Ready on 3000"));
