package com.pop.popview

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ReportFragment.Companion.reportFragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chad.library.adapter4.BaseQuickAdapter
import com.pop.popview.databinding.ActivityMain2Binding
import com.pop.popview.databinding.ItemBinding
import com.pop.popview.databinding.NotificationPanelBinding
import com.pop.toolslib.setOnSingleClickListener
import com.pop.viewlib.pulldown.PullDownPanelManager

@RequiresApi(Build.VERSION_CODES.S)
class Main2Activity : AppCompatActivity() {


    companion object {
        const val TAG = "Main2Activity"
    }

    private lateinit var dialog: PullDownPanelManager
    private val data = List(100) { "字符串 $it" }

    private val binding by lazy { ActivityMain2Binding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        supportActionBar?.hide()
        val contentBinding = NotificationPanelBinding.inflate(LayoutInflater.from(this))

        contentBinding.apply {
            rv.apply {
                adapter = object: BaseQuickAdapter<String,RecyclerView.ViewHolder>(data){
                    override fun onBindViewHolder(
                        holder: RecyclerView.ViewHolder,
                        position: Int,
                        item: String?
                    ) {
                        holder.itemView.findViewById<TextView>(R.id.tv).text = item.toString()
                    }

                    override fun onCreateViewHolder(
                        context: Context,
                        parent: ViewGroup,
                        viewType: Int
                    ): RecyclerView.ViewHolder {
                        return object : RecyclerView.ViewHolder(ItemBinding.inflate(layoutInflater).root){}
                    }

                }

                layoutManager = GridLayoutManager(this@Main2Activity,6)
            }
        }

        dialog = PullDownPanelManager.getInstance(this).init(contentBinding.root)
/*        dialog = NotificationDialog(this@Main2Activity).apply {
            init()
        }*/

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