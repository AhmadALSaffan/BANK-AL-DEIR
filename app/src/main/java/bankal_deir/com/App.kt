package bankal_deir.com

import android.app.Application
import android.content.Context
import android.util.Log
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import java.io.File
import java.security.KeyStore

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        // Delete corrupted Firebase reCAPTCHA keystore entries (fixes "KeysetManager failed")
        try {
            val ks = KeyStore.getInstance("AndroidKeyStore")
            ks.load(null)
            ks.aliases().toList()
                .filter { it.contains("firebear", ignoreCase = true) }
                .forEach { alias ->
                    ks.deleteEntry(alias)
                    Log.w("App", "Deleted corrupted keystore entry: $alias")
                }
        } catch (e: Exception) {
            Log.e("App", "Keystore cleanup error: ${e.message}")
        }

        // Install debug App Check provider
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
            DebugAppCheckProviderFactory.getInstance()
        )

        // Find and log the debug UUID from Firebase's SharedPreferences
        try {
            val prefsDir = File(applicationInfo.dataDir, "shared_prefs")
            prefsDir.listFiles()
                ?.filter { it.name.contains("appcheck", ignoreCase = true) }
                ?.forEach { file ->
                    val prefsName = file.name.removeSuffix(".xml")
                    val prefs = getSharedPreferences(prefsName, Context.MODE_PRIVATE)
                    prefs.all.forEach { (key, value) ->
                        Log.w("★ APP_CHECK_UUID ★", "==============================================")
                        Log.w("★ APP_CHECK_UUID ★", "Key  : $key")
                        Log.w("★ APP_CHECK_UUID ★", "Value: $value")
                        Log.w("★ APP_CHECK_UUID ★", "→ Add this to Firebase Console → App Check")
                        Log.w("★ APP_CHECK_UUID ★", "==============================================")
                    }
                }
        } catch (e: Exception) {
            Log.e("App", "SharedPrefs scan error: ${e.message}")
        }
    }
}
