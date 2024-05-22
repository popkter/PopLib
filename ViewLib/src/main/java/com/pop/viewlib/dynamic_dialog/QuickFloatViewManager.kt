package com.pop.viewlib.dynamic_dialog

import android.animation.AnimatorSet
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
import android.view.ViewGroup
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import android.view.animation.AnticipateOvershootInterpolator
import android.view.animation.Interpolator
import android.view.animation.OvershootInterpolator
import androidx.annotation.RequiresApi
import androidx.core.animation.addListener
import androidx.core.animation.doOnEnd
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

@RequiresApi(Build.VERSION_CODES.O)
class QuickFloatViewManager : View.OnTouchListener {

    companion object {
        private const val TAG = "QuickFloatViewManager"
        private const val ORIGIN_Y_COORDINATE = -1000
    }

    private val mainScope by lazy { CoroutineScope(Dispatchers.Main) }


    private val mDefaultLayoutParams = LayoutParams().apply {
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
    }

    /**
     * current view state
     */
    private val viewPhase = MutableStateFlow<FvState>(FvState.OnHide)

    private var popJob: Job? = null

    private var curViewSize = Size(0, 0)

    private var rootView: View? = null

    /**
     * y-coordinate on display
     */
    private var mCoordinateY = 20

    private lateinit var mWindowManager: WindowManager

    private lateinit var mLayoutParams: LayoutParams

    /**
     * [context] for get WindowManager
     * [yCoordinate] y-coordinate on display when view visible
     * [layoutParams] customized WindowManager LayoutParams, if you want to use for system,you should modify flag and type
     */
    fun initComponent(
        context: Context,
        yCoordinate: Int = 20,
        layoutParams: LayoutParams = mDefaultLayoutParams
    ) = init(
        yCoordinate,
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager,
        layoutParams
    )

    /**
     * [windowManager] for alert window
     * [yCoordinate] y-coordinate on display when view visible
     * [layoutParams] customized WindowManager LayoutParams, if you want to use for system,you should modify flag and type
     */
    fun initComponent(
        windowManager: WindowManager,
        yCoordinate: Int = 20,
        layoutParams: LayoutParams = mDefaultLayoutParams
    ) = init(yCoordinate, windowManager, layoutParams)

    private fun init(
        yCoordinate: Int,
        windowManager: WindowManager,
        layoutParams: LayoutParams
    ) {
        mCoordinateY = yCoordinate
        mWindowManager = windowManager
        mLayoutParams = layoutParams
    }

    /**
     * [view] the view you want show
     * [hideDuration] how long the view you want show
     * [interpolator] a interpolator for enter view
     */
    fun pop(
        view: View,
        hideDuration: Long = 5000L,
        interpolator: Interpolator = OvershootInterpolator(1F)
    ) {
        popJob?.cancel()
        popJob = mainScope.launch(Dispatchers.Main) {

            view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)

            viewPhase.asStateFlow().collect {
                when (it) {
                    FvState.InShow,
                    FvState.InHide -> {
                    }

                    FvState.Init,
                    FvState.OnHide -> {
                        createView(view, hideDuration, interpolator)
                        cancel()
                    }

                    FvState.OnShow -> {
                        updateView(view, hideDuration)
                        cancel()
                    }
                }
            }
        }
    }

    fun dismiss() {
        popJob?.cancel()
        rootView?.dismiss()
    }

    private suspend fun createView(view: View, hideDuration: Long, interpolator: Interpolator) {
        viewPhase.emit(FvState.InShow)
        curViewSize = Size(view.measuredWidth, view.measuredHeight)
        view.setOnTouchListener(this)
        view.y = 0F
//        view.alpha = 1F
        if (!view.isAttachedToWindow) {
            mWindowManager.addView(view, mLayoutParams.apply {
                width = view.measuredWidth
                height = view.measuredHeight
                y = -curViewSize.height
            })
        }
        ValueAnimator.ofInt(-curViewSize.height, mCoordinateY).apply {
            this.interpolator = interpolator
            this.duration = curViewSize.height * 2L
            this.addUpdateListener { animator ->
                val temY = animator.animatedValue as Int
                if (temY >= 0) {
                    view.y = 0F
                    mWindowManager.updateViewLayout(view, mLayoutParams.apply {
                        y = temY
                    })
                } else {
                    view.y = temY.toFloat()
                }

                doOnEnd {
                    mainScope.launch(Dispatchers.Default) { viewPhase.emit(FvState.OnShow) }
                    setUpViewEvent(view, hideDuration)
                    view.resetDismissTimer()
                    rootView = view
                }
            }
        }.start()
    }

    private suspend fun updateView(view: View, hideDuration: Long) {
        viewPhase.emit(FvState.InShow)

        //if the view is same as rootView and has been shown,y-coordinate will be modify,need reset.
        view.y = 0F
//        view.alpha = 0F

        val widthHolder = PropertyValuesHolder.ofInt("width", curViewSize.width, view.measuredWidth)
        val heightHolder =
            PropertyValuesHolder.ofInt("height", curViewSize.height, view.measuredHeight)

        val duration = sqrt(
            (curViewSize.width - view.measuredWidth).toDouble()
                .pow(2) + (curViewSize.height - view.measuredHeight).toDouble().pow(2)
        )


/*        val alphaAnimator =
            ValueAnimator.ofFloat(1F, 0F).apply {
                this.duration = max(100L, duration.toLong() * 2 / 3 )
                this.addUpdateListener { animation ->
                    val inVisibleAlpha = animation.animatedValue as Float
                    rootView?.alpha = inVisibleAlpha
                    view.alpha = 1 - inVisibleAlpha
                }
            }*/

        val animator = ValueAnimator.ofPropertyValuesHolder(widthHolder, heightHolder).apply {
            this.duration = max(100L, duration.toLong() * 2 / 3)
            this.interpolator = AnticipateOvershootInterpolator(1F)
            this.addUpdateListener { animation ->
                if (view.isAttachedToWindow) {

                    val width = animation.getAnimatedValue("width") as Int
                    val height = animation.getAnimatedValue("height") as Int


                    val widthDiff = (curViewSize.width - width).toDouble()
                    val heightDiff = (curViewSize.height - height).toDouble()
                    val deltaStep = sqrt(widthDiff.pow(2) + heightDiff.pow(2))

                    if (deltaStep > 10) {
                        mWindowManager.updateViewLayout(
                            view,
                            mLayoutParams.apply {
                                this.width = width
                                this.height = height
                            }
                        )
                        curViewSize = Size(width, height)
                    }
                }
            }
            this.addListener(
                onStart = {
                    mWindowManager.removeView(rootView)
                    mWindowManager.addView(view, mLayoutParams)
                    view.setOnTouchListener(this@QuickFloatViewManager)
                    rootView?.stopTimer()
                },
                onEnd = {
                    mWindowManager.updateViewLayout(
                        view,
                        mLayoutParams.apply {
                            width = view.measuredWidth;
                            height = view.measuredHeight
                        })
                    rootView = view
                    setUpViewEvent(view, hideDuration)
                    view.resetDismissTimer()
                    curViewSize = Size(view.measuredWidth, view.measuredHeight)
                    MainScope().launch(Dispatchers.Default) { viewPhase.emit(FvState.OnShow) }
                }
            )
        }


        if (!view.isAttachedToWindow) {
            /*val animatorSet = AnimatorSet()
            animatorSet.playTogether(animator, alphaAnimator)
            animatorSet.start()*/
            animator.start()
        }

    }


    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        v?.performClick()

        if (event == null) return false

        if (viewPhase.value in arrayOf(FvState.OnShow)) {
            val isTouching = rootView?.run {
                calcViewScreenLocation(this).contains(event.rawX, event.rawY)
            } ?: false

            Log.d(TAG, "onTouch: inside= $isTouching ")

            if (event.action == MotionEvent.ACTION_OUTSIDE || isTouching.not()) {
                rootView?.dismiss()
            } else {
                if (isTouching) rootView?.resetDismissTimer()
            }
        }
        return false
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

    private fun setUpViewEvent(view: View, duration: Long) {
        view.dismissDuration = duration
        view.doOnDismissStart = {
            mainScope.launch { viewPhase.emit(FvState.InHide) }
        }
        view.doOnDismissEnd = {
            mainScope.launch { viewPhase.emit(FvState.OnHide) }
        }
        view.hideCoordinateY = -view.measuredHeight
        view.showCoordinateY = mCoordinateY
        view.windowLayoutParams = mLayoutParams
    }
}

