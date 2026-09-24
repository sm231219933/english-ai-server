package com.smnm.englishtrackingai

import android.content.Context
import android.graphics.Paint
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.android.billingclient.api.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class VipActivity : AppCompatActivity() {

    private lateinit var billingClient: BillingClient
    private var productDetailsList: List<ProductDetails> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        setContentView(R.layout.activity_vip)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<ImageButton>(R.id.btnVipBack).setOnClickListener { finish() }

        setupUI()
        setupBillingClient()
    }

    private fun setupUI() {
        // Monthly
        updatePlanUI("monthly", R.id.priceOfferMonthly, R.id.priceOriginalMonthly, R.id.badgeMonthly, R.id.btnMonthly, "tinkl_monthly")
        
        // 3 Months
        updatePlanUI("three_month", R.id.priceOffer3Month, R.id.priceOriginal3Month, R.id.badge3Month, R.id.btn3Month, "tinkl_3month")
        
        // Yearly
        updatePlanUI("yearly", R.id.priceOfferYearly, R.id.priceOriginalYearly, R.id.badgeYearly, R.id.btnYearly, "tinkl_yearly")
    }

    private fun updatePlanUI(planKey: String, offerId: Int, originalId: Int, badgeId: Int, btnId: Int, productId: String) {
        val plan = ConfigManager.plans[planKey] ?: return
        
        findViewById<TextView>(offerId).text = "₹${plan.offerPrice}"
        
        val originalView = findViewById<TextView>(originalId)
        if (originalView != null) {
            originalView.text = "₹${plan.originalPrice}"
            originalView.paintFlags = originalView.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            originalView.visibility = if (plan.isOfferActive) View.VISIBLE else View.GONE
        }
        
        val badgeView = findViewById<TextView>(badgeId)
        if (badgeView != null) {
            val discount = plan.getDiscountPercentage()
            if (plan.isOfferActive && discount > 0) {
                badgeView.text = "SAVE $discount%"
                badgeView.visibility = View.VISIBLE
            } else {
                badgeView.visibility = View.GONE
            }
        }
        
        findViewById<Button>(btnId).setOnClickListener { initiatePurchase(productId) }
    }

    private fun setupBillingClient() {
        billingClient = BillingClient.newBuilder(this)
            .setListener { billingResult, purchases ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                    for (purchase in purchases) {
                        handlePurchase(purchase)
                    }
                }
            }
            .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
            .build()

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryProductDetails()
                }
            }
            override fun onBillingServiceDisconnected() {
                setupBillingClient()
            }
        })
    }

    private fun queryProductDetails() {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder().setProductId("tinkl_monthly").setProductType(BillingClient.ProductType.SUBS).build(),
            QueryProductDetailsParams.Product.newBuilder().setProductId("tinkl_3month").setProductType(BillingClient.ProductType.SUBS).build(),
            QueryProductDetailsParams.Product.newBuilder().setProductId("tinkl_yearly").setProductType(BillingClient.ProductType.SUBS).build()
        )

        val params = QueryProductDetailsParams.newBuilder().setProductList(productList).build()
        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                this.productDetailsList = productDetailsResult.productDetailsList ?: emptyList()
            }
        }
    }

    private fun initiatePurchase(productId: String) {
        val productDetails = productDetailsList.find { it.productId == productId }
        if (productDetails == null) {
            Toast.makeText(this, "Fetching from Play Store... Try again in 2 seconds.", Toast.LENGTH_SHORT).show()
            queryProductDetails()
            return
        }

        val offerToken = productDetails.subscriptionOfferDetails?.get(0)?.offerToken ?: ""
        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .setOfferToken(offerToken)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        billingClient.launchBillingFlow(this, billingFlowParams)
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        val planId = purchase.products[0]
                        activateVip(planId)
                    }
                }
            }
        }
    }

    private fun activateVip(planId: String) {
        getSharedPreferences("UserPrefs", Context.MODE_PRIVATE).edit().apply {
            putString("vip_plan", planId)
            putBoolean("is_vip", true)
            apply()
        }
        
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            FirebaseFirestore.getInstance().collection("users").document(uid).update(
                "is_vip", true,
                "vip_plan", planId,
                "vip_expiry", System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000)
            )
        }

        Toast.makeText(this, "Success! You are now a VIP member. 👑", Toast.LENGTH_LONG).show()
        finish()
    }
}
