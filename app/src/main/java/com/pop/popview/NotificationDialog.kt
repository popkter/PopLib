package com.pop.popview

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.PointF
import android.graphics.drawable.ColorDrawable
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.WindowManager
import android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND
import android.view.animation.LinearInterpolator
import com.pop.popview.databinding.NotificationPanelBinding
import com.pop.viewlib.pulldown.not
import com.pop.viewlib.pulldown.yes
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class NotificationDialog(context: Context) : Dialog(context) {

    companion object{
        private const val TAG = "NotificationDialog"
        private const val STANDARD_Y_VELOCITY = 1000
        private const val STANDARD_UNITS = 1000
        private const val HIDE = -1
        private const val SHOW = 1
        private const val ANIMATING = 0
        private const val HEIGHT = 1800
    }

    private var velocityTracker: VelocityTracker? = null
    private var canHandleAction = true

    private val touchDownPointF = PointF()
    private val status = AtomicInteger(HIDE)


    private val binding by lazy {
        NotificationPanelBinding.inflate(LayoutInflater.from(context))
    }

    private var isInit = true

    fun init() {

        if (isInit){
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

    fun display(){
        ValueAnimator.ofInt(-HEIGHT,0).apply {
            addUpdateListener {
                window?.attributes?.apply {
                    y = it.animatedValue as Int
                    window!!.attributes = this
                }
            }
            duration = 2000L
        }.start()
    }

    private fun showAnimation() {
        animate(translationY, 0)
    }

    private fun hideAnimation() {
        animate(translationY, -HEIGHT, flag = 1)
    }

    private fun animate(from: Int, to: Int, flag: Int = 0) {
        Log.e(TAG, "animate: flag= $flag")
        ValueAnimator.ofInt(from, to).apply {
            interpolator = LinearInterpolator()
            addUpdateListener { animation ->
                val modifierY = animation.animatedValue as Int
                translationY = modifierY
//                this@PopPullDownLayout.translationY = translationY
                Log.e(TAG, "animate translationY= $translationY")
                when (translationY) {
                    0 -> {
                        status.set(SHOW)
                    }

                    -HEIGHT -> {
                        status.set(HIDE)
                    }

                    else -> {
                        status.set(ANIMATING)
                    }
                }
            }
        }.start()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {

        if (inAnimating) {
            return false
        }
        velocityTracker?.addMovement(event)
        velocityTracker?.computeCurrentVelocity(STANDARD_UNITS)
        val yVelocity = velocityTracker?.yVelocity ?: 0F

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                touchDownPointF.set(event.rawX, event.rawY)
                velocityTracker = VelocityTracker.obtain()
                velocityTracker?.clear()
                velocityTracker?.addMovement(event)
                canHandleAction = true
                return true
            }

            MotionEvent.ACTION_MOVE -> {

                val distance = event.rawY - touchDownPointF.y +  translationY
                Log.e(TAG, "onTouchEvent: y= ${event.y} distance= $distance currentY= $translationY")

                return (distance in -HEIGHT.toFloat()..0F).run {
                    yes {
                        translationY = Math.round(distance)
                        canHandleAction = true
                        touchDownPointF.set(event.rawX,event.rawY)
                        true
                    }
                    not {
                        // current is show,if still pull down,customized action or current is hide.still pick up
                        // TODO:
                        canHandleAction = false
                        false
                    } == true
                }
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                if (!canHandleAction) {
                    return false
                }
                return (abs(yVelocity) > STANDARD_Y_VELOCITY).run {
                    yes {
                        isShow.yes {
                            hideAnimation()
                        }
                        isHide.yes {
                            showAnimation()
                        }
                        return@yes true
                    }
                    not {
                        isShow.yes {
                            Log.e(TAG, "onTouchEvent - ACTION_UP - ${translationY <= -HEIGHT * 0.4F} ")
                            (translationY <= -HEIGHT * 0.4F).apply {
                                yes {
                                    hideAnimation()
                                }
                                not {
                                    showAnimation()
                                }
                            }
                        }
                        isHide.yes {
                            (translationY >= -HEIGHT * 0.6F).apply {
                                yes {
                                    showAnimation()
                                }
                                not {
                                    hideAnimation()
                                }
                            }
                        }
                        return@not false
                    } == true
                }
            }
        }
        return true
    }

    val isHide: Boolean
        get() = status.get() == HIDE

    val isShow: Boolean
        get() = status.get() == SHOW

    private val inAnimating: Boolean
        get() = status.get() == ANIMATING


    private val height:Int
        get() = window?.attributes?.height ?: 0

    private var translationY: Int
        get() = window?.attributes?.y ?: 0
        set(value) {
            window!!.attributes!!.apply {
                y = value
                window!!.attributes = this
            }
            window!!.setBackgroundBlurRadius(Math.round(max(50F, min(value / HEIGHT.toFloat() * 50,0F))))
        }

}