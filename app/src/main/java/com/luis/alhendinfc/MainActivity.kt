package com.luis.alhendinfc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.luis.alhendinfc.navigation.AlhendinNavGraph
import com.luis.alhendinfc.ui.theme.AlhendinFCTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AlhendinFCTheme {
                val navController = rememberNavController()
                AlhendinNavGraph(navController = navController)
            }
        }
    }
}
