package com.capztone.seafishfy.ui.activities.models

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class  RetrieveQuantity(
    var foodName: String? = "",
    var foodPrice: Int = 0,
    var foodImage: String? = "",
    var foodQuantity: Int = 0,
    var foodDescription: String? = "",
    var shopName: String? = ""
)
