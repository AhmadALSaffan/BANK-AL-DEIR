package bankal_deir.com

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import com.journeyapps.barcodescanner.ViewfinderView

class CustomViewfinderView(context: Context, attrs: AttributeSet?) : ViewfinderView(context, attrs) {
    override fun onDraw(canvas: Canvas) {
        // Draw nothing — removes red line, frame, and dots completely
    }
}