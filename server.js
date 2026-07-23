const express = require('express');
const http = require('http');
const socketIo = require('socket.io');
const fs = require('fs');
const cors = require('cors');

const app = express();
app.use(express.json());
app.use(cors());

const server = http.createServer(app);
const io = socketIo(server, { cors: { origin: "*" } });

let onlineUsers = {}; // email -> socketId
let waitingUser = null; 

io.on('connection', (socket) => {
    socket.on('register-online', (data) => {
        onlineUsers[data.email] = socket.id;
        socket.email = data.email;
        socket.name = data.name;
        io.emit('update-user-list', Object.keys(onlineUsers).map(e => ({email: e, name: "Learner"}))); 
    });

    // --- STRANGER MATCHING ---
    socket.on('join-stranger-queue', () => {
        if (waitingUser && waitingUser !== socket.id) {
            io.to(waitingUser).emit('match-found', { isInitiator: true, remoteEmail: socket.email });
            io.to(socket.id).emit('match-found', { isInitiator: false, remoteEmail: onlineUsers[waitingUser] });
            waitingUser = null;
        } else {
            waitingUser = socket.id;
        }
    });

    // --- PRIVATE CALLING ---
    socket.on('call-user', (data) => {
        const targetSocket = onlineUsers[data.targetEmail];
        if (targetSocket) {
            io.to(targetSocket).emit('incoming-call', { fromName: data.fromName, fromEmail: socket.email });
        }
    });

    socket.on('accept-call', (data) => {
        const callerSocket = onlineUsers[data.callerEmail];
        if (callerSocket) io.to(callerSocket).emit('call-accepted', { by: socket.email });
    });

    // --- WEBRTC SIGNALING ---
    socket.on('sdp-offer', (data) => {
        const target = onlineUsers[data.targetEmail];
        if (target) io.to(target).emit('sdp-offer', { sdp: data.sdp, from: socket.email });
    });

    socket.on('sdp-answer', (data) => {
        const target = onlineUsers[data.targetEmail];
        if (target) io.to(target).emit('sdp-answer', { sdp: data.sdp, from: socket.email });
    });

    socket.on('ice-candidate', (data) => {
        const target = onlineUsers[data.targetEmail];
        if (target) io.to(target).emit('ice-candidate', { candidate: data.candidate, from: socket.email });
    });

    socket.on('disconnect', () => {
        if (waitingUser === socket.id) waitingUser = null;
        delete onlineUsers[socket.email];
    });
});

server.listen(process.env.PORT || 3000, '0.0.0.0', () => console.log('Server Live'));
