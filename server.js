const express = require('express');
const http = require('http');
const socketIo = require('socket.io');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const fs = require('fs');
const cors = require('cors');

const app = express();
app.use(express.json());
app.use(cors());

const server = http.createServer(app);
const io = socketIo(server, { cors: { origin: "*" } });

const DB_FILE = './users_db.json';
const SECRET_KEY = "ultra_secret_key_123";

let userDatabase = {};
if (fs.existsSync(DB_FILE)) {
    try { userDatabase = JSON.parse(fs.readFileSync(DB_FILE)); } catch (e) { userDatabase = {}; }
}
const saveDB = () => fs.writeFileSync(DB_FILE, JSON.stringify(userDatabase));

// --- HTTP APIs (Signup/Login) ---
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

// --- Real-time Engine ---
let onlineUsers = {}; 
let waitingUser = null;

io.on('connection', (socket) => {
    socket.on('register-online', (data) => {
        onlineUsers[data.email] = { name: data.name, socketId: socket.id, email: data.email, gender: data.gender };
        io.emit('update-user-list', Object.values(onlineUsers));
    });

    socket.on('join-stranger-queue', () => {
        if (waitingUser && waitingUser.id !== socket.id) {
            io.to(waitingUser.id).emit('match-found', { isInitiator: true });
            io.to(socket.id).emit('match-found', { isInitiator: false });
            waitingUser = null;
        } else { waitingUser = socket; }
    });

    socket.on('call-user', (data) => {
        const target = onlineUsers[data.targetEmail];
        if (target) io.to(target.socketId).emit('incoming-call', { fromName: data.fromName, fromEmail: data.fromEmail });
    });

    socket.on('sdp-offer', (d) => { const t = onlineUsers[d.targetEmail]; if(t) io.to(t.socketId).emit('sdp-offer', d); });
    socket.on('sdp-answer', (d) => { const t = onlineUsers[d.targetEmail]; if(t) io.to(t.socketId).emit('sdp-answer', d); });
    socket.on('ice-candidate', (d) => { const t = onlineUsers[d.targetEmail]; if(t) io.to(t.socketId).emit('ice-candidate', d); });

    socket.on('disconnect', () => {
        for (let e in onlineUsers) if (onlineUsers[e].socketId === socket.id) delete onlineUsers[e];
        io.emit('update-user-list', Object.values(onlineUsers));
    });
});

server.listen(process.env.PORT || 3000, '0.0.0.0', () => console.log('UNIFIED SERVER LIVE'));
