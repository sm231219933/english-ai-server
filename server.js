const express = require('express');
const http = require('http');
const socketIo = require('socket.io');
const app = express();
const server = http.createServer(app);
const io = socketIo(server, { cors: { origin: "*" } });

let onlineUsers = {}; 
let userDatabase = {}; // THIS IS YOUR PERMANENT CLOUD STORAGE

io.on('connection', (socket) => {
    // When user logs in/opens app
    socket.on('sync-profile', (data) => {
        if(data.uid) {
            // Save to permanent storage
            if(data.name) userDatabase[data.uid] = { name: data.name, age: data.age, gender: data.gender };
            // Send back stored data to app (Solves uninstall problem)
            socket.emit('profile-data', userDatabase[data.uid] || {});
            
            // Add to online list
            onlineUsers[socket.id] = { uid: data.uid, name: userDatabase[data.uid]?.name || "Learner" };
            io.emit('update-user-list', Object.values(onlineUsers));
        }
    });

    socket.on('join-stranger-queue', () => {
        if (waitingUser && waitingUser !== socket.id) {
            io.to(waitingUser).emit('match-found', { isInitiator: true });
            io.to(socket.id).emit('match-found', { isInitiator: false });
            waitingUser = null;
        } else { waitingUser = socket.id; }
    });

    socket.on('send-offer', (data) => socket.broadcast.emit('offer', data));
    socket.on('send-answer', (data) => socket.broadcast.emit('answer', data));
    socket.on('send-ice-candidate', (data) => socket.broadcast.emit('ice-candidate', data));

    socket.on('disconnect', () => {
        delete onlineUsers[socket.id];
        io.emit('update-user-list', Object.values(onlineUsers));
    });
});
server.listen(process.env.PORT || 3000, '0.0.0.0', () => console.log('DB Server Live'));
