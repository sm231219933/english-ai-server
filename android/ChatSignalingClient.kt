package com.smnm.englishtrackingai

import android.content.Context
import android.util.Log
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject

class ChatSignalingClient(context: Context, private val listener: ChatListener) {
    private var socket: Socket? = null
    private val prefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)

    init {
        try {
            socket = IO.socket(ConfigManager.signalingUrl)
            socket?.on(Socket.EVENT_CONNECT) {
                socket?.emit("register_user", JSONObject().apply {
                    put("userId", prefs.getString("user_email", "guest"))
                })
            }
            socket?.on("matched") { listener.onMatched() }
            socket?.on("receive_chat") { args ->
                val data = args[0] as JSONObject
                listener.onMessageReceived(data.getString("message"))
            }
            socket?.on("buddy_left") { listener.onDisconnected() }
            socket?.connect()
        } catch (e: Exception) { Log.e("ChatSig", "Error: ${e.message}") }
    }

    fun findBuddy() { socket?.emit("find_buddy", JSONObject().put("mode", "chat")) }
    fun sendMessage(msg: String) { socket?.emit("send_chat", JSONObject().put("message", msg)) }
    
    // Remote Error Logging
    fun sendError(error: String) { socket?.emit("app_error_log", JSONObject().put("error", error)) }

    fun destroy() { socket?.disconnect(); socket?.off(); socket?.close() }

    interface ChatListener {
        fun onMatched()
        fun onMessageReceived(message: String)
        fun onDisconnected()
    }
}
