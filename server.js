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

// --- AUTH APIs (STAYS UNCHANGED) ---
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

// --- SIMPLE STRANGER MATCHING ENGINE ---
let waitingUsers = []; // List of { socketId, gender }

io.on('connection', (socket) => {
    socket.on('join-stranger-queue', (data) => {
        const myGender = data.gender;
        const prefGender = data.prefGender;

        // Simple Matching Logic: Find someone in queue
        let match = waitingUsers.find(u => {
            if (prefGender === "Any") return true;
            return u.gender === prefGender;
        });

        if (match && match.socketId !== socket.id) {
            waitingUsers = waitingUsers.filter(u => u.socketId !== match.socketId);
            io.to(match.socketId).emit('match-found', { isInitiator: true, peerId: socket.id });
            io.to(socket.id).emit('match-found', { isInitiator: false, peerId: match.socketId });
        } else {
            waitingUsers.push({ socketId: socket.id, gender: myGender });
        }
    });

    socket.on('send-signal', (data) => {
        io.to(data.to).emit('recv-signal', { from: socket.id, signal: data.signal });
    });

    socket.on('disconnect', () => {
        waitingUsers = waitingUsers.filter(u => u.socketId !== socket.id);
    });
});

server.listen(3000, '0.0.0.0', () => console.log('Simple Stranger Server Live'));
