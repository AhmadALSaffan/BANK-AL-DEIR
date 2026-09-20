package bankal_deir.com.ads

/**
 * A promotional card shown above the balance nameplate on the home screen.
 * Populated from the Realtime Database `homeCards` node — see AdsAdapter / MainPage.loadAds.
 */
data class AdItem(
    val title: String = "",
    val subtitle: String = "",
    val imageUrl: String = "",
    val order: Int = 0,
    val active: Boolean = true
)
