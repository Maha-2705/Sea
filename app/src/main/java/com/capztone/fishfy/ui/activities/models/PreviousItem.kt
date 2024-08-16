package com.capztone.fishfy.ui.activities.models

data class PreviousItem(
    var path: String? = null,
    var foodName : String? = null,
    var foodPrice : String? = null,
    var foodDescription : String? = null,
    var foodImage : String? = null,

    var foodQuantity : Int? = null,
    var foodIngredients : String? = null
)
