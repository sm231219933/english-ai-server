const express = require('express');
const http = require('http');
const socketIo = require('socket.io');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const cors = require('cors');
const fs = require('fs');

const app = express();
app.use(express.json());
app.use(cors());

const server = http.createServer(app);
const io = socketIo(server, { cors: { origin: "*" } });

const SECRET_KEY = "ultra_secret_key_123";
const DB_FILE = './users_db.json';

// Persistent Database Logic
let userDatabase = {};
if (fs.existsSync(DB_FILE)) {
    try {
        const data = fs.readFileSync(DB_FILE, 'utf8');
        if (data) userDatabase = JSON.parse(data);
    } catch (err) { userDatabase = {}; }
}
const saveDB = () => fs.writeFileSync(DB_FILE, JSON.stringify(userDatabase));

// --- DIAGNOSTIC ENDPOINT ---
app.get('/', (req, res) => res.send("<h1>Server is LIVE 🚀</h1>"));
app.get('/status', (req, res) => res.json({ status: "alive", users: Object.keys(userDatabase).length }));

// --- AUTH ENDPOINTS ---
app.post('/signup', async (req, res) => {
    const { email, password, name, age, gender } = req.body;
    if (!email || !password || !name) return res.status(400).json({ msg: "Missing fields" });
    if (userDatabase[email]) return res.status(400).json({ msg: "Email already exists!" });

    const hashedPassword = await bcrypt.hash(password, 10);
    userDatabase[email] = { email, password: hashedPassword, name, age, gender };
    saveDB();
    
    const token = jwt.sign({ email }, SECRET_KEY);
    res.json({ token, user: { email, name, age, gender } });
});

app.post('/login', async (req, res) => {
    const { email, password } = req.body;
    const user = userDatabase[email];
    if (!user) return res.status(400).json({ msg: "User not found!" });

    const isMatch = await bcrypt.compare(password, user.password);
    if (!isMatch) return res.status(400).json({ msg: "Invalid password!" });

    const token = jwt.sign({ email }, SECRET_KEY);
    res.json({ token, user: { email, name: user.name, age: user.age, gender: user.gender } });
});

// --- WEBRTC SIGNALING ---
let onlineUsers = {};
io.on('connection', (socket) => {
    console.log("Connected:", socket.id);
    socket.on('register-online', (data) => {
        onlineUsers[socket.id] = { name: data.name, email: data.email };
        io.emit('update-user-list', Object.values(onlineUsers));
    });
    socket.on('disconnect', () => {
        delete onlineUsers[socket.id];
        io.emit('update-user-list', Object.values(onlineUsers));
    });
});

const PORT = process.env.PORT || 3000;
server.listen(PORT, '0.0.0.0', () => console.log('Auth + WebRTC Server running on ' + PORT));
