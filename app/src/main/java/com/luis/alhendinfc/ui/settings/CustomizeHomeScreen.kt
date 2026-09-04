package com.luis.alhendinfc.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.luis.alhendinfc.domain.model.HomeLayoutConfig
import com.luis.alhendinfc.domain.model.HomeModule
import com.luis.alhendinfc.domain.model.HomeModulePreference
import com.luis.alhendinfc.ui.theme.GreenAccent
import com.luis.alhendinfc.ui.theme.GreenMint

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomizeHomeScreen(
    config: HomeLayoutConfig,
    onToggle: (HomeModule, Boolean) -> Unit,
    onMoveUp: (HomeModule) -> Unit,
    onMoveDown: (HomeModule) -> Unit,
    onReset: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Personalizar inicio", fontWeight = FontWeight.Bold)
                        Text(
                            "Atajos del Home",
                            style = MaterialTheme.typography.labelMedium,
                            color = GreenMint
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    TextButton(onClick = onReset) {
                        Text("Restablecer", color = GreenMint)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0E2414),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF0A2410), MaterialTheme.colorScheme.background)
                    )
                )
                .padding(padding)
                .padding(20.dp)
        ) {
            Text(
                "Elige qué módulos aparecen en el inicio y en qué orden. " +
                    "«En vivo» solo se muestra si hay un partido en curso.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                itemsIndexed(config.modules, key = { _, item -> item.module.id }) { index, item ->
                    CustomizeHomeRow(
                        preference = item,
                        canMoveUp = index > 0,
                        canMoveDown = index < config.modules.lastIndex,
                        onToggle = { onToggle(item.module, it) },
                        onMoveUp = { onMoveUp(item.module) },
                        onMoveDown = { onMoveDown(item.module) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomizeHomeRow(
    preference: HomeModulePreference,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onToggle: (Boolean) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    val module = preference.module
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2A1E)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    module.title,
                    fontWeight = FontWeight.Bold,
                    color = if (preference.enabled) Color.White else Color.White.copy(alpha = 0.45f)
                )
                Text(
                    module.description,
                    style = MaterialTheme.typography.labelSmall,
                    color = GreenMint.copy(alpha = if (preference.enabled) 1f else 0.5f)
                )
            }
            IconButton(onClick = onMoveUp, enabled = canMoveUp) {
                Icon(
                    Icons.Default.KeyboardArrowUp,
                    contentDescription = "Subir",
                    tint = if (canMoveUp) GreenAccent else Color.White.copy(alpha = 0.25f)
                )
            }
            IconButton(onClick = onMoveDown, enabled = canMoveDown) {
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = "Bajar",
                    tint = if (canMoveDown) GreenAccent else Color.White.copy(alpha = 0.25f)
                )
            }
            Switch(checked = preference.enabled, onCheckedChange = onToggle)
        }
    }
}
