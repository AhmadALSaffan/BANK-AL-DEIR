package bankal_deir.com.security

import org.mindrot.jbcrypt.BCrypt

object PinHasher {
    fun hashPin(pin: String): String {
        val salt = BCrypt.gensalt(12)
        return BCrypt.hashpw(pin, salt)
    }

    fun verifyPin(pin: String, hash: String): Boolean {
        return try {
            BCrypt.checkpw(pin, hash)
        } catch (e: Exception) {
            false
        }
    }
}