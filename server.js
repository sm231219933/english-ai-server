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

let onlineUsers = {}; 

io.on('connection', (socket) => {
    socket.on('register-online', (data) => {
        // SAVING GENDER FROM CLIENT
        onlineUsers[data.email] = { name: data.name, socketId: socket.id, email: data.email, gender: data.gender };
        io.emit('update-user-list', Object.values(onlineUsers));
    });

    socket.on('call-user', (data) => {
        const targetSocket = onlineUsers[data.targetEmail];
        if (targetSocket) {
            io.to(targetSocket.socketId).emit('incoming-call', { fromName: data.fromName, fromEmail: data.fromEmail });
        }
    });

    socket.on('disconnect', () => {
        for (let email in onlineUsers) {
            if (onlineUsers[email].socketId === socket.id) {
                delete onlineUsers[email];
                break;
            }
        }
        io.emit('update-user-list', Object.values(onlineUsers));
    });
    
    // WebRTC Signaling Logic (SDP/ICE) remains same...
});

const PORT = process.env.PORT || 3000;
server.listen(PORT, '0.0.0.0', () => console.log('Pro Server Live on ' + PORT));
