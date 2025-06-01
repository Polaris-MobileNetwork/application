package com.iust.polaris.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import com.iust.polaris.R
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

@Composable
fun MetricsControls(
    statusText: String,
    isCollecting: Boolean,
    onToggleCollection: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = statusText,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Button(
            onClick = onToggleCollection,
            modifier = Modifier.fillMaxWidth(0.8f) // Use a fraction of the width
        ) {
            Icon(
                painter = painterResource(if (isCollecting) R.drawable.ic_action_stop else R.drawable.ic_action_start),
                contentDescription = if (isCollecting) "Stop Collection" else "Start Collection"
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = if (isCollecting) "Stop Collection" else "Start Collection")
        }
    }
}
