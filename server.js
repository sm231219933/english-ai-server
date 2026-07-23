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

let userDatabase = {};
if (fs.existsSync(DB_FILE)) {
    try { userDatabase = JSON.parse(fs.readFileSync(DB_FILE)); } catch (e) { userDatabase = {}; }
}
const saveDB = () => fs.writeFileSync(DB_FILE, JSON.stringify(userDatabase));

app.get('/status', (req, res) => res.json({ status: "alive" }));

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
    if (!user) return res.status(400).json({ msg: "User not found!" });
    const isMatch = await bcrypt.compare(password, user.password);
    if (!isMatch) return res.status(400).json({ msg: "Wrong password!" });
    const token = jwt.sign({ email }, SECRET_KEY);
    res.json({ token, user: { email, name: user.name, age: user.age, gender: user.gender } });
});

let waitingUser = null;
io.on('connection', (socket) => {
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
    socket.on('disconnect', () => { if (waitingUser && waitingUser.id === socket.id) waitingUser = null; });
});

server.listen(process.env.PORT || 3000, '0.0.0.0', () => console.log('Server Live'));
