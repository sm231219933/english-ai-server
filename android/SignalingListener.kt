package com.smnm.englishtrackingai

interface SignalingListener {
    fun onIncomingCall(fromUserId: String, fromUserName: String, offer: String, callType: String)
    fun onCallAnswered(answer: String)
    fun onCallRejected()
    fun onCallEnded()
    fun onIceCandidateReceived(candidate: String)
    fun onCallFailed(reason: String)
    fun onConnected()
    fun onLiveCountUpdate(count: Int)
    fun onUserListUpdated(users: List<Map<String, String>>)
    fun onPageCountUpdate(count: Int)
}
