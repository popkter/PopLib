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
        supportActionBar?.hide()
        binding.seekbar.apply {
            max = 50
            stretchStep = 150
            maxStretchDistance =70
//            canResponseTouch = true
        }

        binding.seekbar1.apply {
//            canResponseTouch = true
        }

        binding.seekbar.onProgressChangeListener = { progress, isFinal ->
            binding.muteIv.isSelected = progress > 0
            binding.progressTv.text = progress.toString()
            if (isFinal){
                Log.e(TAG, "onCreate: final progress= $progress")
            }
        }

        binding.seekbar1.onProgressChangeListener = { progress, isFinal ->
            binding.progressTv1.text = progress.toString()
        }

    }
}