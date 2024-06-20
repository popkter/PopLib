package com.pop.viewlib.seekbar

import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import com.pop.demopanel.view.drawHorizontalProgressPath
import com.pop.demopanel.view.drawHorizontalProgressPathNatural
import com.pop.demopanel.view.drawTrackPath
import com.pop.demopanel.view.drawVerticalProgressPath
import com.pop.demopanel.view.drawVerticalProgressPathNatural
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

    private var progress = 0
    private var finalValue = 0

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

    private var isHorizontal = true

    private var isInit = true

    private val touchDownPoint = PointF(0F, 0F)

    private var touchDownProgress = 0

    /**
     * whether use natural process modifier
     */
    var naturalProcess = true

    /**
     * progress change listener
     */
    var onProgressChangeListener: ((progress: Int, isFinal: Boolean) -> Unit)? = null

    /**
     * whether can response touch down event
     * not suggest to modify it
     */
    var canResponseTouch = false

    /**
     * max value of progress
     */
    var max = 100

    /**
     * deformation dimension per unit stretch of the view
     */
    var stretchStep = 125

    /**
     * maximum distance to stretch the view
     */
    var maxStretchDistance = 50

    var overStretchEnable = true

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

        naturalProcess = typeArray.getBoolean(R.styleable.PopSeekBar_naturalProcess, true)

        isHorizontal = typeArray.getInt(R.styleable.PopSeekBar_orientation, 0) == 0

        overStretchEnable = typeArray.getBoolean(R.styleable.PopSeekBar_overStretchEnable, true)

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

    private fun notifyFinalProgress() {
        if (finalValue != progress) {
            finalValue = progress
            onProgressChangeListener?.invoke(progress, true)
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        if (isInit) {
            isInit = false
            originWidth = width
            originHeight = height
        }
        trackPath.drawTrackPath(0F, 0F, width.toFloat(), height.toFloat(), if (trackRadius == 0F) commonRadius else trackRadius)
        if (trackColors.any { it != 0 }) {
            trackPaint.setGradientShader(
                0F,
                0F,
                width.toFloat(),
                height.toFloat(),
                trackColors,
                trackGradientType,
                trackGradientAngle
            )
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawPath(trackPath, trackPaint)

        if (isHorizontal) {
            val progressWidth = width * (progress.toFloat() / max)
            if (naturalProcess) {
                progressPath.drawHorizontalProgressPathNatural(
                    0F,
                    0F,
                    width.toFloat(),
                    height.toFloat(),
                    if (progressRadius == 0F) commonRadius else progressRadius,
                    0F + progressWidth
                )
            } else {
                progressPath.drawHorizontalProgressPath(
                    0F,
                    0F,
                    width.toFloat(),
                    height.toFloat(),
                    if (progressRadius == 0F) commonRadius else progressRadius,
                    0F + progressWidth
                )
            }
        } else {
            val progressHeight = height * (progress.toFloat() / max)
            if (naturalProcess) {
                progressPath.drawVerticalProgressPathNatural(
                    0F,
                    0F,
                    width.toFloat(),
                    height.toFloat(),
                    if (progressRadius == 0F) commonRadius else progressRadius,
                    progressHeight
                )
            } else {
                progressPath.drawVerticalProgressPath(
                    0F,
                    0F,
                    width.toFloat(),
                    height.toFloat(),
                    if (progressRadius == 0F) commonRadius else progressRadius,
                    progressHeight
                )
            }
        }

        finalPath.reset()
        finalPath.op(progressPath, trackPath, Path.Op.INTERSECT)
        if (progressColors.any { it != 0 }) {
            progressPaint.setGradientShader(
                0F,
                0F,
                width.toFloat(),
                height.toFloat(),
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
        if (isHorizontal) {
            val x = event.x
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    touchDownProgress = if (canResponseTouch) {
                        val progressValue = ((x / width) * max).toInt()
                        setProgress(progressValue, notifyListener = false, animator = true)
                        progressValue
                    }else{
                        progress
                    }
                    touchDownPoint.set(event.x, event.y)
                }

                MotionEvent.ACTION_MOVE -> {
                    val progressDiffValue = ((event.x - touchDownPoint.x) / width * max).toInt()
                    val progressValue = touchDownProgress + progressDiffValue

                    if (overStretchEnable){
                        if (progressValue > max || progressValue < 0) {
                            val widthAddition = min(maxStretchDistance, ((if (progressValue > max) progressValue - max else abs(progressValue)) / max.toFloat() * stretchStep).toInt())

                            if (widthAddition > maxStretchDistance) return true
                            layoutParams.width = widthAddition + originWidth
                            layoutParams.height =
                                ((originWidth * originHeight) / (widthAddition + originWidth))
                            requestLayout()

                        }
                    }

                    setProgress(max(min(max, progressValue), 0), true)
                }

                MotionEvent.ACTION_UP -> {
                    if (layoutParams.width >= originWidth) {
                        val widthHolder = PropertyValuesHolder.ofInt("width", width, originWidth)
                        val heightHolder =
                            PropertyValuesHolder.ofInt("height", height, originHeight)
                        ValueAnimator.ofPropertyValuesHolder(widthHolder, heightHolder).apply {
                            interpolator = OvershootInterpolator()
                            addUpdateListener {
                                layoutParams.width = it.getAnimatedValue("width") as Int
                                layoutParams.height = it.getAnimatedValue("height") as Int
                                requestLayout()
                            }
                        }.start()
                    }
                    notifyFinalProgress()
                }
            }
        } else {
            val y = event.y
            when(event.action){
                MotionEvent.ACTION_DOWN -> {
                    touchDownProgress = if (canResponseTouch) {
                        val progressValue = ((1 - y / height) * max).toInt()
                        setProgress(progressValue, notifyListener = false, animator = true)
                        progressValue
                    }else{
                        progress
                    }
                    touchDownPoint.set(event.x, event.y)
                }

                MotionEvent.ACTION_MOVE -> {
                    val progressDiffValue = ((touchDownPoint.y - event.y ) / height * max).toInt()
                    val progressValue = touchDownProgress + progressDiffValue

                    if (overStretchEnable){
                        if (progressValue > max || progressValue < 0) {
                            val heightAddition = min(maxStretchDistance,  ((if (progressValue > max) progressValue - max else abs(progressValue)) / max.toFloat() * stretchStep).toInt())

                            layoutParams.height = heightAddition + originHeight
                            layoutParams.width = ((originWidth * originHeight) / (heightAddition + originHeight))
                            requestLayout()
                        }
                    }

                    setProgress(max(min(max, progressValue), 0), true)
                }

                MotionEvent.ACTION_UP -> {
                    if (layoutParams.height >= originHeight) {
                        val widthHolder = PropertyValuesHolder.ofInt("width", width, originWidth)
                        val heightHolder = PropertyValuesHolder.ofInt("height", height, originHeight)
                        ValueAnimator.ofPropertyValuesHolder(widthHolder, heightHolder).apply {
                            interpolator = OvershootInterpolator()
                            addUpdateListener {
                                layoutParams.width = it.getAnimatedValue("width") as Int
                                layoutParams.height = it.getAnimatedValue("height") as Int
                                requestLayout()
                            }
                        }.start()
                    }
                    notifyFinalProgress()
                }
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
        if (aimProgress != curProgress) {
            if (animator) {
                ValueAnimator.ofInt(curProgress, aimProgress).apply {
                    interpolator = AccelerateDecelerateInterpolator()
                    addUpdateListener { animation ->
                        (animation.animatedValue as Int).let {
                            this@PopSeekBar.progress = it
                            if (notifyListener) {
                                onProgressChangeListener?.invoke(it, false)
                            }
                        }
                        invalidate()
                    }
                    duration = abs(aimProgress - curProgress) * 5L
                }.start()
            } else {
                this.progress = aimProgress
                invalidate()
                if (notifyListener) {
                    onProgressChangeListener?.invoke(this.progress, false)
                }
            }
        }
    }
    
}
