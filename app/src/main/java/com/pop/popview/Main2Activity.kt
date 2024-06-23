package com.pop.popview

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ReportFragment.Companion.reportFragment
import com.pop.popview.databinding.ActivityMain2Binding
import com.pop.toolslib.setOnSingleClickListener

@RequiresApi(Build.VERSION_CODES.S)
class Main2Activity : AppCompatActivity() {


    companion object {
        const val TAG = "Main2Activity"
    }

    private lateinit var dialog: NotificationDialog

    private val binding by lazy { ActivityMain2Binding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        supportActionBar?.hide()

        dialog = NotificationDialog(this@Main2Activity).apply {
            init()
        }

        binding.pull.setOnClickListener {
            dialog.display()
        }
        binding.pick.setOnClickListener {
            dialog.dismiss()
        }
//        dialog.init()
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
       return event?.let { dialog.onTouchEvent(it) } ?: false
    }

//    override fun onTouchEvent(event: MotionEvent?): Boolean {
////        return if (/*.onTouchEvent(event)*/) super.onTouchEvent(event) else true
//        event?.let { dialog.display(it) }
//        return true
//    }
}