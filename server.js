const express = require('express');
const http = require('http');
const { Server } = require("socket.io");
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const sqlite3 = require('sqlite3').verbose(); // Professional DB
const cors = require('cors');

const app = express();
app.use(express.json());
app.use(cors());

const server = http.createServer(app);
const io = new Server(server, { cors: { origin: "*" } });

const SECRET_KEY = "ultra_secret_key_123";

// --- SQLITE DATABASE SETUP ---
const db = new sqlite3.Database('./app_database.db', (err) => {
    if (err) console.error("Database Error:", err.message);
    else console.log("Connected to SQLite Database ✅");
});

// Create Users Table if not exists
db.run(`CREATE TABLE IF NOT EXISTS users (
    email TEXT PRIMARY KEY,
    password TEXT,
    name TEXT,
    age TEXT,
    gender TEXT,
    is_vip INTEGER DEFAULT 0
)`);

// --- AUTH APIs (SQLite Powered) ---

app.post('/signup', async (req, res) => {
    const { email, password, name, age, gender } = req.body;
    const hashedPassword = await bcrypt.hash(password, 10);

    db.run(`INSERT INTO users (email, password, name, age, gender) VALUES (?, ?, ?, ?, ?)`,
        [email, hashedPassword, name, age, gender],
        function(err) {
            if (err) {
                if (err.message.includes("UNIQUE")) return res.status(400).json({ msg: "Email already registered" });
                return res.status(500).json({ msg: "Database Error" });
            }
            const token = jwt.sign({ email }, SECRET_KEY);
            res.json({ token, user: { email, name, age, gender } });
        }
    );
});

app.post('/login', async (req, res) => {
    const { email, password } = req.body;
    db.get(`SELECT * FROM users WHERE email = ?`, [email], async (err, user) => {
        if (err) return res.status(500).json({ msg: "Database Error" });
        if (!user) return res.status(404).json({ msg: "Please register first" });

        const isMatch = await bcrypt.compare(password, user.password);
        if (!isMatch) return res.status(401).json({ msg: "User or password mismatch" });

        const token = jwt.sign({ email }, SECRET_KEY);
        res.json({ token, user: { email, name: user.name, age: user.age, gender: user.gender } });
    });
});

// --- CALLING ENGINE (OMEGLE STYLE) ---
let onlineUsers = new Map(); 
let waitingQueue = [];      

io.on("connection", (socket) => {
    socket.on("register_user", (data) => {
        socket.userId = data.userId;
        socket.gender = data.gender;
        onlineUsers.set(data.userId, { socketId: socket.id, name: data.userName, gender: data.gender, userId: data.userId });
        io.emit("online_users_list", Array.from(onlineUsers.values()));
    });

    socket.on("find_stranger", (data) => {
        const pref = data.prefGender;
        let partnerId = waitingQueue.find(id => {
            let u = io.sockets.sockets.get(id);
            return u && id !== socket.id && (pref === "Any" || u.gender === pref);
        });

        if (partnerId) {
            waitingQueue = waitingQueue.filter(id => id !== partnerId);
            io.to(socket.id).emit("stranger_matched", { peerSocketId: partnerId, isInitiator: true });
            io.to(partnerId).emit("stranger_matched", { peerSocketId: socket.id, isInitiator: false });
        } else {
            if (!waitingQueue.includes(socket.id)) waitingQueue.push(socket.id);
        }
    });

    socket.on("webrtc_signal", (data) => {
        io.to(data.targetSocketId).emit("webrtc_signal", { senderSocketId: socket.id, signalData: data.signalData });
    });

    socket.on("disconnect", () => {
        waitingQueue = waitingQueue.filter(id => id !== socket.id);
        if (socket.userId) onlineUsers.delete(socket.userId);
        io.emit("online_users_list", Array.from(onlineUsers.values()));
    });
});

server.listen(3000, '0.0.0.0', () => console.log("Enterprise SQLite Server Ready"));
