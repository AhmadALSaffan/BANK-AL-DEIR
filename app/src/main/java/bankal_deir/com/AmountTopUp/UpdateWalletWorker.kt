package bankal_deir.com.AmountTopUp

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import kotlinx.coroutines.tasks.await

class UpdateWalletWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val amount = inputData.getDouble("amount", 0.0)
        val walletId = inputData.getString("walletId")

        if (walletId.isNullOrEmpty()) {
            Log.e("UpdateWorker", "WalletID is EMPTY! Cannot update database.")
            return Result.failure()
        }

        return try {
            Log.d("UpdateWorker", "Updating path: wallets/$walletId/balance with amount: $amount")

            val dbRef = FirebaseDatabase.getInstance().getReference("wallets").child(walletId)

            // The wallet Balance is the single main balance (the main card mirrors it).
            val snapshot = dbRef.child("Balance").get().await()
            val currentBalance = snapshot.getValue(Double::class.java) ?: 0.0
            dbRef.child("Balance").setValue(currentBalance + amount).await()

            Log.d("UpdateWorker", "Database update COMPLETED online")
            Result.success()
        } catch (e: Exception) {
            Log.e("UpdateWorker", "Error updating Firebase: ${e.message}")
            Result.retry()
        }
    }
}