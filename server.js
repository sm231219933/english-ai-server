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

const DB_FILE = './app_db.json';
let db = { users: {}, friends: {}, requests: {} }; // Simple Persistent DB

if (fs.existsSync(DB_FILE)) db = JSON.parse(fs.readFileSync(DB_FILE));
const saveDB = () => fs.writeFileSync(DB_FILE, JSON.stringify(db));

// Auth APIs
app.post('/signup', async (req, res) => {
    const { email, password, name, age, gender } = req.body;
    if (db.users[email]) return res.status(400).json({ msg: "Exists" });
    const hashedPassword = await bcrypt.hash(password, 10);
    db.users[email] = { email, password: hashedPassword, name, age, gender, requests: [], friends: [] };
    saveDB();
    res.json({ user: db.users[email], token: jwt.sign({ email }, "key") });
});

app.post('/login', async (req, res) => {
    const { email, password } = req.body;
    const user = db.users[email];
    if (!user || !(await bcrypt.compare(password, user.password))) return res.status(400).json({ msg: "Fail" });
    res.json({ user, token: jwt.sign({ email }, "key") });
});

// Socket Engine
let online = {}; // email -> socketId
io.on('connection', (socket) => {
    socket.on('go-online', (email) => {
        online[email] = socket.id;
        io.emit('update-list', Object.values(db.users).map(u => ({ name: u.name, email: u.email, online: !!online[u.email] })));
    });

    // Chat
    socket.on('send-msg', (data) => {
        if (online[data.toEmail]) io.to(online[data.toEmail]).emit('recv-msg', data);
    });

    // Calling
    socket.on('call-user', (data) => {
        if (online[data.toEmail]) io.to(online[data.toEmail]).emit('incoming-call', data);
    });

    socket.on('call-response', (data) => {
        if (online[data.toEmail]) io.to(online[data.toEmail]).emit('call-response', data);
    });

    socket.on('disconnect', () => {
        for (let email in online) if (online[email] === socket.id) delete online[email];
    });
});

server.listen(process.env.PORT || 3000, '0.0.0.0', () => console.log('Social Server Live'));
