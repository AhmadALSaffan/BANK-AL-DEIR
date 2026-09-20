package bankal_deir.com.transactions

data class transactions(
    val amount: Double = 0.0,
    val date: String = "",
    val receiverWalletID: String = "",
    val senderUserId: String = "",
    val senderWalletID: String = "",
    val transactionNumber: String = ""
) {
    constructor() : this(0.0, "", "", "", "", "")
}

