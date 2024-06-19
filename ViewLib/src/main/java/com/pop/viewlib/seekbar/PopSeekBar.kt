package com.pop.viewlib.seekbar

import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import com.pop.demopanel.view.drawProgressPath
import com.pop.demopanel.view.drawProgressPathNatural
import com.pop.demopanel.view.drawTrackPath
import com.pop.demopanel.view.setGradientShader
import com.pop.viewlib.R
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min


class PopSeekBar : View {

    // TODO: properties to abstract class

    companion object {
        const val TAG = "PopSeekBar"
    }

    private val max = 100
    private var progress = 0

    private lateinit var trackPaint: Paint
    private lateinit var trackColors: IntArray
    private lateinit var trackPath: Path
    private var trackGradientType: Int = 0
    private var trackGradientAngle: Int = 0
    private var trackRadius: Float = 0F

    private lateinit var progressPaint: Paint
    private lateinit var progressColors: IntArray
    private lateinit var progressPath: Path
    private var progressGradientType: Int = 0
    private var progressGradientAngle: Int = 0
    private var progressRadius: Float = 0F

    private lateinit var finalPath: Path

    private var commonRadius = 0F
    private var originWidth = 0
    private var originHeight = 0

    private var isInit = true

    /**
     * whether use natural process modifier
     */
    var naturalProcess = true

    /**
     * progress change listener
     */
    var onProgressChangeListener: ((Int) -> Unit)? = null

    /**
     * whether can response touch down event
     * not suggest to modify it
     */
    var canResponseTouch = false

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        initView(attrs)
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        initView(attrs)
    }

    private fun initView(attrs: AttributeSet?) {
        attrs ?: return
        val typeArray = context.obtainStyledAttributes(attrs, R.styleable.PopSeekBar)

        val progressSolidColor =
            typeArray.getColor(R.styleable.PopSeekBar_progressSolidColor, 0)
        val progressStartSolidColor =
            typeArray.getColor(R.styleable.PopSeekBar_progressStartSolidColor, 0)
        val progressEndSolidColor =
            typeArray.getColor(R.styleable.PopSeekBar_progressEndSolidColor, 0)
        progressGradientType =
            typeArray.getInt(R.styleable.PopSeekBar_progressGradientType, 0)
        progressGradientAngle =
            typeArray.getInt(R.styleable.PopSeekBar_progressGradientAngle, 0)
        progressColors = intArrayOf(progressEndSolidColor, progressStartSolidColor)
        progressRadius = typeArray.getDimension(R.styleable.PopSeekBar_progressRadius, 0F)


        val trackSolidColor =
            typeArray.getColor(R.styleable.PopSeekBar_trackSolidColor, 0)
        val trackStartSolidColor =
            typeArray.getColor(R.styleable.PopSeekBar_trackStartSolidColor, 0)
        val trackEndSolidColor =
            typeArray.getColor(R.styleable.PopSeekBar_trackEndSolidColor, 0)
        trackGradientType =
            typeArray.getInt(R.styleable.PopSeekBar_trackGradientType, 0)
        trackGradientAngle =
            typeArray.getInt(R.styleable.PopSeekBar_trackGradientAngle, 0)
        trackColors = intArrayOf(trackStartSolidColor, trackEndSolidColor)
        trackRadius = typeArray.getDimension(R.styleable.PopSeekBar_trackRadius, 0F)

        commonRadius = typeArray.getDimension(R.styleable.PopSeekBar_commonRadius, 0F)

        naturalProcess = typeArray.getBoolean(R.styleable.PopSeekBar_naturalProcess, false)

        trackPath = Path()
        trackPaint = Paint().apply {
            style = Paint.Style.FILL
            if (trackSolidColor != 0){
                color = trackSolidColor
            }
        }

        progressPath = Path()
        progressPaint = Paint().apply {
            style = Paint.Style.FILL
            if (progressSolidColor != 0){color = progressSolidColor
            }
        }

        finalPath = Path()
        typeArray.recycle()
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        if (isInit) {
            isInit = false
            originWidth = width
            originHeight = height
        }
        trackPath.drawTrackPath(0F + paddingStart, 0F + paddingTop, width.toFloat() - paddingEnd, height.toFloat() - paddingBottom, if (trackRadius == 0F) commonRadius else trackRadius)
        if (trackColors.any { it != 0 }) {
            trackPaint.setGradientShader(
                0F + paddingStart,
                0F + paddingTop,
                width.toFloat() - paddingEnd,
                height.toFloat() - paddingBottom,
                trackColors,
                trackGradientType,
                trackGradientAngle
            )
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawPath(trackPath, trackPaint)
        val progressWidth = width * (progress.toFloat() / max)
        if (!naturalProcess) {
            progressPath.drawProgressPath(
                0F + paddingStart,
                0F + paddingTop,
                width.toFloat() - paddingEnd,
                height.toFloat() - paddingBottom,
                if (progressRadius == 0F) commonRadius else progressRadius,
                0F + paddingStart + progressWidth
            )
        } else {
            progressPath.drawProgressPathNatural(
                0F + paddingStart,
                0F + paddingTop,
                width.toFloat() - paddingEnd,
                height.toFloat() - paddingBottom,
                if (progressRadius == 0F) commonRadius else progressRadius,
                0F + paddingStart + progressWidth
            )
        }
        finalPath.reset()
        finalPath.op(progressPath, trackPath, Path.Op.INTERSECT)
        if (progressColors.any { it != 0 }) {
            progressPaint.setGradientShader(
                0F + paddingStart,
                0F + paddingTop,
                progressWidth - paddingEnd,
                height.toFloat() - paddingBottom,
                progressColors,
                progressGradientType,
                progressGradientAngle
            )
        }
        canvas.drawPath(finalPath, progressPaint)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!this.isEnabled) return false
        val x = event.x
        val progressValue = ((x / width) * max).toInt()
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (canResponseTouch){
                    setProgress(progressValue,notifyListener = false,animator = true)
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (progressValue > max || progressValue < 0) {
                    val widthAddition = (if (progressValue < 0) abs(progressValue - 100) else progressValue) / 8
                    layoutParams.width = widthAddition + originWidth
                    layoutParams.height = ( (originWidth * originHeight) / (widthAddition + originWidth))
                    requestLayout()
                } else {
                    if (layoutParams.width > originWidth){
                        val widthAddition = progressValue / 8
                        layoutParams.width = width - widthAddition
                        layoutParams.height = ( (originWidth * originHeight) / (width - widthAddition))
                        requestLayout()
                    }
                }
                setProgress(max(min(max, progressValue), 0), true)
            }

            MotionEvent.ACTION_UP -> {
                if (layoutParams.width >= originWidth){
                    val widthHolder = PropertyValuesHolder.ofInt("width",width, originWidth)
                    val heightHolder = PropertyValuesHolder.ofInt("height",height, originHeight)
                    ValueAnimator.ofPropertyValuesHolder(widthHolder, heightHolder).apply {
                        interpolator = OvershootInterpolator()
                        addUpdateListener {
                            layoutParams.width = it.getAnimatedValue("width") as Int
                            layoutParams.height = it.getAnimatedValue("height") as Int
                            requestLayout()
                        }
                    }.start()
                }
                onProgressChangeListener?.invoke(progressValue)
            }
        }
        invalidate()
        return true
    }

    /**
     * update Progress value
     * [notifyListener] whether notify listener
     * [animator] whether use animator
     */
    fun setProgress(progress: Int, notifyListener: Boolean, animator: Boolean = false) {
        val aimProgress = max(0, min(max, progress))
        val curProgress = this.progress
        if (aimProgress != curProgress){
            if (animator) {
                ValueAnimator.ofInt(curProgress, aimProgress).apply {
                    interpolator = AccelerateDecelerateInterpolator()
                    addUpdateListener {
                        this@PopSeekBar.progress = it.animatedValue as Int
                        invalidate()
                    }
                    duration = abs(aimProgress - curProgress) * 5L
                }.start()
            } else {
                this.progress = aimProgress
                invalidate()
            }
        }
        if (notifyListener) {
            onProgressChangeListener?.invoke(this.progress)
        }
    }
    
}
