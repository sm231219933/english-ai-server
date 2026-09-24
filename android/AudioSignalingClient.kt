package com.smnm.englishtrackingai

import android.content.Context
import android.util.Log
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription

class AudioSignalingClient(context: Context, private val listener: AudioListener) {
    private var socket: Socket? = null
    private val prefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)

    init {
        try {
            socket = IO.socket(ConfigManager.signalingUrl)
            socket?.on(Socket.EVENT_CONNECT) {
                socket?.emit("register_user", JSONObject().apply {
                    put("userId", prefs.getString("user_email", "guest"))
                    put("gender", prefs.getString("user_gender", "Male"))
                })
            }
            socket?.on("matched") { args ->
                val data = args[0] as JSONObject
                listener.onMatched(data.getBoolean("initiator"))
            }
            socket?.on("webrtc_signal") { args ->
                val data = args[0] as JSONObject
                val signal = data.getJSONObject("signalData")
                if (signal.has("sdp")) {
                    val sdpObj = signal.getJSONObject("sdp")
                    listener.onSdpReceived(SessionDescription(if (sdpObj.getString("type") == "offer") SessionDescription.Type.OFFER else SessionDescription.Type.ANSWER, sdpObj.getString("sdp")))
                } else if (signal.has("candidate")) {
                    val cand = signal.getJSONObject("candidate")
                    listener.onIceReceived(IceCandidate(cand.getString("sdpMid"), cand.getInt("sdpMLineIndex"), cand.getString("candidate")))
                }
            }
            socket?.on("buddy_left") { listener.onDisconnected() }
            socket?.connect()
        } catch (e: Exception) { Log.e("AudioSig", "Error: ${e.message}") }
    }

    fun findBuddy(pref: String) { socket?.emit("find_buddy", JSONObject().put("mode", "audio").put("prefGender", pref)) }
    fun sendSignal(signal: JSONObject) { socket?.emit("webrtc_signal", JSONObject().put("signalData", signal)) }
    
    // Remote Error Logging
    fun sendError(error: String) { socket?.emit("app_error_log", JSONObject().put("error", error)) }

    fun destroy() { socket?.disconnect(); socket?.off(); socket?.close() }

    interface AudioListener {
        fun onMatched(isInitiator: Boolean)
        fun onSdpReceived(description: SessionDescription)
        fun onIceReceived(candidate: IceCandidate)
        fun onDisconnected()
    }
}
