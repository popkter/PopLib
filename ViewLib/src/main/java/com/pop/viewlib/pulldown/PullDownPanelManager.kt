package com.pop.viewlib.pulldown

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND
import com.pop.toolslib.SingletonHolder
import com.pop.viewlib.dynamic_dialog.QuickFloatViewManager.Companion.ORIGIN_Y_COORDINATE

class PullDownPanelManager private constructor(context: Context) {

    companion object : SingletonHolder<PullDownPanelManager, Context>(::PullDownPanelManager)

    private val windowManager by lazy { context.getSystemService(Context.WINDOW_SERVICE) as WindowManager }

    private val mDefaultLayoutParams = LayoutParams().apply {
        type = LayoutParams.TYPE_APPLICATION_OVERLAY
        format = PixelFormat.TRANSPARENT
        flags =
            LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    LayoutParams.FLAG_NOT_FOCUSABLE or
                    LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                    LayoutParams.FLAG_HARDWARE_ACCELERATED or
                    LayoutParams.FLAG_BLUR_BEHIND
        gravity = Gravity.TOP or Gravity.CENTER
        width = ViewGroup.LayoutParams.MATCH_PARENT
        height = ViewGroup.LayoutParams.MATCH_PARENT
        windowAnimations = androidx.constraintlayout.widget.R.anim.abc_grow_fade_in_from_bottom
        dimAmount = 0F
        x  = 0
//        setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
//        y = ORIGIN_Y_COORDINATE
    }


    init {

        type = LayoutParams.TYPE_APPLICATION_OVERLAY
        format = PixelFormat.TRANSPARENT
        flags =
            LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    LayoutParams.FLAG_NOT_FOCUSABLE or
                    LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                    LayoutParams.FLAG_HARDWARE_ACCELERATED
        gravity = Gravity.TOP or Gravity.CENTER
        width = ViewGroup.LayoutParams.WRAP_CONTENT
        height = ViewGroup.LayoutParams.WRAP_CONTENT
        windowAnimations = androidx.constraintlayout.widget.R.anim.abc_grow_fade_in_from_bottom
        y = ORIGIN_Y_COORDINATE

        window?.apply {
            setGravity(Gravity.TOP or Gravity.CENTER)
            setType(WindowManager.LayoutParams.TYPE_APPLICATION)
            setContentView(binding.root)
            attributes = attributes.apply {
                flags = (WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        or WindowManager.LayoutParams.FLAG_BLUR_BEHIND
                        or WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS
                        )
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                width = MATCH_PARENT
                height = MATCH_PARENT
                dimAmount = 0F
                y = -HEIGHT
                x = 0
                setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
//                    setBackgroundDrawable(ColorDrawable(0xFF0000FF.toInt()))
            }
            clearFlags(FLAG_DIM_BEHIND)
//                setBackgroundBlurRadius(0)
            /*                decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION*/

        }
        show()
        isInit = false
    }

}