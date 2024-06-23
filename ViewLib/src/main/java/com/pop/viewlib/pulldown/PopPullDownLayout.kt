package com.pop.viewlib.pulldown

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PointF
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import com.pop.viewlib.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs

@RequiresApi(Build.VERSION_CODES.S)
class PopPullDownLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr),
    CoroutineScope by CoroutineScope(Dispatchers.Main + SupervisorJob()) {

    private var mBackgroundResource: Int = 0
    private var mDefaultHide = false

    private var isInitLayoutComplete = false
    private var velocityTracker: VelocityTracker? = null
    private var canHandleAction = true

    private val touchDownPointF = PointF()
    private val status = AtomicInteger(HIDE)
    private val background by lazy {
        ImageView(context).apply {
            layoutParams = LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = ImageView.ScaleType.CENTER_CROP
            visibility = GONE // 默认隐藏
        }
    }

    companion object {
        private const val TAG = "PopPullDownLayout"
        private const val STANDARD_Y_VELOCITY = 1000
        private const val STANDARD_UNITS = 1000
        private const val HIDE = -1
        private const val SHOW = 1
        private const val ANIMATING = 0
    }

    init {
        initAttrs(attrs)
        // 将背景视图添加到布局中
        addView(background, 0)


        /*        (context as Activity).window.apply {
            attributes.apply {
                flags = FLAG_BLUR_BEHIND
                blurBehindRadius = 20
            }
        }*/

    }


    private fun initAttrs(attrs: AttributeSet?) {
        attrs ?: return
        val typeArray = context.obtainStyledAttributes(attrs, R.styleable.PopPullDownLayout)
        mDefaultHide = typeArray.getBoolean(R.styleable.PopPullDownLayout_hide, false)
        mBackgroundResource = typeArray.getResourceId(R.styleable.PopPullDownLayout_blurImage, 0)
        if (mBackgroundResource != 0) {
            background.apply {
                setImageResource(mBackgroundResource)
//                setBackgroundColor(Color.TRANSPARENT)
                visibility = VISIBLE
            }
        }
        typeArray.recycle()
    }


    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (mDefaultHide && !isInitLayoutComplete) {
            isInitLayoutComplete = true
            this.translationY = (-height).toFloat()
        }
    }

    fun hide() {
        hideAnimation()
    }

    fun hideImmediately() {
        translationY = -height.toFloat()
        status.set(HIDE)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event ?: return false
        if (!mDefaultHide) {
            return super.onTouchEvent(event)
        }
        Log.e(TAG, "onTouchEvent: $inAnimating")
        if (inAnimating) {
            return false
        }
        velocityTracker?.addMovement(event)
        velocityTracker?.computeCurrentVelocity(STANDARD_UNITS)
        val yVelocity = velocityTracker?.yVelocity ?: 0F

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {

                touchDownPointF.set(event.x, event.y)
                velocityTracker = VelocityTracker.obtain()
                velocityTracker?.clear()
                velocityTracker?.addMovement(event)
                canHandleAction = true
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val distance = event.y - touchDownPointF.y + translationY
                Log.e(
                    TAG,
                    "onTouchEvent -ACTION_MOVE - distance: $distance canTranslationY: ${distance in -height.toFloat()..0F}"
                )
                return (distance in -height.toFloat()..0F).run {
                    yes {
                        translationY = distance
                        touchDownPointF.set(event.x, event.y)
                        canHandleAction = true
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
                Log.e(
                    TAG,
                    "onTouchEvent - ACTION_UP - overSpeed = ${abs(yVelocity) > STANDARD_Y_VELOCITY} status= $status canHandleAction= $canHandleAction "
                )
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
                                "onTouchEvent - ACTION_UP - ${translationY <= -height * 0.4F} "
                            )
                            (translationY <= -height * 0.4F).apply {
                                yes {
                                    hideAnimation()
                                }
                                not {
                                    showAnimation()
                                }
                            }
                        }
                        isHide.yes {
                            (translationY >= -height * 0.6F).apply {
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


    @RequiresApi(Build.VERSION_CODES.S)
    private fun showAnimation() {
        animate(translationY, 0F)
/*        val blurMaskEffect = RenderEffect.createBlurEffect(
            20F,
            20F, Shader.TileMode.CLAMP
        )*/

        /*
                val rootView: View = (context as Activity).window.decorView.getRootView()
                rootView.setRenderEffect(blurMaskEffect)*/

    }

    private fun hideAnimation() {
        animate(translationY, -height.toFloat())
    }

    private fun animate(from: Float, to: Float, flag: Int = 0) {
        Log.e(TAG, "animate: flag= $flag")
        ValueAnimator.ofFloat(from, to).apply {
            interpolator = LinearInterpolator()
            addUpdateListener { animation ->
                val translationY = animation.animatedValue as Float
                this@PopPullDownLayout.translationY = translationY
                when (this@PopPullDownLayout.translationY) {
                    0F -> {
                        status.set(SHOW)
                    }

                    -height.toFloat() -> {
                        status.set(HIDE)
                    }

                    else -> {
                        status.set(ANIMATING)
                    }
                }
            }
        }.start()
    }

    val isHide: Boolean
        get() = status.get() == HIDE

    val isShow: Boolean
        get() = status.get() == SHOW

    private val inAnimating: Boolean
        get() = status.get() == ANIMATING



}

fun <T> Boolean.not(action: () -> T): T? {
    return if (!this) {
        action.invoke()
    } else null
}

fun <T> Boolean.yes(action: () -> T): T? {
    return if (this) {
        action.invoke()
    } else null
}