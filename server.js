const express = require('express');
const http = require('http');
const socketIo = require('socket.io');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const cors = require('cors');

const app = express();
app.use(express.json());
app.use(cors());

const server = http.createServer(app);
const io = socketIo(server, { cors: { origin: "*" } });

let userDatabase = {}; 
let onlineUsers = {}; // socketId -> {uid, name, email}

io.on('connection', (socket) => {
    socket.on('register-online', (data) => {
        onlineUsers[socket.id] = { uid: data.uid, name: data.name, email: data.email };
        io.emit('update-user-list', Object.values(onlineUsers));
    });

    // 1. Private Call Request
    socket.on('request-private-call', (data) => {
        const target = Object.values(onlineUsers).find(u => u.uid === data.targetUid);
        if (target) {
            io.to(target.socketId).emit('incoming-call', { 
                fromName: onlineUsers[socket.id].name, 
                fromUid: onlineUsers[socket.id].uid,
                room: "room_" + socket.id 
            });
        }
    });

    // 2. Accept/Reject Call
    socket.on('respond-call', (data) => {
        const target = Object.values(onlineUsers).find(u => u.uid === data.callerUid);
        if (target) io.to(target.socketId).emit('call-response', { accepted: data.accepted, room: data.room });
    });

    socket.on('disconnect', () => {
        delete onlineUsers[socket.id];
        io.emit('update-user-list', Object.values(onlineUsers));
    });
});

server.listen(process.env.PORT || 3000, () => console.log('Branded Server Live'));
