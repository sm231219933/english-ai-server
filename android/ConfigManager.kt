package com.smnm.englishtrackingai

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore

data class SubscriptionPlan(
    val id: String = "",
    val name: String = "",
    val originalPrice: Int = 0,
    val offerPrice: Int = 0,
    val isOfferActive: Boolean = false,
    val productId: String = ""
) {
    fun getDiscountPercentage(): Int {
        if (originalPrice <= 0 || offerPrice <= 0 || offerPrice >= originalPrice) return 0
        return ((originalPrice - offerPrice).toDouble() / originalPrice * 100).toInt()
    }
}

object ConfigManager {
    private val db by lazy { FirebaseFirestore.getInstance() }
    
    // Remote Config / Secrets
    var turnUser: String = "" 
        private set
    var turnPass: String = "" 
        private set
    var signalingUrl: String = "" 
        private set
    var turnUrl: String = "" 
        private set

    var freeCallLimit = 5
        private set

    var plans = mutableMapOf<String, SubscriptionPlan>()
        private set

    fun fetchConfig(onComplete: () -> Unit) {
        // Fetch Plans and Free Limit
        db.collection("app_config").document("subscriptions").get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    freeCallLimit = doc.getLong("free_call_limit")?.toInt() ?: 5
                    val plansData = doc.get("plans") as? Map<String, Map<String, Any>>
                    plansData?.forEach { (key, value) ->
                        plans[key] = SubscriptionPlan(
                            id = key,
                            name = value["name"] as? String ?: "",
                            originalPrice = (value["original_price"] as? Long)?.toInt() ?: 0,
                            offerPrice = (value["offer_price"] as? Long)?.toInt() ?: 0,
                            isOfferActive = value["is_offer_active"] as? Boolean ?: false,
                            productId = value["product_id"] as? String ?: ""
                        )
                    }
                } else {
                    setupDefaultPlans()
                }
                
                // Fetch Secrets after plans are fetched
                fetchSecrets(onComplete)
            }
            .addOnFailureListener {
                Log.e("ConfigManager", "Failed to fetch config: ${it.message}")
                setupDefaultPlans()
                fetchSecrets(onComplete)
            }
    }

    private fun fetchSecrets(onComplete: () -> Unit) {
        db.collection("app_config").document("secrets").get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    turnUser = doc.getString("turn_user") ?: turnUser
                    turnPass = doc.getString("turn_pass") ?: turnPass
                    signalingUrl = doc.getString("signaling_url") ?: signalingUrl
                    turnUrl = doc.getString("turn_url") ?: turnUrl
                    Log.d("ConfigManager", "Secrets fetched successfully")
                }
                onComplete()
            }
            .addOnFailureListener {
                Log.e("ConfigManager", "Failed to fetch secrets: ${it.message}")
                onComplete()
            }
    }

    private fun setupDefaultPlans() {
        plans["monthly"] = SubscriptionPlan("monthly", "Starter Monthly", 99, 29, true, "tinkl_monthly")
        plans["three_month"] = SubscriptionPlan("three_month", "Pro 3-Months", 297, 159, true, "tinkl_3month")
        plans["yearly"] = SubscriptionPlan("yearly", "Elite Yearly", 1100, 550, true, "tinkl_yearly")
    }

    fun isUserVip(context: Context): Boolean {
        return context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE).getBoolean("is_vip", false)
    }

    private fun getVipPlan(context: Context): String {
        return context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE).getString("vip_plan", "") ?: ""
    }

    fun shouldShowBannerAds(context: Context): Boolean {
        if (!isUserVip(context)) return true
        val plan = getVipPlan(context)
        return plan == "tinkl_monthly"
    }

    fun shouldShowRewardedAds(context: Context): Boolean {
        return !isUserVip(context)
    }
}
