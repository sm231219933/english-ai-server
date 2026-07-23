const express = require('express');
const http = require('http');
const { Server } = require("socket.io");
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const fs = require('fs');
const cors = require('cors');

const app = express();
app.use(express.json());
app.use(cors());

const server = http.createServer(app);
const io = new Server(server, { cors: { origin: "*" } });

const DB_FILE = './users_db.json';
const SECRET_KEY = "ultra_secret_key_123";

let userDatabase = {};
if (fs.existsSync(DB_FILE)) {
    try { 
        userDatabase = JSON.parse(fs.readFileSync(DB_FILE)); 
    } catch (e) { userDatabase = {}; }
}
const saveDB = () => fs.writeFileSync(DB_FILE, JSON.stringify(userDatabase));

// --- 1. SMART AUTH APIs ---

app.post('/signup', async (req, res) => {
    const { email, password, name, age, gender } = req.body;
    if (userDatabase[email]) {
        return res.status(400).json({ msg: "Email already registered" });
    }
    const hashedPassword = await bcrypt.hash(password, 10);
    userDatabase[email] = { email, password: hashedPassword, name, age, gender };
    saveDB();
    res.json({ token: jwt.sign({ email }, SECRET_KEY), user: { email, name, age, gender } });
});

app.post('/login', async (req, res) => {
    const { email, password } = req.body;
    const user = userDatabase[email];
    
    if (!user) {
        return res.status(404).json({ msg: "Please register first" });
    }

    const isMatch = await bcrypt.compare(password, user.password);
    if (!isMatch) {
        return res.status(401).json({ msg: "User or password mismatch" });
    }

    const token = jwt.sign({ email }, SECRET_KEY);
    res.json({ token, user: { email, name: user.name, age: user.age, gender: user.gender } });
});

// --- 2. PROFESSIONAL CALLING ENGINE ---

let onlineUsers = new Map(); // userId -> {socketId, name, gender}
let waitingQueue = []; 

io.on("connection", (socket) => {
    console.log("New User Connected:", socket.id);

    socket.on("register_user", (data) => {
        socket.userId = data.userId;
        socket.gender = data.gender;
        onlineUsers.set(data.userId, { 
            socketId: socket.id, 
            name: data.userName, 
            gender: data.gender, 
            userId: data.userId 
        });
        broadcastOnlineUsers();
    });

    socket.on("find_stranger", (data) => {
        const pref = data.prefGender;
        let partnerSocketId = waitingQueue.find(id => {
            let p = io.sockets.sockets.get(id);
            return p && id !== socket.id && (pref === "Any" || p.gender === pref);
        });

        if (partnerSocketId) {
            waitingQueue = waitingQueue.filter(id => id !== partnerSocketId);
            io.to(socket.id).emit("stranger_matched", { peerSocketId: partnerSocketId, isInitiator: true });
            io.to(partnerSocketId).emit("stranger_matched", { peerSocketId: socket.id, isInitiator: false });
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
        broadcastOnlineUsers();
    });

    function broadcastOnlineUsers() {
        io.emit("online_users_list", Array.from(onlineUsers.values()));
    }
});

server.listen(3000, '0.0.0.0', () => console.log("--- FINAL UNIFIED SERVER LIVE ---"));
