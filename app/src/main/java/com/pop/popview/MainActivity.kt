package com.pop.popview

import android.os.Build
import android.os.Bundle
import android.view.animation.OvershootInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.pop.popview.databinding.PopNotification2Binding
import com.pop.popview.databinding.PopNotification3Binding
import com.pop.popview.databinding.PopNotificationBinding
import com.pop.popview.ui.theme.PopViewTheme
import com.pop.viewlib.dynamic_dialog.QuickFloatViewManager
import com.pop.viewlib.dynamic_dialog.SpiritViewManager
import java.util.UUID

@RequiresApi(Build.VERSION_CODES.O)
class MainActivity : ComponentActivity() {

    private val spiritViewManager: SpiritViewManager by lazy { SpiritViewManager(this@MainActivity) }
    private val quickFloatViewManager by lazy { QuickFloatViewManager() }

    private val notificationBinding by lazy {
        PopNotificationBinding.inflate(layoutInflater).apply {
            title.text = "这是一个通知"
            content.text = "这是一个小的通知"
            closeButton.setOnClickListener {
                quickFloatViewManager.pop(notificationBinding3.root)
            }
        }
    }

    private val notificationBinding2 by lazy {
        PopNotification2Binding.inflate(layoutInflater).apply {
            title.text = "这是一个通知"
            content.text = "这是一个大的通知"
            closeButton.setOnClickListener {
                quickFloatViewManager.dismiss()
            }
        }
    }

    private val notificationBinding3 by lazy {
        PopNotification3Binding.inflate(layoutInflater).apply {
            title.text = "这是一个通知"
            content.text = UUID.randomUUID().toString()
            closeButton.setOnClickListener {
                quickFloatViewManager.pop(notificationBinding2.root)
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        spiritViewManager.init()
        ViewTools.instance.checkPopWindowPermission(this@MainActivity) {
            spiritViewManager.init()
//            popViewManager.initComponent()
            quickFloatViewManager.initComponent(this)
        }

        enableEdgeToEdge()
        setContent {
            PopViewTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column {
                        Greeting(
                            name = "Android",
                            modifier = Modifier.padding(innerPadding)
                        ) {
                            quickFloatViewManager.pop(notificationBinding.root)
//                            spiritViewManager.show(notificationBinding.root)
                        }

                        Greeting(
                            name = "Android",
                            modifier = Modifier.padding(innerPadding)
                        ) {
                            spiritViewManager.showView(
                                notificationBinding.root,
                                2000,
                                OvershootInterpolator(0.5F)
                            )
                        }

                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier
    ) {
        Text(text = "Hello $name!")
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    PopViewTheme {
        Greeting("Android") {}
    }
}