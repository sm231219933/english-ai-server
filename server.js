const express = require('express');
const http = require('http');
const socketIo = require('socket.io');

const app = express();
const server = http.createServer(app);
const io = socketIo(server, { cors: { origin: "*" } });

let onlineUsers = {}; // Tracks {socketId: {name, level}}
let waitingUser = null;

io.on('connection', (socket) => {
    // When a user joins the app
    socket.on('register-user', (data) => {
        onlineUsers[socket.id] = { name: data.name, level: data.level, id: socket.id };
        io.emit('update-user-list', Object.values(onlineUsers)); // Send to everyone
    });

    socket.on('join-stranger-queue', () => {
        if (waitingUser && waitingUser !== socket.id) {
            io.to(waitingUser).emit('match-found', { isInitiator: true });
            io.to(socket.id).emit('match-found', { isInitiator: false });
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

app.get('/', (req, res) => res.send('Server is Live and Tracking Users!'));

server.listen(process.env.PORT || 3000, '0.0.0.0', () => {
    console.log('Cloud Server Running...');
});
