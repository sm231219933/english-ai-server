const express = require('express');
const http = require('http');
const { Server } = require("socket.io");
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const sqlite3 = require('sqlite3').verbose();
const cors = require('cors');
const AWS = require('aws-sdk'); // Naya import

const app = express();
app.use(express.json());
app.use(cors());

const server = http.createServer(app);
const io = new Server(server, { cors: { origin: "*" } });

const SECRET_KEY = "ultra_secret_key_123";

// --- AWS DYNAMODB SETUP ---
AWS.config.update({ region: 'us-east-1' }); // Mumbai hai toh 'ap-south-1' karein
const dynamoDB = new AWS.DynamoDB.DocumentClient();
const TABLE_NAME = 'tinklusers';

// --- DATABASE SETUP (SQLite) ---
const db = new sqlite3.Database('./app_database.db', (err) => {
    if (err) console.error("DB Error:", err.message);
    else console.log("SQLite Connected ✅");
});

db.run(`CREATE TABLE IF NOT EXISTS users (
    email TEXT PRIMARY KEY, password TEXT, name TEXT, age TEXT, gender TEXT, is_vip INTEGER DEFAULT 0
)`);

// --- REAL-TIME PRESENCE DATA ---
let onlineRegistry = { text: {}, audio: {}, video: {} };

// --- ONLINE COUNTS API ---
app.get('/online-counts', (req, res) => {
    const { mode, hwId } = req.query;
    const now = Date.now();
    if (mode && hwId && onlineRegistry[mode]) { onlineRegistry[mode][hwId] = now; }
    const counts = {};
    ["text", "audio", "video"].forEach(m => {
        const activeUsers = Object.keys(onlineRegistry[m]).filter(id => {
            if (now - onlineRegistry[m][id] > 10000) { delete onlineRegistry[m][id]; return false; }
            return true;
        });
        counts[m] = activeUsers.length;
    });
    res.json({ count: counts[mode] || 0, all: counts });
});

// --- NEW: CHECK USER (For Professional Auto-Fill) ---
app.get('/check-user', async (req, res) => {
    const { email } = req.query;
    console.log("--> Checking AWS for user:", email);
    
    const params = { TableName: TABLE_NAME, Key: { "email": email } };
    try {
        const data = await dynamoDB.get(params).promise();
        if (data.Item) {
            console.log("SUCCESS: User found in AWS");
            res.json({ 
                exists: true, 
                user: { name: data.Item.name, age: data.Item.age, gender: data.Item.gender } 
            });
        } else {
            res.json({ exists: false });
        }
    } catch (err) {
        console.error("AWS Error:", err.message);
        res.status(500).json({ msg: "Server Error" });
    }
});

// --- AUTH APIs (Updated with AWS Sync) ---
app.post('/signup', async (req, res) => {
    const { email, password, name, age, gender } = req.body;
    console.log("--> New Signup Request for:", email);

    const hashed = await bcrypt.hash(password, 10);

    // 1. Save to SQLite (Local Backup)
    db.run(`INSERT INTO users (email, password, name, age, gender) VALUES (?, ?, ?, ?, ?)`,
        [email, hashed, name, age, gender], async (err) => {
            if (err) {
                console.log("SQLite Error (User might exist)");
            }
        });

    // 2. Save to AWS DynamoDB (Source of Truth)
    const params = {
        TableName: TABLE_NAME,
        Item: { email, password: hashed, name, age, gender, isPaid: false, createdAt: new Date().toISOString() }
    };

    try {
        await dynamoDB.put(params).promise();
        console.log("SUCCESS: Saved to AWS DynamoDB");
        res.json({ token: jwt.sign({ email }, SECRET_KEY), user: { email, name, age, gender } });
    } catch (err) {
        console.error("AWS Save Fail:", err.message);
        res.status(500).json({ msg: "AWS Sync Failed" });
    }
});

app.post('/login', async (req, res) => {
    const { email, password } = req.body;
    console.log("--> Login attempt:", email);

    // Try AWS First for Professional Sync
    const params = { TableName: TABLE_NAME, Key: { "email": email } };
    try {
        const data = await dynamoDB.get(params).promise();
        if (data.Item && (await bcrypt.compare(password, data.Item.password))) {
            console.log("SUCCESS: Login via AWS");
            return res.json({ 
                token: jwt.sign({ email }, SECRET_KEY), 
                user: { email, name: data.Item.name, age: data.Item.age, gender: data.Item.gender } 
            });
        }
        
        // Fallback to SQLite
        db.get(`SELECT * FROM users WHERE email = ?`, [email], async (err, user) => {
            if (user && (await bcrypt.compare(password, user.password))) {
                console.log("SUCCESS: Login via SQLite");
                res.json({ token: jwt.sign({ email }, SECRET_KEY), user: { email, name: user.name, gender: user.gender } });
            } else {
                res.status(401).json({ msg: "Invalid credentials" });
            }
        });
    } catch (err) {
        res.status(500).json({ msg: "Login error" });
    }
});

// --- UNIFIED MATCHING ENGINE (Socket.IO) ---
let queues = { audio: [], video: [], chat: [] };
let pairs = {}; 

io.on("connection", (socket) => {
    socket.on("register_user", (data) => {
        socket.userId = data.userId;
        socket.gender = data.gender || "Male";
    });

    socket.on("find_buddy", (data) => {
        const mode = data.mode;
        const pref = data.prefGender || "Any";
        Object.keys(queues).forEach(m => queues[m] = queues[m].filter(id => id !== socket.id));
        let partnerIndex = queues[mode].findIndex(id => {
            let u = io.sockets.sockets.get(id);
            return u && id !== socket.id && (pref === "Any" || u.gender === pref);
        });
        if (partnerIndex !== -1) {
            let partnerId = queues[mode].splice(partnerIndex, 1)[0];
            pairs[socket.id] = partnerId; pairs[partnerId] = socket.id;
            io.to(socket.id).emit("matched", { partnerId: partnerId, initiator: true });
            io.to(partnerId).emit("matched", { partnerId: socket.id, initiator: false });
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

    socket.on("disconnect", () => {
        Object.keys(queues).forEach(m => queues[m] = queues[m].filter(id => id !== socket.id));
        let pId = pairs[socket.id];
        if (pId) { io.to(pId).emit("buddy_left"); delete pairs[pId]; }
        delete pairs[socket.id];
    });
});

server.listen(3000, '0.0.0.0', () => console.log("Master Server Ready on 3000"));
