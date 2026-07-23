const express = require('express');
const http = require('http');
const { Server } = require("socket.io");
const cors = require('cors');

const app = express();
app.use(cors());
const server = http.createServer(app);
const io = new Server(server, { cors: { origin: "*" } });

let onlineUsers = new Map(); // userId -> {socketId, gender, name}
let waitingQueue = []; // Array of socket IDs

io.on("connection", (socket) => {
    socket.on("register_user", (data) => {
        socket.userId = data.userId;
        socket.userName = data.userName;
        socket.gender = data.gender;
        onlineUsers.set(data.userId, { socketId: socket.id, name: data.userName, gender: data.gender });
        
        // Broadcast online list (excluding self)
        broadcastOnlineUsers();
    });

    socket.on("find_stranger", (data) => {
        const prefGender = data.prefGender; // 'Any', 'Male', 'Female'
        
        // Find match in queue
        let partnerSocketId = waitingQueue.find(id => {
            let user = io.sockets.sockets.get(id);
            if (!user || id === socket.id) return false;
            if (prefGender === "Any") return true;
            return user.gender === prefGender;
        });

        if (partnerSocketId) {
            waitingQueue = waitingQueue.filter(id => id !== partnerSocketId);
            io.to(socket.id).emit("stranger_matched", { peerSocketId: partnerSocketId, isInitiator: true });
            io.to(partnerSocketId).emit("stranger_matched", { peerSocketId: socket.id, isInitiator: false });
        } else {
            waitingQueue.push(socket.id);
        }
    });

    socket.on("webrtc_signal", (data) => {
        io.to(data.targetSocketId).emit("webrtc_signal", { senderSocketId: socket.id, signalData: data.signalData });
    });

    socket.on("disconnect", () => {
        waitingQueue = waitingQueue.filter(id => id !== socket.id);
        onlineUsers.delete(socket.userId);
        broadcastOnlineUsers();
    });

    function broadcastOnlineUsers() {
        const list = Array.from(onlineUsers.values()).map(u => ({ name: u.name, gender: u.gender, userId: u.userId }));
        io.emit("online_users_list", list);
    }
});

server.listen(3000, '0.0.0.0', () => console.log("Final Omegle-Style Server Live"));
