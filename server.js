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

// Persistent Database Logic
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
    const token = jwt.sign({ email }, SECRET_KEY);
    res.json({ token, user: { email, name, age, gender } });
});

app.post('/login', async (req, res) => {
    const { email, password } = req.body;
    const user = userDatabase[email];
    if (!user || !(await bcrypt.compare(password, user.password))) {
        return res.status(400).json({ msg: "Invalid Credentials" });
    }
    const token = jwt.sign({ email }, SECRET_KEY);
    res.json({ token, user: { email, name: user.name, age: user.age, gender: user.gender } });
});

// --- CALLING & SOCIAL ENGINE ---
let onlineUsers = {}; 
let waitingUser = null;

io.on('connection', (socket) => {
    socket.on('register-online', (data) => {
        onlineUsers[data.email] = { name: data.name, socketId: socket.id, email: data.email };
        io.emit('update-user-list', Object.values(onlineUsers));
    });

    socket.on('join-stranger-queue', () => {
        if (waitingUser && waitingUser.id !== socket.id) {
            io.to(waitingUser.id).emit('match-found', { isInitiator: true });
            io.to(socket.id).emit('match-found', { isInitiator: false });
            waitingUser = null;
        } else { waitingUser = socket; }
    });

    socket.on('send-offer', (data) => socket.broadcast.emit('offer', data));
    socket.on('send-answer', (data) => socket.broadcast.emit('answer', data));
    socket.on('send-ice-candidate', (data) => socket.broadcast.emit('ice-candidate', data));

    socket.on('disconnect', () => {
        for (let email in onlineUsers) {
            if (onlineUsers[email].socketId === socket.id) {
                delete onlineUsers[email];
                break;
            }
        }
        io.emit('update-user-list', Object.values(onlineUsers));
    });
});

const PORT = process.env.PORT || 3000;
server.listen(PORT, '0.0.0.0', () => console.log('Pro Server Live on ' + PORT));
