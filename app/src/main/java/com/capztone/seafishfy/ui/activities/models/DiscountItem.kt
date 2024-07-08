package com.capztone.seafishfy.ui.activities.models

data class DiscountItem(
    var foodNames: ArrayList<String>? =null,
    val foodPrices: String? = null,
    val foodDescriptions: String? = null,
    val foodImages: String? = null,
    val quantity: Int? = null,
    val discounts: String? = null,
    val productQuantity:String?=null,
    var path: String? = null,
)

