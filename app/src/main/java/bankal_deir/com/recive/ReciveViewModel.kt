package bankal_deir.com.recive

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ReciveViewModel : ViewModel() {
    private val repo = ReciveRepository()

    val userData = MutableStateFlow<Map<String, Any?>?>(null)
    val qrBitmap = MutableStateFlow<Bitmap?>(null)
    val loading = MutableStateFlow(false)

    fun loadUserData() {
        val uid = repo.getCurrentUserId() ?: return
        loading.value = true
        repo.getUserData(uid) { data ->
            userData.value = data
            loading.value = false
            val accountNumber = data?.get("accountNumber")?.toString() ?: ""
            if (accountNumber.isNotEmpty()) {
                qrBitmap.value = removeBackground(getBitmapFromString(accountNumber))
            }
        }
    }

    fun getBitmapFromString(string: String): Bitmap? {
        return try {
            val multiFormatWriter = com.google.zxing.MultiFormatWriter()
            val bitMatrix = multiFormatWriter.encode(string, com.google.zxing.BarcodeFormat.QR_CODE, 250, 250)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                }
            }
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    fun removeBackground(bitmap: Bitmap?): Bitmap? {
        bitmap ?: return null
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        for (i in pixels.indices) {
            if (pixels[i] == android.graphics.Color.WHITE) {
                pixels[i] = android.graphics.Color.TRANSPARENT
            }
        }
        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
    }
}