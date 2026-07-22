const express = require('express');
const http = require('http');
const socketIo = require('socket.io');
const app = express();
const server = http.createServer(app);
const io = socketIo(server, { cors: { origin: "*" } });

let onlineUsers = {}; 
let waitingUser = null;

io.on('connection', (socket) => {
    socket.on('register-user', (data) => {
        onlineUsers[socket.id] = { name: data.name, level: data.level, socketId: socket.id };
        io.emit('update-user-list', Object.values(onlineUsers));
    });

    socket.on('join-stranger-queue', () => {
        if (waitingUser && waitingUser !== socket.id) {
            io.to(waitingUser).emit('match-found', { isInitiator: true, room: "room_" + waitingUser });
            io.to(socket.id).emit('match-found', { isInitiator: false, room: "room_" + waitingUser });
            waitingUser = null;
        } else {
            waitingUser = socket.id;
        }
    });

    socket.on('send-offer', (data) => socket.broadcast.emit('offer', data));
    socket.on('send-answer', (data) => socket.broadcast.emit('answer', data));
    socket.on('send-ice-candidate', (data) => socket.broadcast.emit('ice-candidate', data));

    socket.on('disconnect', () => {
        delete onlineUsers[socket.id];
        io.emit('update-user-list', Object.values(onlineUsers));
        if (waitingUser === socket.id) waitingUser = null;
    });
});
server.listen(process.env.PORT || 3000, '0.0.0.0', () => console.log('Server Live'));
