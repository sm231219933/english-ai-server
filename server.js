const express = require('express');
const http = require('http');
const socketIo = require('socket.io');
const app = express();
const server = http.createServer(app);
const io = socketIo(server, { cors: { origin: "*" } });

let onlineUsers = {}; 

io.on('connection', (socket) => {
    socket.on('register-user', (data) => {
        onlineUsers[socket.id] = { name: data.name, socketId: socket.id };
        io.emit('update-user-list', Object.values(onlineUsers));
    });

    // Private Message logic
    socket.on('send-private-msg', (data) => {
        io.to(data.targetSocketId).emit('receive-msg', { senderName: onlineUsers[socket.id].name, text: data.text });
    });

    // Call Request with Ringtone notification
    socket.on('call-user', (data) => {
        io.to(data.targetSocketId).emit('incoming-call', { fromName: onlineUsers[socket.id].name, fromSocketId: socket.id });
    });

    socket.on('disconnect', () => {
        delete onlineUsers[socket.id];
        io.emit('update-user-list', Object.values(onlineUsers));
    });
});
server.listen(process.env.PORT || 3000, '0.0.0.0', () => console.log('Server Live'));
