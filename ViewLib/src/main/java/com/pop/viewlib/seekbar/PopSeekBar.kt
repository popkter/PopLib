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
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.core.animation.doOnEnd
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

    private var mProgress: Int = 0
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

    private var mSeekbarChangeListener: OnPopSeekBarChangeListener? = null

    /**
     * whether use natural process modifier
     */
    var naturalProcess = true

    /**
     * whether can response touch down event
     * not suggest to modify it
     */
    var canResponseTouch = false

    /**
     * deformation dimension per unit stretch of the view
     */
    var stretchStep = 125

    /**
     * maximum distance to stretch the view
     */
    var maxStretchDistance = 50

    var overStretchEnable = true

    var onUserInput = false

    var max: Int = 100

    var min: Int = 0

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
            isAntiAlias = true
            if (trackSolidColor != 0){
                color = trackSolidColor
            }
        }

        progressPath = Path()
        progressPaint = Paint().apply {
            style = Paint.Style.FILL
            isAntiAlias = true
            if (progressSolidColor != 0) {
                color = progressSolidColor
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
        trackPath.drawTrackPath(
            0F,
            0F,
            width.toFloat(),
            height.toFloat(),
            if (trackRadius == 0F) commonRadius else trackRadius
        )
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
//        super.onDraw(canvas)
        canvas.drawPath(trackPath, trackPaint)

        if (isHorizontal) {
            val progressWidth = width * (mProgress.toFloat() / max)
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
            val progressHeight = height * (mProgress.toFloat() / max)
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
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                onUserInput = true
                onStartTrackingTouch(event)
            }

            MotionEvent.ACTION_MOVE -> {
                onMoveTrackingTouch(event)
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                onUserInput = false
                onStopTrackingTouch(event)
            }
        }
        invalidate()
        return true
    }

    private fun onStartTrackingTouch(event: MotionEvent) {
        mSeekbarChangeListener?.onStartTrackingTouch(this, mProgress)
        touchDownPoint.set(event.rawX, event.rawY)
        touchDownProgress = if (isHorizontal) {
            if (canResponseTouch) {
                val progressValue = ((x / width) * max).toInt()
                setProgressInternal(progressValue, true)
                progressValue
            } else {
                mProgress
            }
        } else {
            if (canResponseTouch) {
                val progressValue = ((1 - y / height) * max).toInt()
                progressValue
            } else {
                mProgress
            }
        }
    }

    private fun onMoveTrackingTouch(event: MotionEvent): Boolean {
        val progressValue = touchDownProgress + if (isHorizontal) {
            ((event.rawX - touchDownPoint.x) / width * max).toInt()
        } else {
            ((touchDownPoint.y - event.rawY) / height * max).toInt()
        }

        if (progressValue !in min..max) {
            if (overStretchEnable) {
                val addition = min(maxStretchDistance, ((if (progressValue > max) progressValue - max else abs(progressValue)) / max.toFloat() * stretchStep).toInt())
                if (addition <= maxStretchDistance) {
                    if (isHorizontal) { layoutParams.width = addition + originWidth
                        layoutParams.height = ((originWidth * originHeight) / (addition + originWidth))
                    } else {
                        layoutParams.height = addition + originHeight
                        layoutParams.width = ((originWidth * originHeight) / (addition + originHeight))
                    }
                    requestLayout()
                }
            }
        }
        setProgressInternal(progressValue, true)

        return true
    }

    private fun onStopTrackingTouch(event: MotionEvent) {
        if (layoutParams.width >= originWidth || layoutParams.height >= originHeight) {
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
                doOnEnd {
                    mSeekbarChangeListener?.onStopTrackingTouch(this@PopSeekBar, mProgress)
                }
            }.start()
        }
    }

    /**
     * update Progress value
     * [animator] whether use animator
     */
    fun setProgress(progress: Int, animate: Boolean = false) {
        setProgressInternal(progress, false, animate)
    }

    fun getProgress(): Int = mProgress

    private fun setProgressInternal(progress: Int, fromUser: Boolean, animate: Boolean = false) {
        val temp = max(min(max, progress), min)
        if (mProgress != temp) {
            if (animate) {
                ValueAnimator.ofInt(mProgress, temp).apply {
                    interpolator = AccelerateDecelerateInterpolator()
                    addUpdateListener { animation ->
                        mProgress = animation.animatedValue as Int
                        mSeekbarChangeListener?.onProgressChanged(this@PopSeekBar, mProgress,fromUser)
                        invalidate()
                    }
                    duration = abs(mProgress - temp) * 5L
                }.start()
            } else {
                mProgress = temp
                mSeekbarChangeListener?.onProgressChanged(this@PopSeekBar, mProgress,fromUser)
                invalidate()
            }
        }
    }

    fun setOnSeekBarChangeListener(listener: OnPopSeekBarChangeListener?) {
        mSeekbarChangeListener = listener
    }


    interface OnPopSeekBarChangeListener {
        fun onStartTrackingTouch(seekBar: PopSeekBar, mProgress: Int)
        fun onStopTrackingTouch(seekBar: PopSeekBar, mProgress: Int)
        fun onProgressChanged(seekBar: PopSeekBar, mProgress: Int, fromUser: Boolean)
    }
}

