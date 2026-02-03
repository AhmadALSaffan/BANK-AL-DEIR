package bankal_deir.com.AmountTopUp

data class transactions(
    var transactionNumber: String = "",
    var senderUserId: String = "",
    var receiverWalletID: String = "",
    var amount: Double = 0.0,
    var date: String = "",
    var transactionType: String = ""
) {
    constructor() : this("", "", "", 0.0, "", "")

    companion object {
        fun generateTransactionNumber(prefix: String): String {
            val randomNumbers = (1..18).map { (0..9).random() }.joinToString("")
            return "$prefix$randomNumbers"
        }


        fun createTopUpTransaction(userId: String, amount: Double, date: String): transactions {
            return transactions(
                transactionNumber = generateTransactionNumber("PLP"),
                senderUserId = userId,
                receiverWalletID = userId, // Same user for top-up
                amount = amount,
                date = date,
                transactionType = "PLP"
            )
        }

        fun createSendTransaction(senderId: String, receiverId: String, amount: Double, date: String): transactions {
            return transactions(
                transactionNumber = generateTransactionNumber("SYP"),
                senderUserId = senderId,
                receiverWalletID = receiverId,
                amount = amount,
                date = date,
                transactionType = "SYP"
            )
        }
    }
}