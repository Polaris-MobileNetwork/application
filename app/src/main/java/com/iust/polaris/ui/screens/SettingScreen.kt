package com.iust.polaris.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.iust.polaris.ui.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val themePreference by viewModel.themePreference.collectAsState()
    val collectionInterval by viewModel.collectionInterval.collectAsState()
    val autoSyncEnabled by viewModel.autoSyncEnabled.collectAsState()
    val syncInterval by viewModel.syncInterval.collectAsState()

    Column(
        modifier = modifier
            .padding(16.dp)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()) // Make screen scrollable for more settings
    ) {
        // --- Theme Settings ---
        Text("Theme", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
        ThemeSettingItem(
            label = "System Default",
            isSelected = themePreference == "System",
            onClick = { viewModel.updateTheme("System") }
        )
        ThemeSettingItem(
            label = "Light",
            isSelected = themePreference == "Light",
            onClick = { viewModel.updateTheme("Light") }
        )
        ThemeSettingItem(
            label = "Dark",
            isSelected = themePreference == "Dark",
            onClick = { viewModel.updateTheme("Dark") }
        )

        Divider(modifier = Modifier.padding(vertical = 24.dp))

        // --- Data Collection Settings ---
        Text("Data Collection", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Collection Interval: $collectionInterval seconds")
        Slider(
            value = collectionInterval.toFloat(),
            onValueChange = { newValue -> viewModel.updateCollectionInterval(newValue.toInt()) },
            valueRange = 5f..60f, // From 5 seconds to 60 seconds
            steps = 10 // (60-5)/5 - 1 = 10 steps for 5s increments
        )

        Divider(modifier = Modifier.padding(vertical = 24.dp))

        // --- Sync Settings (New Section) ---
        Text("Data Sync", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        AutoSyncSettingItem(
            label = "Auto-sync",
            isChecked = autoSyncEnabled,
            onCheckedChange = { viewModel.updateAutoSync(it) }
        )

        // The Sync Interval slider is only visible if auto-sync is enabled
        AnimatedVisibility(
            visible = autoSyncEnabled,
            enter = fadeIn(animationSpec = tween(durationMillis = 300)),
            exit = fadeOut(animationSpec = tween(durationMillis = 300))
        ) {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                Text("Sync Interval: $syncInterval minutes")
                Slider(
                    value = syncInterval.toFloat(),
                    onValueChange = { newValue -> viewModel.updateSyncInterval(newValue.toInt()) },
                    valueRange = 15f..180f, // From 15 minutes to 3 hours
                    steps = 10 // (180-15)/15 - 1 = 11 steps for 15min increments
                )
            }
        }
    }
}

@Composable
fun ThemeSettingItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick
        )
        Text(text = label, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
fun AutoSyncSettingItem(
    label: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = { onCheckedChange(!isChecked) })
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange
        )
    }
}
