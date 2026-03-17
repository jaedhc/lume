package com.jaedhc.lume

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.jaedhc.lume.ui.screens.receiver.ShareReceiverScreen
import com.jaedhc.lume.ui.theme.LumeTheme

class ReceiverActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LumeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var uris by remember { mutableStateOf<List<Uri>>(emptyList()) }
                    val ctx = LocalContext.current
                    LaunchedEffect(Unit) {
                        handleShareIntent(intent)?.let { uris = it }
                            ?: run {
                                Toast.makeText(ctx, "No hay imágenes para procesar", Toast.LENGTH_SHORT).show()
                                finish()
                            }
                    }
                    // Maneja intents posteriores (singleTop)
                    DisposableEffect(Unit) {
                        val activity = this@ReceiverActivity
                        val callback = object : NewIntentListener {
                            override fun onNewIntent(newIntent: Intent) {
                                handleShareIntent(newIntent)?.let { uris = it }
                            }
                        }
                        activity.newIntentListener = callback
                        onDispose { activity.newIntentListener = null }
                    }

                    ShareReceiverScreen(
                        uris = uris,
                        onDone = { finish() },
                        onNavigate = { route ->
                            val intent = Intent(ctx, MainActivity::class.java).apply {
                                putExtra("START_ROUTE", route)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            }
                            ctx.startActivity(intent)
                            finish()
                        }
                    )
                }
            }
        }
    }

    // Soporte simple para onNewIntent desde Compose
    interface NewIntentListener { fun onNewIntent(newIntent: Intent) }
    var newIntentListener: NewIntentListener? = null
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.let { newIntentListener?.onNewIntent(it) }
    }

    private fun handleShareIntent(intent: Intent): List<Uri>? {
        val type = intent.type ?: return null
        if (!type.startsWith("image/")) return null

        return when (intent.action) {
            Intent.ACTION_SEND -> {
                intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)?.let { listOf(it) }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
            }
            else -> null
        }
    }
}