package com.findly.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.findly.app.ui.navigation.FindlyApp
import com.findly.app.ui.navigation.LaunchTarget
import com.findly.app.ui.theme.FindlyTheme

class MainActivity : ComponentActivity() {

    private var launchTarget by mutableStateOf<LaunchTarget?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) launchTarget = targetFrom(intent)
        val container = (application as FindlyApplication).container
        setContent {
            val settings by container.settings.collectAsStateWithLifecycle()
            FindlyTheme(mode = settings.themeMode) {
                FindlyApp(launchTarget = launchTarget, onLaunchTargetHandled = { launchTarget = null })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        targetFrom(intent)?.let { launchTarget = it }
    }

    private fun targetFrom(intent: Intent?): LaunchTarget? {
        intent ?: return null
        intent.getStringExtra(EXTRA_PRODUCT_ID)?.let { return LaunchTarget.Product(it) }
        if (intent.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val text = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return null
            Regex("https://\\S+").find(text)?.value?.let { return LaunchTarget.SharedLink(it) }
        }
        return null
    }

    companion object {
        const val EXTRA_PRODUCT_ID = "com.findly.app.PRODUCT_ID"
    }
}
