package com.luis.alhendinfc

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.luis.alhendinfc.cloud.AlhendinCloud
import com.luis.alhendinfc.cloud.FirebaseAvailability
import com.luis.alhendinfc.cloud.auth.AuthErrorMapper
import com.luis.alhendinfc.cloud.auth.AuthSession
import com.luis.alhendinfc.data.local.AlhendinDatabase
import com.luis.alhendinfc.data.sync.UnavailableCloudException
import com.luis.alhendinfc.dev.DevSeedData
import com.luis.alhendinfc.navigation.AlhendinNavGraph
import com.luis.alhendinfc.ui.auth.LoginScreen
import com.luis.alhendinfc.ui.theme.AlhendinFCTheme
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()
        val cloud = AlhendinCloud.getInstance(applicationContext)
        cloud.start()

        setContent {
            AlhendinFCTheme {
                var showBrandedSplash by remember { mutableStateOf(true) }
                var appReady by remember { mutableStateOf(false) }
                val session by cloud.session.collectAsStateWithLifecycle()
                var loginBusy by remember { mutableStateOf(false) }
                var loginError by remember { mutableStateOf<String?>(null) }
                val scope = rememberCoroutineScope()

                LaunchedEffect(Unit) {
                    withContext(Dispatchers.IO) {
                        AlhendinDatabase.getInstance(applicationContext)
                        if (!FirebaseAvailability.isConfigured(applicationContext)) {
                            DevSeedData.runIfNeeded(applicationContext)
                        }
                    }
                    withFrameNanos { }
                    keepSystemSplash.set(false)
                    delay(900)
                    appReady = true
                    showBrandedSplash = false
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    if (appReady) {
                        when (val current = session) {
                            is AuthSession.Ready -> {
                                val navController = rememberNavController()
                                AlhendinNavGraph(navController = navController)
                            }
                            AuthSession.Checking -> {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    CircularProgressIndicator()
                                    Text(
                                        "Cargando…",
                                        color = Color.White,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(top = 16.dp)
                                    )
                                }
                            }
                            else -> {
                                val unavailable = when (current) {
                                    is AuthSession.Unavailable -> current.message
                                    else -> if (!FirebaseAvailability.isConfigured(applicationContext)) {
                                        FirebaseAvailability.NOT_CONFIGURED
                                    } else {
                                        null
                                    }
                                }
                                val error = when {
                                    current is AuthSession.NonMember ->
                                        "Esta cuenta no pertenece al espacio Alhendín CF."
                                    else -> loginError
                                }
                                LoginScreen(
                                    busy = loginBusy || current is AuthSession.Checking,
                                    errorMessage = error,
                                    unavailableMessage = unavailable,
                                    onSignIn = { email, password ->
                                        loginError = null
                                        loginBusy = true
                                        scope.launch {
                                            try {
                                                val resolved = withContext(Dispatchers.IO) {
                                                    cloud.signIn(email, password)
                                                }
                                                if (resolved is AuthSession.NonMember) {
                                                    loginError = "Esta cuenta no pertenece al espacio Alhendín CF."
                                                }
                                            } catch (e: UnavailableCloudException) {
                                                loginError = e.message ?: FirebaseAvailability.NOT_CONFIGURED
                                            } catch (e: Exception) {
                                                loginError = AuthErrorMapper.map(e)
                                            } finally {
                                                loginBusy = false
                                            }
                                        }
                                    }
                                )
                            }
                        }
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
