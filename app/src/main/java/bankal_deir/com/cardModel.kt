package bankal_deir.com

data class CardModel(
    val cardnumber: String = "",
    val cardexp: String = "",
    val cardcvv: String = "",
    val cardholder: String = "",
    val cardname: String = "",
    val Balnce: Double = 0.0,
    var isVisible: Boolean = false,
    var pin: String = "",
    var locked: Boolean = false,
    @com.google.firebase.database.Exclude
    var cardKey: String = ""
)
