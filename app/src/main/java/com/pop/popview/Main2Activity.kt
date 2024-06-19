package com.pop.popview

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ReportFragment.Companion.reportFragment
import com.pop.popview.databinding.ActivityMain2Binding

class Main2Activity : AppCompatActivity() {


    companion object{
        const val TAG = "Main2Activity"
    }

    private val binding by lazy { ActivityMain2Binding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        binding.seekbar.apply {
            max = 8
            onProgressChangeListener = {
                Log.e(TAG, "onCreate: $it")
            }
        }

    }
}