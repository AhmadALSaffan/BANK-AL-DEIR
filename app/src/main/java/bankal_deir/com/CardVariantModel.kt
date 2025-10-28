package bankal_deir.com

data class CardVariantModel(
    val variantName: String,
    val variantDescription: String,
    val imageResource: Int,
    val fees: Int,
    var isSelected: Boolean = false
)
