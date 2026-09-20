package bankal_deir.com.common
import bankal_deir.com.R

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.android.material.textfield.TextInputEditText

/**
 * Shared 4-digit confirmation step. Sends the code as a notification, then asks the
 * user to type it back. [onVerified] only runs once the correct code is entered, so
 * callers can wrap any money-moving action with it.
 */
object OtpGate {

    private const val NOTIF_ID = 4202
    private const val CHANNEL_ID = "otp_channel"

    fun require(
        activity: Activity,
        confirmText: String = "Confirm payment",
        onCancelled: () -> Unit = {},
        onVerified: () -> Unit
    ) {
        var code = randomCode()
        postCode(activity, code)

        val dialog = Dialog(activity)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_otp)
        dialog.setCancelable(false)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            (activity.resources.displayMetrics.widthPixels * 0.92).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val edtOtp = dialog.findViewById<TextInputEditText>(R.id.edtOtp)
        val btnVerify = dialog.findViewById<Button>(R.id.btnVerifyOtp)
        val btnCancel = dialog.findViewById<Button>(R.id.btnCancelOtp)
        val txtResend = dialog.findViewById<TextView>(R.id.txtResendOtp)
        val sheet = dialog.findViewById<View>(R.id.otpSheet)

        btnVerify.text = confirmText

        txtResend.setOnClickListener {
            code = randomCode()
            postCode(activity, code)
            Toast.makeText(activity, "New code sent", Toast.LENGTH_SHORT).show()
        }

        btnVerify.setOnClickListener {
            if (edtOtp.text.toString() == code) {
                NotificationManagerCompat.from(activity).cancel(NOTIF_ID)
                dialog.dismiss()
                onVerified()
            } else {
                edtOtp.error = "Incorrect code"
                shake(sheet)
            }
        }

        btnCancel.setOnClickListener {
            NotificationManagerCompat.from(activity).cancel(NOTIF_ID)
            dialog.dismiss()
            onCancelled()
        }

        dialog.setOnShowListener { animateIn(sheet) }
        dialog.show()
    }

    private fun randomCode() = (1000..9999).random().toString()

    private fun postCode(activity: Activity, code: String) {
        val nm = activity.getSystemService(Activity.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Verification codes", NotificationManager.IMPORTANCE_HIGH)
            )
        }
        val notif = NotificationCompat.Builder(activity, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_lock)
            .setColor(0xFF7A1B2E.toInt()) // enamel oxblood accent
            .setContentTitle("Bank Al-Deir · verification code")
            .setContentText("Your code is $code — do not share it with anyone.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                activity, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

        if (granted) {
            NotificationManagerCompat.from(activity).notify(NOTIF_ID, notif)
        } else {
            // Notifications are off — show the code so the user isn't locked out.
            Toast.makeText(activity, "Your code: $code", Toast.LENGTH_LONG).show()
        }
    }

    /** Asks for notification permission if we don't have it yet. */
    fun ensurePermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                activity, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            activity.requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 991)
        }
    }

    private fun animateIn(sheet: View?) {
        sheet ?: return
        val density = sheet.resources.displayMetrics.density
        sheet.translationY = 60f * density
        sheet.alpha = 0f
        sheet.animate().translationY(0f).alpha(1f).setDuration(260)
            .setInterpolator(DecelerateInterpolator(1.6f)).start()
    }

    private fun shake(view: View?) {
        view ?: return
        android.animation.ObjectAnimator.ofFloat(
            view, View.TRANSLATION_X, 0f, -16f, 16f, -10f, 10f, 0f
        ).apply { duration = 400; start() }
    }
}
