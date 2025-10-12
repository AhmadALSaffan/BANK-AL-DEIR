package bankal_deir.com

data class transactions(
    val amount: Int = 0,
    val date: String = "",
    val receiverWalletID: String = "",
    val senderUserId: String = "",
    val senderWalletID: String = "",
    val transactionNumber: String = ""
) {
    constructor() : this(0, "", "", "", "", "")
}

