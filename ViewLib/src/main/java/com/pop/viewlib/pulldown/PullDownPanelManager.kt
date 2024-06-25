package com.pop.viewlib.pulldown

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.PointF
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND
import android.view.animation.LinearInterpolator
import androidx.annotation.RequiresApi
import com.pop.toolslib.SingletonHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@RequiresApi(Build.VERSION_CODES.P)
class PullDownPanelManager private constructor(context: Context) : Dialog(context) {
    private val TAG = "PullDownPanelManager"
    private val STANDARD_Y_VELOCITY = 1000
    private val STANDARD_UNITS = 1000
    private val HIDE = -1
    private val SHOW = 1
    private val ANIMATING = 0
    private val HEIGHT = 1800

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
        dimAmount = 0F
        x = 0
        layoutInDisplayCutoutMode = LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
    }

    private var velocityTracker: VelocityTracker? = null
    private var canHandleAction = true

    private val touchDownPointF = PointF()
    private val status = AtomicInteger(HIDE)

    private var isInit = true

    private var dismissJob: Job? = null

    fun init(contentView: View): PullDownPanelManager {

        if (isInit) {
            window?.apply {
                setGravity(Gravity.TOP or Gravity.CENTER)
                setType(LayoutParams.TYPE_APPLICATION)
                setContentView(contentView)
                attributes = attributes.apply {
                    flags = (LayoutParams.FLAG_NOT_TOUCH_MODAL
                            or LayoutParams.FLAG_NOT_FOCUSABLE
                            or LayoutParams.FLAG_LAYOUT_IN_SCREEN
                            or LayoutParams.FLAG_LAYOUT_NO_LIMITS
                            or LayoutParams.FLAG_BLUR_BEHIND
                            or LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS
                            )
                    layoutInDisplayCutoutMode = LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                    width = MATCH_PARENT
                    height = MATCH_PARENT
                    dimAmount = 0F
                    y = -HEIGHT
                    x = 0
                    setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                }
                clearFlags(FLAG_DIM_BEHIND)
            }
            show()
            isInit = false
        }
        return this
    }

    fun display() {
        ValueAnimator.ofInt(-HEIGHT, 0).apply {
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
                        dismissJob?.cancel()
                        dismissJob = CoroutineScope(Dispatchers.Main).launch {
                            delay(2000)
                            hideAnimation()
                        }
                        status.set(SHOW)
                    }

                    -HEIGHT -> {
                        dismissJob?.cancel()
                        status.set(HIDE)
                    }

                    else -> {
                        dismissJob?.cancel()
                        status.set(ANIMATING)
                    }
                }
            }
        }.start()
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        dismissJob?.cancel()
        dismissJob = CoroutineScope(Dispatchers.Main).launch {
            delay(2000)
            hideAnimation()
        }
        return super.dispatchTouchEvent(ev)
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

                val distance = event.rawY - touchDownPointF.y + translationY
                Log.e(
                    TAG,
                    "onTouchEvent: y= ${event.y} distance= $distance currentY= $translationY"
                )

                return (distance in -HEIGHT.toFloat()..0F).run {
                    yes {
                        translationY = Math.round(distance)
                        canHandleAction = true
                        touchDownPointF.set(event.rawX, event.rawY)
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
                            Log.e(
                                TAG,
                                "onTouchEvent - ACTION_UP - ${translationY <= -HEIGHT * 0.4F} "
                            )
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


    private val height: Int
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