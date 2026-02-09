package bankal_deir.com.Fatora.Data

data class PaymentTransaction(
    var transactionNumber: String = "",  // Primary key: SGI/SGF/SGD/SMP/SMR/SEL/SUN/SUF/SUH + 17 random numbers
    var senderUserId: String = "",
    var amount: Double = 0.0,
    var date: String = "",
    var transactionType: String = "",  // "SGI", "SGF", "SGD", etc.

    var orderNumber: String = "",  // For types that need it (10 random digits)
    var referenceNumber: String = "",  // For types that need it (10 random digits)
    var receiverWalletID: String = "",  // If payments go to specific wallets
    var senderWalletID: String = "",

    var idNumber: String = "",  // For government payments
    var mobileNumber: String = "",  // For mobile payments
    var passportType: String = "",  // "fast" or "slow"
    var provider: String = "",  // "MTN" or "Syriatel"
    var company: String = "",  // "Green Energy" or "Ministry of Electricity"
    var billNumber: String = "",  // For electricity
    var university: String = "",  // University name
    var studentNumber: String = ""  // For university payments
)