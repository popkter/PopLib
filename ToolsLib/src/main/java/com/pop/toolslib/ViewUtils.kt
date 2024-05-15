package com.pop.toolslib

import android.app.Activity
import android.content.Intent
import android.content.res.Resources
import android.net.Uri
import android.provider.Settings

import android.util.TypedValue
import android.widget.Toast

class ViewTools {

    companion object {
        val instance by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { ViewTools() }
    }

    fun dp2px(dp: Float): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, Resources.getSystem().displayMetrics)
    }

    /**
     * 检查悬浮窗权限
     * Check whether the suspension window permission is enabled
     */
    fun checkPopWindowPermission(context: Activity, block: () -> Unit) {
        if (Settings.canDrawOverlays(context)) {
            block()
        } else {
            Toast.makeText(context, "请开启悬浮窗权限", Toast.LENGTH_SHORT).show()
            context.startActivityForResult(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                data = Uri.parse("package:${context.packageName}")
            }, 1001)
        }
    }

}