package bankal_deir.com

data class CardVariantModel(
    val variantName: String,
    val variantDescription: String,
    val imageResource: Int,
    val fees: Int,
    val type: String = "",
    val fullName: String = "",
    var isSelected: Boolean = false
)
