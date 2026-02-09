package bankal_deir.com.Fatora.Data

object PaymentConstants {
    // Transaction Type Prefixes
    const val PASSPORT_FAST = "SGI"
    const val IMMIGRATION_FINE = "SGF"
    const val NEW_ID = "SGD"
    const val MOBILE_BILL = "SMP"
    const val MOBILE_REFILL = "SMR"
    const val ELECTRICITY = "SEL"
    const val UNIVERSITY_TUITION = "SUN"
    const val UNIVERSITY_FINE = "SUF"
    const val UNIVERSITY_HOSTEL = "SUH"

    // Amounts
    const val PASSPORT_FAST_AMOUNT = 400.0
    const val PASSPORT_SLOW_AMOUNT = 200.0
    const val NEW_ID_AMOUNT = 20.0

    // Passport Types
    const val PASSPORT_TYPE_FAST = "fast"
    const val PASSPORT_TYPE_SLOW = "slow"

    // Mobile Providers
    const val PROVIDER_MTN = "MTN"
    const val PROVIDER_SYRIATEL = "Syriatel"

    // Electricity Companies
    const val COMPANY_GREEN_ENERGY = "Green Energy"
    const val COMPANY_MINISTRY = "Ministry of Electricity"
}