package com.pop.viewlib.colorpickerview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ComposeShader
import android.graphics.Paint
import android.graphics.Point
import android.graphics.PorterDuff
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.SweepGradient
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import com.pop.viewlib.R
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.pow
import kotlin.math.sqrt

class ColorPickerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        private const val TAG = "ColorPickerView"
        private const val BASE_RADIO = 1.0f
    }

    private var mBigCircle: Int = 0
    private var mRudeRadius: Int = 0
    private var mCenterColor: Int = Color.WHITE
    private var isRing: Boolean = false
    private var mRingCircle: Int = 0
    private var mRudeStrokeWidth: Int = 5

    private var mBitmapBack: Bitmap
    private val mPaint = Paint()
    private var mBlankPaint: Paint? = null
    private val mCenterPaint = Paint()
    private var mCenterPoint: Point
    private var mRockPosition: Point

    private var mListener: OnColorChangedListener? = null

    private var length: Double = 0.0
    private var mHue: Double = 0.0
    private var mBrightness: Double = 1.0
    private var mSaturation: Double = 1.0

    private val mHSV = FloatArray(3)

    init {
        attrs?.let {
            val types = context.obtainStyledAttributes(it, R.styleable.ColorPickerView)
            try {
                mBigCircle =
                    types.getDimensionPixelOffset(R.styleable.ColorPickerView_circle_radius, 100)
                mRudeRadius =
                    types.getDimensionPixelOffset(R.styleable.ColorPickerView_center_radius, 10)
                mCenterColor = types.getColor(R.styleable.ColorPickerView_center_color, Color.WHITE)
                isRing = types.getBoolean(R.styleable.ColorPickerView_is_ring, false)
                mRingCircle =
                    types.getDimensionPixelOffset(R.styleable.ColorPickerView_ring_radius, 30)
            } finally {
                types.recycle()
            }
        }

        mCenterPoint = Point(mBigCircle, mBigCircle)
        mRockPosition = Point(mCenterPoint)

        mPaint.isAntiAlias = true
        mBitmapBack = createColorWheelBitmap(mBigCircle * 2, mBigCircle * 2)

        mCenterPaint.color = mCenterColor
        mCenterPaint.strokeWidth = mRudeStrokeWidth.toFloat()
        mCenterPaint.style = Paint.Style.STROKE
    }

    private fun createColorWheelBitmap(width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val colorCount = 12
        val colorAngleStep = 360 / colorCount
        val colors = IntArray(colorCount + 1)
        val hsv = floatArrayOf(0f, 1f, 1f)
        for (i in colors.indices) {
            hsv[0] = (360 - (i * colorAngleStep) % 360).toFloat()
            colors[i] = Color.HSVToColor(hsv)
        }
        colors[colorCount] = colors[0]

        val sweepGradient = SweepGradient(width / 2f, height / 2f, colors, null)
        val radialGradient = RadialGradient(
            width / 2f,
            height / 2f,
            mBigCircle.toFloat(),
            0xFFFFFFFF.toInt(),
            0x00FFFFFF,
            Shader.TileMode.CLAMP
        )
        val composeShader = ComposeShader(sweepGradient, radialGradient, PorterDuff.Mode.SRC_OVER)

        mPaint.shader = composeShader
        val canvas = Canvas(bitmap)
        canvas.drawCircle(width / 2f, height / 2f, mBigCircle.toFloat(), mPaint)
        if (isRing) {
            mBlankPaint = Paint().apply {
                isAntiAlias = true
                color = Color.WHITE
            }
            canvas.drawCircle(
                width / 2f,
                height / 2f,
                (mBigCircle - mRingCircle).toFloat(),
                mBlankPaint!!
            )
        }
        return bitmap
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawBitmap(mBitmapBack, 0f, 0f, mPaint)
        canvas.drawCircle(
            mRockPosition.x.toFloat(),
            mRockPosition.y.toFloat(),
            mRudeRadius.toFloat(),
            mCenterPaint
        )
    }

    fun setOnColorChangedListener(listener: OnColorChangedListener) {
        this.mListener = listener
    }

    interface OnColorChangedListener {
        fun onColorChange(hsb: FloatArray)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        Log.e(TAG, "onTouchEvent: ${event.action}")
        val cX = mCenterPoint.x.toFloat()
        val cY = mCenterPoint.y.toFloat()
        val pX = event.x
        val pY = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP, MotionEvent.ACTION_MOVE -> {
                length =
                    getLength(event.x, event.y, mCenterPoint.x.toFloat(), mCenterPoint.y.toFloat())
                if (isRing) {
                    handleRingTouchEvent(event)
                } else {
                    handleCircleTouchEvent(event)
                }
                mHue = getHue(cX, cY, pX, pY).also {
                    if (it < 0) mHue += 360.0
                }
                val deltaX = Math.abs(cX - pX).toDouble()
                val deltaY = (cY - pY).toDouble()
                mSaturation = Math.sqrt(deltaX * deltaX + deltaY * deltaY) / mBigCircle * BASE_RADIO
                mSaturation = mSaturation.coerceIn(0.0, 1.0)
            }
        }

        mHSV[0] = mHue.toFloat()
        mHSV[1] = mSaturation.toFloat()
        mHSV[2] = mBrightness.toFloat()

        mListener?.onColorChange(mHSV)
        invalidate()
        return true
    }

    private fun handleRingTouchEvent(event: MotionEvent) {
        if (length <= mBigCircle - mRudeRadius && length >= mBigCircle - mRingCircle + mRudeRadius) {
            mRockPosition.set(event.x.toInt(), event.y.toInt())
        } else if (length > mBigCircle - mRudeRadius) {
            mRockPosition = getBorderPoint(
                mCenterPoint,
                Point(event.x.toInt(), event.y.toInt()),
                mBigCircle - mRudeRadius - 5
            )
        } else if (length < mBigCircle - mRingCircle + mRudeRadius) {
            return
        }
    }

    private fun handleCircleTouchEvent(event: MotionEvent) {
        if (length <= mBigCircle - mRudeRadius) {
            mRockPosition.set(event.x.toInt(), event.y.toInt())
        } else {
            mRockPosition = getBorderPoint(
                mCenterPoint,
                Point(event.x.toInt(), event.y.toInt()),
                mBigCircle - mRudeRadius - 5
            )
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(mBigCircle * 2, mBigCircle * 2)
    }

    private fun getLength(x1: Float, y1: Float, x2: Float, y2: Float): Double {
        return sqrt((x1 - x2).toDouble().pow(2.0) + (y1 - y2).toDouble().pow(2.0))
    }

    private fun getBorderPoint(a: Point, b: Point, cutRadius: Int): Point {
        val radian = getRadian(a, b)
        return Point(
            a.x + (cutRadius * Math.cos(radian.toDouble())).toInt(),
            a.y + (cutRadius * Math.sin(radian.toDouble())).toInt()
        )
    }

    private fun getRadian(a: Point, b: Point): Float {
        val lenA = (b.x - a.x).toFloat()
        val lenB = (b.y - a.y).toFloat()
        val lenC = sqrt((lenA * lenA + lenB * lenB).toDouble()).toFloat()
        var ang = acos((lenA / lenC).toDouble()).toFloat()
        ang *= if (b.y < a.y) -1 else 1
        return ang
    }

    private fun getHue(centerX: Float, centerY: Float, rockX: Float, rockY: Float): Double {
        if (centerX == rockX && centerY == rockY) return 0.0
        if (centerX == rockX) return if (centerY > rockY) 90.0 else 270.0
        if (centerY == rockY) return if (centerX > rockX) 180.0 else 0.0

        val deltaA = abs(rockX - centerX)
        val deltaB = abs(rockY - centerY)
        val deltaC = getLength(centerX, centerY, rockX, rockY)

        return when {
            rockX > centerX && centerY > rockY -> asin(deltaB / deltaC) * 180 / Math.PI
            rockX < centerX && rockY < centerY -> asin(deltaA / deltaC) * 180 / Math.PI + 90
            rockX < centerX && rockY > centerY -> asin(deltaB / deltaC) * 180 / Math.PI + 180
            rockX > centerX && rockY > centerY -> asin(deltaA / deltaC) * 180 / Math.PI + 270
            else -> 0.0
        }
    }
}
