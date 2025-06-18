package com.example.uberprofitabilityanalyzer

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.uberprofitabilityanalyzer.databinding.OverlayLayoutBinding

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private lateinit var binding: OverlayLayoutBinding // Using ViewBinding

    private var params: WindowManager.LayoutParams? = null

    companion object {
        const val TAG = "OverlayService"
        const val ACTION_SHOW = "com.example.uberprofitabilityanalyzer.ACTION_SHOW"
        const val ACTION_HIDE = "com.example.uberprofitabilityanalyzer.ACTION_HIDE"
        const val EXTRA_IS_PROFITABLE = "extra_is_profitable"
        const val EXTRA_DETAILS_TEXT = "extra_details_text"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "OverlayService onCreate")
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val inflater = LayoutInflater.from(this)
        binding = OverlayLayoutBinding.inflate(inflater)
        overlayView = binding.root

        val layoutFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlags,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH, // To detect taps outside
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100 // Initial position X
            y = 100 // Initial position Y
        }

        // Optional: Make the overlay draggable (basic implementation)
        // setupTouchListener()
    }

    /*
    // Basic drag functionality (can be refined)
    private fun setupTouchListener() {
        overlayView?.setOnTouchListener(object : View.OnTouchListener {
            private var initialX: Int = 0
            private var initialY: Int = 0
            private var initialTouchX: Float = 0.toFloat()
            private var initialTouchY: Float = 0.toFloat()

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                if (params == null) return false
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params!!.x
                        initialY = params!!.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        // Optional: Handle click/tap here if not dragging
                        // if (Math.abs(event.rawX - initialTouchX) < 5 && Math.abs(event.rawY - initialTouchY) < 5) {
                        //    // It's a tap
                        // }
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params!!.x = initialX + (event.rawX - initialTouchX).toInt()
                        params!!.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager.updateViewLayout(overlayView, params)
                        return true
                    }
                }
                return false
            }
        })
    }
    */

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand action: ${intent?.action}")
        when (intent?.action) {
            ACTION_SHOW -> {
                val isProfitable = intent.getBooleanExtra(EXTRA_IS_PROFITABLE, false)
                val details = intent.getStringExtra(EXTRA_DETAILS_TEXT) ?: "N/A"
                showOverlay(isProfitable, details)
            }
            ACTION_HIDE -> {
                hideOverlay()
            }
        }
        return START_NOT_STICKY
    }


    private fun showOverlay(isProfitable: Boolean, details: String) {
        Log.d(TAG, "showOverlay called. Profitable: $isProfitable, Details: $details")
        if (overlayView?.windowToken == null) {
            try {
                windowManager.addView(overlayView, params)
            } catch (e: Exception) {
                Log.e(TAG, "Error adding overlay view to window manager", e)
                return
            }
        }
        overlayView?.visibility = View.VISIBLE

        if (isProfitable) {
            binding.overlayIcon.setImageResource(R.drawable.ic_profitable_green)
            binding.overlayIcon.contentDescription = getString(R.string.overlay_icon_alt_profitable)

        } else {
            binding.overlayIcon.setImageResource(R.drawable.ic_not_profitable_red)
            binding.overlayIcon.contentDescription = getString(R.string.overlay_icon_alt_not_profitable)
        }
        binding.overlayText.text = details
        Log.d(TAG, "Overlay updated and visible.")
    }

    private fun hideOverlay() {
        Log.d(TAG, "hideOverlay called.")
        if (overlayView != null && overlayView?.windowToken != null) {
            try {
                // Consider just setting visibility to GONE if you want to reuse it quickly
                // overlayView?.visibility = View.GONE
                windowManager.removeView(overlayView)
                overlayView = null // Important to allow re-creation if service is restarted
                Log.d(TAG, "Overlay view removed from window manager.")
            } catch (e: Exception) {
                Log.e(TAG, "Error removing overlay view from window manager", e)
            }
        }
         // Stop the service if it's no longer needed
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "OverlayService onDestroy")
        hideOverlay() // Ensure view is removed
    }

    override fun onBind(intent: Intent): IBinder? {
        return null // Not a bound service
    }
}
