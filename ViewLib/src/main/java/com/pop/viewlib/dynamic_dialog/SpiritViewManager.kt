package com.pop.viewlib.dynamic_dialog

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.graphics.RectF
import android.os.Build
import android.util.Log
import android.util.Size
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import android.view.animation.AccelerateInterpolator
import android.view.animation.AnticipateInterpolator
import android.view.animation.Interpolator
import android.widget.FrameLayout
import androidx.annotation.RequiresApi
import androidx.core.view.contains
import com.pop.viewlib.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.sqrt


/**
 * Dynamic Island Manager
 * [context] Context For get WindowManager & create rootView
 */
@Deprecated("instead of [QuickFloatViewManager] ")
@SuppressLint("ClickableViewAccessibility")
@RequiresApi(Build.VERSION_CODES.O)
class SpiritViewManager(private val context: Context) : View.OnTouchListener,
    CoroutineScope by CoroutineScope(Dispatchers.Main) {

//    companion object : SingletonHolder<SpiritViewManager, Context>(::SpiritViewManager)

    private val TAG = "SpiritViewManager"

    private var lastChangeTimeStamp: Long = 0L

    /**
     * default layoutParams for FloatView
     */
    private val defaultLp = LayoutParams().apply {
        type = LayoutParams.TYPE_APPLICATION_OVERLAY
        format = PixelFormat.TRANSPARENT
        flags =
            LayoutParams.FLAG_NOT_TOUCH_MODAL or LayoutParams.FLAG_NOT_FOCUSABLE or LayoutParams.FLAG_LAYOUT_IN_SCREEN or LayoutParams.FLAG_HARDWARE_ACCELERATED or LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
        gravity = Gravity.TOP or Gravity.CENTER
        width = ViewGroup.LayoutParams.WRAP_CONTENT
        height = ViewGroup.LayoutParams.WRAP_CONTENT
//        layoutInDisplayCutoutMode = LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        windowAnimations = androidx.constraintlayout.widget.R.anim.abc_grow_fade_in_from_bottom
        y = 20
    }


    private var preViewSize = Size(0, 0)
    private var currentViewSize = Size(0, 0)

    private val currentViewList: Array<View?> = arrayOfNulls(2)

    private val preViewList: Array<View?> = arrayOfNulls(2)

    private val windowManager: WindowManager by lazy { context.getSystemService(Context.WINDOW_SERVICE) as WindowManager }

    private val rootView: FrameLayout by lazy {
        FrameLayout(context).apply {
            setBackgroundResource(R.drawable.spirit_view_bg)
        }
    }

    private lateinit var rootViewLayoutParams: LayoutParams

    private var timerJob: Job? = null

    private val isShowing = AtomicBoolean(false)
    private val inHideProcess = AtomicBoolean(false)

    /**
     * if u want to show View, must init
     */
    fun init(
        backgroundResource: Int = R.drawable.spirit_view_bg,
        layoutParams: LayoutParams = defaultLp
    ): Boolean {
        lastChangeTimeStamp = System.currentTimeMillis()
        rootViewLayoutParams = layoutParams
        rootView.setBackgroundResource(backgroundResource)
        rootView.setOnTouchListener(this)
        runCatching {
            windowManager.addView(rootView, rootViewLayoutParams)
        }.onFailure {
            Log.e(TAG, "init rootView already has parent!")
            return false
        }.onSuccess {
            rootView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
            preViewSize = Size(rootView.measuredWidth, rootView.measuredHeight)
            launch { slideUp(100) {} }
            return true
        }
        return false
    }


    /**
     * show View
     * [childView] View to show
     * [hideDelay] hide delay time
     * [interpolator] animation interpolator
     *
     */
    fun showView(
        childView: View,
        hideDelay: Long = 5000,
        interpolator: Interpolator = AccelerateInterpolator(1F),
    ) {
        if (inHideProcess.get()){
            return
        }
        if (System.currentTimeMillis() - lastChangeTimeStamp < 500) return
        lastChangeTimeStamp = System.currentTimeMillis()
        stopTimer(1)
        if (rootView.contains(childView) || currentViewList[0] == childView) {
            Log.i(TAG, "showView-update ${isShowing.get()}")
            return
        } else {
            Log.i(TAG, "showView-init ${isShowing.get()}")
        }

        currentViewList[0] = childView
        childView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        currentViewSize = Size(childView.measuredWidth, childView.measuredHeight)
        currentViewList[1] = null

        if (!isShowing.getAndSet(true)) {
            launch {
                rootView.removeAllViews()
                slideDown(100) {
                    windowManager.updateViewLayout(rootView, rootView.layoutParams.apply {
                        width = currentViewSize.width
                        height = currentViewSize.height
                    })
                    rootView.addView(childView)
                    updateViewSize()
                }
            }
        } else {
            updateView(interpolator)
        }
        resetTimer(hideDelay, 1)
    }

    private fun resetTimer(hideDelay: Long, flag: Int) {
        Log.d(TAG, "resetTimer:$flag")
        timerJob?.cancel()

        timerJob = launch {
            delay(hideDelay)
            dismiss()
        }
    }

    fun stopTimer(flag: Int = 0) {
        Log.d(TAG, "stopTimer:$flag")
        timerJob?.cancel()
    }

    private fun updateView(interpolator: Interpolator) {
        val widthUpdater = if (preViewSize.width != currentViewSize.width) {
            ValueAnimator.ofInt(preViewSize.width, currentViewSize.width).apply {
                addUpdateListener {
                    CoroutineScope(Dispatchers.IO).launch {
                        val target = it.animatedValue as Int
                        if (abs(target - rootView.layoutParams.width) > 5) {
                            withContext(Dispatchers.Main) {
                                if (isShowing.get()) {
                                    windowManager.updateViewLayout(
                                        rootView,
                                        rootView.layoutParams.apply {
                                            width = target
                                        })
                                }
                            }
                        }
                        cancel()
                    }

                }
                this.interpolator = interpolator
            }
        } else null

        val heightUpdater = if (preViewSize.height != currentViewSize.height) {
            ValueAnimator.ofInt(preViewSize.height, currentViewSize.height).apply {
                addUpdateListener {
                    CoroutineScope(Dispatchers.IO).launch {
                        val target = it.animatedValue as Int
                        if (abs(target - rootView.layoutParams.height) > 2) {
                            withContext(Dispatchers.Main) {
                                if (isShowing.get()) {
                                    windowManager.updateViewLayout(
                                        rootView,
                                        rootView.layoutParams.apply {
                                            height = target
                                        })
                                }
                            }
                        }
                        cancel()
                    }
                }
                this.interpolator = interpolator
            }
        } else null

        val animatorSet = AnimatorSet()
        if (widthUpdater != null && heightUpdater != null) {
            animatorSet.playTogether(heightUpdater, widthUpdater)
        } else {
            heightUpdater?.let { animatorSet.playTogether(it) }
            widthUpdater?.let { animatorSet.playTogether(it) }
        }
        animatorSet.addListener(object : Animator.AnimatorListener {

            override fun onAnimationStart(animation: Animator) {
                preViewList.asList().forEach {
                    it?.let { v ->
                        if (rootView.contains(v)) {
                            CoroutineScope(Dispatchers.Main).launch {
                                rootView.removeAllViews()
                                cancel()
                            }
                        }
                    }
                }
            }

            override fun onAnimationEnd(animation: Animator) {
                updateViewSize()
                currentViewList.asList().reversed().forEach {
                    it?.let { v ->
                        if (!rootView.contains(v)) {
                            CoroutineScope(Dispatchers.Main).launch {
                                rootView.removeView(v)
                                rootView.addView(v)
                                cancel()
                            }
                        }
                    }
                }
            }

            override fun onAnimationCancel(animation: Animator) {
                //updateViewSize()
            }

            override fun onAnimationRepeat(animation: Animator) {

            }

        })

        val widthDiff = (preViewSize.width - currentViewSize.width).toDouble()
        val heightDiff = (preViewSize.height - currentViewSize.height).toDouble()
        val distance = sqrt(widthDiff * widthDiff + heightDiff * heightDiff).toLong()

        Log.e(TAG, "updateView: $distance")
        animatorSet.duration = if (distance != 0L) distance else 100L
        animatorSet.start()

    }

    private fun updateViewSize() {
        preViewSize = currentViewSize
        preViewList[0] = currentViewList[0]
        preViewList[1] = currentViewList[1]
    }

    fun dismiss() {
        launch {
            Log.d(TAG, "dismiss: ${isShowing.get()}")
            if (isShowing.get()) {
                inHideProcess.set(true)
                stopTimer(3)
                Log.d(TAG, "dismiss: ")
                slideUp(200) {
                    rootView.removeAllViews()
                    windowManager.updateViewLayout(rootView, rootViewLayoutParams.apply {
                        width = 1
                        height = 1
                    })
                    currentViewSize = Size(1, 1)
                    preViewSize = Size(0, 0)
                    currentViewList[0] = null
                    isShowing.set(false)
                    inHideProcess.set(false)
                }
            }
        }
    }

    private suspend fun slideUp(duration: Long, block: suspend () -> Unit) {
        if (rootView.y < 0) return
        ObjectAnimator.ofFloat(rootView, "translationY", 0F, -1000F).apply {
            interpolator = AnticipateInterpolator(1F)
            this.duration = duration
        }.start()
        delay(duration)
        block.invoke()
    }

    private suspend fun slideDown(duration: Long, block: suspend () -> Unit) {
        block.invoke()
        delay(100)
        ObjectAnimator.ofFloat(rootView, "translationY", -1000F, 0F).apply {
            this.duration = 100
        }.start()
    }

    /**
     * 计算指定的 View 在屏幕中的坐标。
     */
    private fun calcViewScreenLocation(view: View): RectF {
        val location = IntArray(2)
        // 获取控件在屏幕中的位置，返回的数组分别为控件左顶点的 x、y 的值
        view.getLocationOnScreen(location)
        return RectF(
            location[0].toFloat(),
            location[1].toFloat(),
            (location[0] + view.width).toFloat(),
            (location[1] + view.height).toFloat()
        )
    }

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        Log.d(TAG, "onTouch: action= ${event?.action} rawX= ${event?.rawX} rawY= ${event?.rawY}")

        if (event == null) return false

        val isTouching = calcViewScreenLocation(rootView).contains(event.rawX, event.rawY)

        Log.d(TAG, "onTouch: inside= $isTouching ")

        if (event.action == MotionEvent.ACTION_OUTSIDE) {
            launch { dismiss() }
        } else {
            if (isTouching) resetTimer(2000,2)
        }
        return false
    }

    fun release() {
        launch {
            dismiss()
            windowManager.removeView(rootView)
        }
    }

    fun onThemeChange() {
        rootView.setBackgroundResource(R.drawable.spirit_view_bg)
    }
}