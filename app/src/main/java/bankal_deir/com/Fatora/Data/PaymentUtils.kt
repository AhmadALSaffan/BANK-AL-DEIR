package bankal_deir.com.Fatora.Data

import java.text.SimpleDateFormat
import java.util.*

object PaymentUtils {

    fun generateTransactionNumber(prefix: String): String {
        val randomNumbers = (1..17).map { ('0'..'9').random() }.joinToString("")
        return "$prefix$randomNumbers"
    }

    fun generateOrderNumber(): String {
        return (1..10).map { ('0'..'9').random() }.joinToString("")
    }

    fun generateReferenceNumber(): String {
        return (1..10).map { ('0'..'9').random() }.joinToString("")
    }

    fun getCurrentDate(): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }

    fun isValidIdNumber(id: String): Boolean {
        return id.isNotEmpty() && id.length >= 5
    }

    fun isValidMobileNumber(mobile: String): Boolean {
        return mobile.isNotEmpty() && mobile.length >= 9
    }

    fun isValidStudentNumber(number: String): Boolean {
        return number.isNotEmpty() && number.length >= 4
    }

    fun isBillValid(number: String): Boolean {
        return number.isNotEmpty() && number.length == 9
    }
}