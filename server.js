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

// --- AUTH APIs (Never Breaking Again) ---
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

// --- CALLING LOGIC ( WhatsApp + Omegle Style ) ---
let onlineUsers = new Map(); 
let strangerQueue = [];      

io.on("connection", (socket) => {
    socket.on("register_user", (data) => {
        socket.userId = data.userId; // email
        socket.userName = data.userName;
        onlineUsers.set(data.userId, { socketId: socket.id, name: data.userName, gender: data.gender || "Male", status: "available" });
        broadcastOnlineUsers();
    });

    socket.on("initiate_direct_call", ({ targetUserId }) => {
        const target = onlineUsers.get(targetUserId);
        if (target && target.status === "available") {
            io.to(target.socketId).emit("incoming_call", { 
                callerId: socket.userId, callerName: socket.userName, callerSocketId: socket.id 
            });
            socket.emit("call_status", { status: "ringing" });
        } else { socket.emit("call_status", { status: "busy_or_offline" }); }
    });

    socket.on("respond_to_call", ({ callerSocketId, accepted }) => {
        if (accepted) {
            if(onlineUsers.has(socket.userId)) onlineUsers.get(socket.userId).status = "busy";
            io.to(callerSocketId).emit("call_accepted", { receiverSocketId: socket.id });
        } else { io.to(callerSocketId).emit("call_rejected", { reason: "Declined" }); }
        broadcastOnlineUsers();
    });

    socket.on("find_stranger", () => {
        if (strangerQueue.length > 0) {
            const partnerId = strangerQueue.shift();
            if (partnerId !== socket.id && io.sockets.sockets.get(partnerId)) {
                io.to(socket.id).emit("stranger_matched", { peerSocketId: partnerId, isInitiator: true });
                io.to(partnerId).emit("stranger_matched", { peerSocketId: socket.id, isInitiator: false });
            } else { strangerQueue.push(socket.id); }
        } else {
            strangerQueue.push(socket.id);
            socket.emit("stranger_searching", { message: "Searching..." });
        }
    });

    socket.on("webrtc_signal", ({ targetSocketId, signalData }) => {
        io.to(targetSocketId).emit("webrtc_signal", { senderSocketId: socket.id, signalData });
    });

    socket.on("end_call", ({ targetSocketId }) => {
        if (onlineUsers.has(socket.userId)) onlineUsers.get(socket.userId).status = "available";
        io.to(targetSocketId).emit("call_ended");
        broadcastOnlineUsers();
    });

    socket.on("disconnect", () => {
        strangerQueue = strangerQueue.filter(id => id !== socket.id);
        if (socket.userId) { onlineUsers.delete(socket.userId); broadcastOnlineUsers(); }
    });

    function broadcastOnlineUsers() {
        const list = Array.from(onlineUsers.entries()).map(([id, data]) => ({ 
            userId: id, name: data.name, gender: data.gender, status: data.status 
        }));
        io.emit("online_users_list", list);
    }
});

server.listen(3000, '0.0.0.0', () => console.log("Signaling Server Ready"));
