package bankal_deir.com.sendmoney

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SendMoneyViewModel : ViewModel() {
    sealed class Resource<out T> {
        data class Success<out T>(val data: T) : Resource<T>()
        data class Error(val message: String) : Resource<Nothing>()
        object Loading : Resource<Nothing>()
    }
    private val repo = SendMoneyRepository()

    val balance = MutableStateFlow<Double?>(null)
    val transferState = MutableStateFlow<Resource<String>>(Resource.Loading)

    fun fetchBalance(walletId: String) {
        repo.getWalletBalance(walletId) {
            balance.value = it
        }
    }

    fun sendMoneyFlow(receiverAccount: String, amount: Double) {
        val senderId = repo.getCurrentUserId() ?: return
        repo.getUserWalletId(senderId) { senderWID ->
            if (senderWID == null) {
                transferState.value = Resource.Error("Sender wallet not found")
                return@getUserWalletId
            }
            repo.findReceiverWalletId(receiverAccount) { receiverWID ->
                if (receiverWID == null) {
                    transferState.value = Resource.Error("Receiver not found")
                    return@findReceiverWalletId
                }
                if (receiverWID == senderWID) {
                    transferState.value = Resource.Error("You cannot send money to yourself")
                    return@findReceiverWalletId
                }
                repo.performTransfer(senderWID, receiverWID, amount) { success, error ->
                    if (success) {
                        val trxNum = repo.saveTransaction(senderWID, receiverWID, amount)
                        transferState.value = Resource.Success(trxNum)
                        fetchBalance(senderWID)
                    } else {
                        transferState.value = Resource.Error(error ?: "Transfer failed")
                    }
                }
            }
        }
    }
}
