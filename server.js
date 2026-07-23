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
    try { userDatabase = JSON.parse(fs.readFileSync(DB_FILE)); } catch (e) { userDatabase = {}; }
}
const saveDB = () => fs.writeFileSync(DB_FILE, JSON.stringify(userDatabase));

// --- AUTH APIs ---
app.post('/signup', async (req, res) => {
    const { email, password, name, age, gender } = req.body;
    if (userDatabase[email]) return res.status(400).json({ msg: "User exists!" });
    const hashedPassword = await bcrypt.hash(password, 10);
    userDatabase[email] = { email, password: hashedPassword, name, age, gender };
    saveDB();
    res.json({ token: jwt.sign({ email }, SECRET_KEY), user: { email, name, age, gender } });
});

app.post('/login', async (req, res) => {
    const { email, password } = req.body;
    const user = userDatabase[email];
    if (!user || !(await bcrypt.compare(password, user.password))) return res.status(400).json({ msg: "Fail" });
    res.json({ token: jwt.sign({ email }, SECRET_KEY), user: { email, name: user.name, age: user.age, gender: user.gender } });
});

// --- CALLING ENGINE (OMEGLE STYLE) ---
let onlineUsers = new Map(); 
let waitingQueue = [];      

io.on("connection", (socket) => {
    socket.on("register_user", (data) => {
        socket.userId = data.userId;
        socket.userName = data.userName;
        socket.gender = data.gender;
        onlineUsers.set(data.userId, { socketId: socket.id, name: data.userName, gender: data.gender });
        broadcastOnlineUsers();
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
            waitingQueue.push(socket.id);
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
        const list = Array.from(onlineUsers.values());
        io.emit("online_users_list", list);
    }
});

server.listen(3000, '0.0.0.0', () => console.log("PERFECT SERVER LIVE"));
