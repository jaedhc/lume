package com.jaedhc.lume

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.jaedhc.lume.ui.navigation.AppNavigation
import com.jaedhc.lume.ui.theme.LumeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LumeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val startRoute = intent.getStringExtra("START_ROUTE")
                    if (startRoute != null) {
                        AppNavigation(startDestination = startRoute)
                    } else {
                        AppNavigation()
                    }
                }
            }
        }
    }
}