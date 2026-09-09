package com.luis.alhendinfc

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.luis.alhendinfc.data.local.AlhendinDatabase
import com.luis.alhendinfc.dev.DevSeedData
import com.luis.alhendinfc.navigation.AlhendinNavGraph
import com.luis.alhendinfc.ui.theme.AlhendinFCTheme
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val keepSystemSplash = AtomicBoolean(true)
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { keepSystemSplash.get() }
        splashScreen.setOnExitAnimationListener { provider ->
            provider.remove()
        }

        super.onCreate(savedInstanceState)
        // Tablet en banquillo: la pantalla no se apaga sola durante el uso.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()

        setContent {
            AlhendinFCTheme {
                var showBrandedSplash by remember { mutableStateOf(true) }
                var appReady by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    // Precalienta Room en IO mientras se ve el splash; monta la UI al final.
                    withContext(Dispatchers.IO) {
                        AlhendinDatabase.getInstance(applicationContext)
                        DevSeedData.runIfNeeded(applicationContext)
                    }
                    withFrameNanos { }
                    keepSystemSplash.set(false)
                    delay(900)
                    appReady = true
                    showBrandedSplash = false
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    if (appReady) {
                        val navController = rememberNavController()
                        AlhendinNavGraph(navController = navController)
                    }

                    AnimatedVisibility(
                        visible = showBrandedSplash,
                        exit = fadeOut()
                    ) {
                        SplashCargaScreen()
                    }
                }
            }
        }
    }
}

@Composable
private fun SplashCargaScreen() {
    Image(
        painter = painterResource(R.drawable.splash_carga),
        contentDescription = "Alhendin CF",
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop
    )
}
