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
let userDatabase = {};
if (fs.existsSync(DB_FILE)) userDatabase = JSON.parse(fs.readFileSync(DB_FILE));
const saveDB = () => fs.writeFileSync(DB_FILE, JSON.stringify(userDatabase));

let onlineUsers = {}; // email -> {name, socketId, status}

io.on('connection', (socket) => {
    socket.on('register-online', (data) => {
        onlineUsers[data.email] = { name: data.name, socketId: socket.id, status: "available" };
        io.emit('update-user-list', Object.values(onlineUsers));
    });

    // Private Call Routing
    socket.on('private-call-request', (data) => {
        const target = onlineUsers[data.targetEmail];
        if (target && target.status === "available") {
            io.to(target.socketId).emit('incoming-call', { fromName: data.fromName, fromEmail: data.fromEmail });
        }
    });

    socket.on('call-response', (data) => {
        const caller = onlineUsers[data.callerEmail];
        if (caller) io.to(caller.socketId).emit('call-response', data);
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
});

const PORT = process.env.PORT || 3000;
server.listen(PORT, '0.0.0.0', () => console.log('Pro Server Live on ' + PORT));
