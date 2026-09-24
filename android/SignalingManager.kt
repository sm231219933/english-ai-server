package com.smnm.englishtrackingai

import android.content.Context
import android.util.Log
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONArray
import org.json.JSONObject

object SignalingManager {
    private var socket: Socket? = null
    private val listeners = mutableSetOf<SignalingListener>()
    private var isConnected = false

    fun init(context: Context, mode: String, listener: SignalingListener) {
        listeners.add(listener)
        
        if (socket != null && isConnected) {
            // RE-REGISTER WITH NEW MODE
            val prefs = context.applicationContext.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
            val myEmail = prefs.getString("user_email", "guest_${System.currentTimeMillis()}") ?: "guest"
            val myGender = prefs.getString("user_gender", "Any") ?: "Any"
            
            socket?.emit("register_user", JSONObject().apply {
                put("userId", myEmail)
                put("gender", myGender)
                put("mode", mode)
            })
            socket?.emit("request_user_list", JSONObject().put("mode", mode))
            
            listener.onConnected()
            return
        }

        val prefs = context.applicationContext.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val myEmail = prefs.getString("user_email", "guest_${System.currentTimeMillis()}") ?: "guest"
        val myName = prefs.getString("user_name", "Learner") ?: "Learner"
        val myGender = prefs.getString("user_gender", "Any") ?: "Any"

        try {
            val options = IO.Options().apply {
                forceNew = true
                reconnection = true
            }
            socket = IO.socket(ConfigManager.signalingUrl, options)

            socket?.on(Socket.EVENT_CONNECT) {
                isConnected = true
                Log.d("SignalingManager", "Connected to Port 3000 for $mode")
                
                // REGISTER USER WITH MODE
                val data = JSONObject().apply {
                    put("userId", myEmail)
                    put("gender", myGender)
                    put("mode", mode)
                }
                socket?.emit("register_user", data)
                
                // REQUEST LIST FOR SPECIFIC MODE
                socket?.emit("request_user_list", JSONObject().put("mode", mode))
                
                listeners.forEach { it.onConnected() }
            }

            socket?.on("online_users_list") { args ->
                try {
                    val arr = args[0] as JSONArray
                    Log.d("SignalingManager", "online_users_list received: ${arr.length()} items")
                    val list = ArrayList<Map<String, String>>()
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val name = obj.optString("name", obj.optString("userName", "Learner"))
                        val gender = obj.optString("gender", "Any")
                        val email = obj.optString("userId", obj.optString("email", ""))
                        
                        list.add(mapOf("name" to name, "gender" to gender, "email" to email))
                    }
                    listeners.forEach { it.onUserListUpdated(list) }
                } catch (e: Exception) {
                    Log.e("SignalingManager", "Parse Error: ${e.message}")
                }
            }

            socket?.on("live_count") { args ->
                val count = try { (args[0] as? Number)?.toInt() ?: 0 } catch(e: Exception) { 0 }
                Log.d("SignalingManager", "live_count: $count")
                listeners.forEach { it.onLiveCountUpdate(count) }
            }

            socket?.on("online_users_count") { args ->
                val count = try { (args[0] as? Number)?.toInt() ?: 0 } catch(e: Exception) { 0 }
                Log.d("SignalingManager", "online_users_count: $count")
                listeners.forEach { it.onLiveCountUpdate(count) }
            }

            socket?.on("page_user_count") { args ->
                val count = try { (args[0] as? Number)?.toInt() ?: 0 } catch(e: Exception) { 0 }
                Log.d("SignalingManager", "page_user_count: $count")
                listeners.forEach { it.onPageCountUpdate(count) }
            }

            socket?.on(Socket.EVENT_DISCONNECT) {
                isConnected = false
                Log.d("SignalingManager", "Disconnected")
            }

            socket?.connect()
        } catch (e: Exception) {
            Log.e("SignalingManager", "Connection Error: ${e.message}")
        }
    }

    fun removeListener(listener: SignalingListener) {
        listeners.remove(listener)
    }

    fun joinPool(userName: String, mode: String) {
        // Port 3000 uses matching via find_buddy in Audio/Video clients
        // Keeping this as stub for compatibility
        Log.d("SignalingManager", "joinPool stub called for $userName in $mode")
    }

    fun leavePool() {
        // Keeping this as stub for compatibility
        Log.d("SignalingManager", "leavePool stub called")
    }

    fun joinPage(pageName: String) {
        socket?.emit("join_page", pageName)
    }

    fun leavePage(pageName: String) {
        socket?.emit("leave_page", pageName)
    }

    fun disconnect() {
        socket?.disconnect()
        socket?.off()
        socket = null
        isConnected = false
    }
}
