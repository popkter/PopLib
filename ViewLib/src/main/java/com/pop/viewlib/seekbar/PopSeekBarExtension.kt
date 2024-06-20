package com.pop.demopanel.view

import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.os.Build.VERSION_CODES.P
import android.util.Log

fun Paint.setGradientShader(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    colors: IntArray,
    type: Int,
    angle: Int
) {

    when (type) {
        0 -> {

            val linearGradient = when (angle) {

                45 -> {
                    LinearGradient(left, top, right, bottom, colors, floatArrayOf(0.0f, 1.0f), Shader.TileMode.CLAMP)
                }

                90 -> {
                    LinearGradient(left, top, left, bottom, colors, floatArrayOf(0.0f, 1.0f), Shader.TileMode.CLAMP)
                }

                135 -> {
                    LinearGradient(right, top, left, bottom, colors, floatArrayOf(0.0f, 1.0f), Shader.TileMode.CLAMP)
                }

                180 -> {
                    LinearGradient(right, bottom, left, bottom, colors, floatArrayOf(0.0f, 1.0f), Shader.TileMode.CLAMP)
                }

                225 -> {
                    LinearGradient(right, bottom, left, top, colors, floatArrayOf(0.0f, 1.0f), Shader.TileMode.CLAMP)
                }

                270 -> {
                    LinearGradient(left, bottom, left, top, colors, floatArrayOf(0.0f, 1.0f), Shader.TileMode.CLAMP)
                }

                315 -> {
                    LinearGradient(left, bottom, right, top, colors, floatArrayOf(0.0f, 1.0f), Shader.TileMode.CLAMP)
                }

                else -> {
                    LinearGradient(left, top, right, top, colors, floatArrayOf(0.0f, 1.0f), Shader.TileMode.CLAMP)
                }

            }

            this.setShader(linearGradient)

        }

        1 -> {

        }

        else -> {

        }
    }


}



fun Path.drawTrackPath(left: Float, top: Float, right: Float, bottom: Float, radius: Float) {
    reset()
    moveTo(left + radius, top)
    lineTo(right - radius, top)
    arcTo(right - radius * 2, top, right, top + radius * 2, 270F, 90F, false)
    lineTo(right, bottom - radius)
    arcTo(right - radius * 2, bottom - radius * 2, right, bottom, 0F, 90F, false)
    lineTo(left + radius, bottom)
    arcTo(left, bottom - radius * 2, left + radius * 2, bottom, 90F, 90F, false)
    lineTo(left, radius + top)
    arcTo(left, top, left + radius * 2, top + radius * 2, 180F, 90F, false)
    close()
}

fun Path.drawHorizontalProgressPathNatural(left: Float, top: Float, right: Float, bottom: Float, radius: Float,progress: Float) {
    reset()
    moveTo(left, top)
    when (progress) {
        in left + radius..left + radius * 2 -> {
            lineTo(left + radius, top)
            quadTo(progress, top, progress, top + progress - left - radius)
            lineTo(progress, bottom - progress + left + radius)
            quadTo(progress, bottom, progress - (progress - left - radius), bottom)
        }

        in left + radius * 2..right - 2 * radius -> {
            lineTo(progress - radius, top)
            arcTo(progress - radius * 2, top, progress, top + radius * 2, 270F, 90F, false)
            lineTo(progress, bottom - radius)
            arcTo(progress - radius * 2, bottom - radius * 2, progress, bottom, 0F, 90F, false)
        }

        in right - 2 * radius..right - radius -> {
            val r = right - progress - radius
            lineTo(progress - r,top)
            quadTo(progress,top,progress,top + r)
            lineTo(progress,bottom - r)
            quadTo(progress,bottom,progress - r,bottom)
        }

        else -> {
            lineTo(progress, top)
            lineTo(progress, bottom)
        }
    }

    lineTo(left, bottom)
    lineTo(left, top)
    close()
}

fun Path.drawVerticalProgressPathNatural(left: Float, top: Float, right: Float, bottom: Float, radius: Float, progress: Float) {
    reset()
    moveTo(left, bottom)

    when (progress) {

        in radius..2 * radius -> {
            lineTo(left, bottom - radius)
            quadTo(left, bottom - progress, left + progress - radius, bottom - progress)
            lineTo(right - progress + radius, bottom - progress)
            quadTo(right, bottom - progress, right, bottom - radius)
        }

        in radius * 2..bottom - radius * 2 -> {
            lineTo(left, bottom - progress + radius)
            arcTo(left, bottom - progress, left + radius * 2, bottom - progress + radius * 2, 180F, 90F, false)
            lineTo(right - radius, bottom - progress)
            arcTo(right - radius * 2, bottom - progress, right, bottom - progress + radius * 2, 270F, 90F, false)
        }

        in bottom - radius * 2..bottom - radius -> {
            val r = bottom - progress - radius
            lineTo(left, bottom - progress + r)
            quadTo(left, bottom - progress, left + r, bottom - progress)
            lineTo(right - r, bottom - progress)
            quadTo(right, bottom - progress, right, bottom - progress + r)
        }

        else -> {
            lineTo(left, bottom - progress)
            lineTo(right, bottom - progress)
        }

    }
    lineTo(right, bottom)
    lineTo(left, bottom)
    close()
}


fun Path.drawHorizontalProgressPath(left: Float, top: Float, right: Float, bottom: Float, radius: Float,progress: Float) {
    reset()
    moveTo(left, top)
    lineTo(progress - radius, top)
    arcTo(progress - radius * 2, top, progress, top + radius * 2, 270F, 90F, false)
    lineTo(progress, bottom - radius)
    arcTo(progress - radius * 2, bottom - radius * 2, progress, bottom, 0F, 90F, false)
    lineTo(left, bottom)
    lineTo(left, top)
    close()
}

fun Path.drawVerticalProgressPath(left: Float, top: Float, right: Float, bottom: Float, radius: Float, progress: Float) {
    reset()
    moveTo(left, bottom)
    lineTo(left, bottom - progress + radius)
    arcTo(left, bottom - progress, left + radius * 2, bottom - progress + radius * 2, 180F, 90F, false)
    lineTo(right - radius, bottom - progress)
    arcTo(right - radius * 2, bottom - progress, right, bottom - progress + radius * 2, 270F, 90F, false)
    lineTo(right, bottom)
    lineTo(left, bottom)
    close()
}


