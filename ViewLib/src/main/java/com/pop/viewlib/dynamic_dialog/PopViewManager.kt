package com.pop.viewlib.dynamic_dialog

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.PixelFormat
import android.graphics.RectF
import android.os.Build
import android.util.Log
import android.util.Size
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup.LayoutParams
import android.view.WindowManager
import android.view.animation.AnticipateOvershootInterpolator
import android.view.animation.Interpolator
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.core.animation.addListener
import androidx.core.view.isVisible
import com.pop.viewlib.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.WeakHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

@RequiresApi(Build.VERSION_CODES.O)
class PopViewManager(context: Context) : View.OnTouchListener,
    CoroutineScope by CoroutineScope(Dispatchers.Main) {

    companion object {
        const val TAG = "PopViewManager"
    }


    private val _defaultLayoutParams by lazy {
        WindowManager.LayoutParams().apply {
            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            format = PixelFormat.TRANSPARENT
            flags =
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
            gravity = Gravity.TOP or Gravity.CENTER
            width = LayoutParams.WRAP_CONTENT
            height = LayoutParams.WRAP_CONTENT
//        layoutInDisplayCutoutMode = LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            windowAnimations = androidx.constraintlayout.widget.R.anim.abc_grow_fade_in_from_bottom
            y = -1000
        }
    }

    /**
     * lastViewSize
     */
    private var preSize = Size(0, 0)

    /**
     * current View Size
     */
    private var curSize = Size(0, 0)

    private val mWindowManager by lazy { context.getSystemService(Context.WINDOW_SERVICE) as WindowManager }

    private val mOnShowing = AtomicBoolean(false)

    private val mOnHiding = AtomicBoolean(false)

    private lateinit var mLayoutParams: LayoutParams

    private val rootView = AtomicReference<View?>(null)


    fun initComponent(
        @DrawableRes background: Int = R.drawable.spirit_view_bg,
        layoutParams: WindowManager.LayoutParams = _defaultLayoutParams
    ) {
        mLayoutParams = layoutParams
    }

    fun pop(
        view: View,
        displayDuration: Long = 2000,
        interpolator: Interpolator = AnticipateOvershootInterpolator(0.5F)
    ) {

        launch {
            view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)

            if (!mOnShowing.get()) {
                createView(view)
            } else {
//                updateView(view)
                rootView.get()?.let { updateView(it, view) }
            }
        }
    }


    private fun createView(view: View) {
        rootView.set(view)
        view.setOnTouchListener(this@PopViewManager)
        preSize = curSize
        if (!view.isAttachedToWindow) {
            mWindowManager.addView(view, mLayoutParams.apply {
                width = view.measuredWidth
                height = view.measuredHeight
            })
        }
        launch {
            rootView.get()?.run {
                ObjectAnimator.ofFloat(this, "translationY", -1000F, 20F).apply {
                    this.duration = 500
                    this.interpolator = interpolator
                    this.addListener(
                        onEnd = {
                            mOnShowing.set(true)
                            view.resetDismissTimer(
                                duration = 2000,
                                windowManager = mWindowManager,
                                doOnStart = {
                                    mOnHiding.set(true)
                                },
                                doOnEnd = {
                                    onDismiss()
                                })
                            curSize = Size(view.measuredWidth, view.measuredHeight)
                        }
                    )
                }.start()
            }
        }
    }

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        v?.performClick()
        Log.d(TAG, "onTouch: action= ${event?.action} rawX= ${event?.rawX} rawY= ${event?.rawY}")

        if (event == null) return false

        val isTouching = rootView.get()?.run {
            calcViewScreenLocation(this).contains(event.rawX, event.rawY)
        } ?: false

        Log.d(TAG, "onTouch: inside= $isTouching ")

        if (event.action == MotionEvent.ACTION_OUTSIDE || isTouching.not()) {
            rootView.get()?.dismiss(
                windowManager = mWindowManager,
                doOnStart = {
                    mOnHiding.set(true)
                },
                doOnEnd = {
                    onDismiss()
                })
        } else {
            if (isTouching) rootView.get()
                ?.resetDismissTimer(
                    duration = 2000,
                    windowManager = mWindowManager,
                    doOnStart = {
                        mOnHiding.set(true)
                    },
                    doOnEnd = {
                        onDismiss()
                    })
        }
        return false
    }

    private fun onDismiss() {
        mOnShowing.set(false)
        mOnHiding.set(false)
        this@PopViewManager.rootView.set(null)
    }

    private fun calcViewScreenLocation(view: View): RectF {
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        return RectF(
            location[0].toFloat(),
            location[1].toFloat(),
            (location[0] + view.width).toFloat(),
            (location[1] + view.height).toFloat()
        )
    }

    fun updateView(fromView: View, toView: View) {
        val toLayoutParams = WindowManager.LayoutParams().apply {
            copyFrom(_defaultLayoutParams)
            width = curSize.width
            height = curSize.height
            y = 20
        }

        toView.y = 0F

        // Create PropertyValuesHolder for width and height
        val widthHolder =
            PropertyValuesHolder.ofInt("width", curSize.width, toView.measuredWidth)
        val heightHolder =
            PropertyValuesHolder.ofInt("height", curSize.height, toView.measuredHeight)

        // Create ValueAnimator
        val animator = ValueAnimator.ofPropertyValuesHolder(widthHolder, heightHolder)
        animator.duration = 500 // Animation duration 1 second
        animator.interpolator = AnticipateOvershootInterpolator(1.5F)

        animator.addUpdateListener { animation ->
            val width = animation.getAnimatedValue("width") as Int
            val height = animation.getAnimatedValue("height") as Int
            toLayoutParams.width = width
            toLayoutParams.height = height
            mWindowManager.updateViewLayout(toView, toLayoutParams)
        }

        animator.addListener(
            onStart = {
                mWindowManager.addView(toView, toLayoutParams)
                toView.setOnTouchListener(this)
            },
            onEnd = {
                mWindowManager.removeView(fromView)
                curSize = Size(toView.measuredWidth, toView.measuredHeight)
                rootView.set(toView)
                toView.resetDismissTimer(
                    duration = 2000,
                    windowManager = mWindowManager,
                    doOnStart = { mOnHiding.set(true) },
                    doOnEnd = { onDismiss() }
                )
            }
        )

        animator.start()
    }

}

fun View.dismiss(
    windowManager: WindowManager,
    doOnStart: () -> Unit = {},
    doOnEnd: () -> Unit = {}
) {
    ObjectAnimator.ofFloat(this, "translationY", 20F, -1000F).apply {
        this.interpolator = AnticipateOvershootInterpolator(0.5F)
        this.duration = 500
        addListener(
            onStart = { doOnStart() },
            onEnd = {
                Log.e("PopViewManager", "dismiss: onEnd")
                setOnTouchListener(null)
                if (isAttachedToWindow) {
                    windowManager.removeViewImmediate(this@dismiss)
                }
                doOnEnd()
            }
        )
    }.start()
}

private val viewTimerJobMap = WeakHashMap<View, Job?>()

// 使用扩展属性为 View 添加 timerJob 属性
var View.timerJob: Job?
    get() = viewTimerJobMap[this]
    set(value) {
        viewTimerJobMap[this] = value
    }

fun View.resetDismissTimer(
    duration: Long,
    windowManager: WindowManager,
    doOnStart: () -> Unit = {},
    doOnEnd: () -> Unit = {}
) {
    this.timerJob?.cancel()
    this.timerJob = CoroutineScope(Dispatchers.Main).launch {
        delay(duration)
        dismiss(windowManager, doOnStart, doOnEnd)
    }
}

fun View.stopTimer() {
    this.timerJob?.cancel()
}
