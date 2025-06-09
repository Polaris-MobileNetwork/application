package com.iust.polaris.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.iust.polaris.R
import com.iust.polaris.data.local.NetworkMetric
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NetworkMetricItem(metric: NetworkMetric) {
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }
    val itemTextStyle = MaterialTheme.typography.bodySmall
    val itemTextColor = MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ID: ${metric.id}",
                    style = itemTextStyle,
                    color = itemTextColor
                )
                // --- NEW: Sync Status Icon ---
                Icon(
                    painter = painterResource(if (metric.isUploaded) R.drawable.ic_cloude_done else R.drawable.ic_cloude_off),
                    contentDescription = if (metric.isUploaded) "Synced" else "Not Synced",
                    tint = if (metric.isUploaded) MaterialTheme.colorScheme.primary else itemTextColor.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
            // Display timestamp on a new line for better alignment with the icon
            Text(
                text = sdf.format(Date(metric.timestamp)),
                style = itemTextStyle,
                color = itemTextColor
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "${metric.networkType} | Signal: ${metric.signalStrength.takeIf { it != -999 } ?: "N/A"} dBm",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))

            // Location
            if (metric.latitude != null && metric.longitude != null) {
                Text(
                    text = "Loc: ${String.format("%.4f", metric.latitude)}, ${String.format("%.4f", metric.longitude)}",
                    style = itemTextStyle, color = itemTextColor
                )
            } else {
                Text("Loc: N/A", style = itemTextStyle, color = itemTextColor)
            }

            // Cell Identifiers
            metric.plmnId?.let { Text("PLMN: $it", style = itemTextStyle, color = itemTextColor) }
            metric.cellId?.let { Text("Cell ID: $it", style = itemTextStyle, color = itemTextColor) }
            metric.tac?.let { Text("TAC: $it", style = itemTextStyle, color = itemTextColor) }
            metric.lac?.let { Text("LAC: $it", style = itemTextStyle, color = itemTextColor) }

            // Frequency Info
            metric.arfcn?.let { Text("ARFCN: $it", style = itemTextStyle, color = itemTextColor) }
            metric.frequencyBand?.let { Text("Band: $it", style = itemTextStyle, color = itemTextColor) }

            // Signal Quality Metrics
            metric.rsrp?.let { Text("RSRP: $it dBm", style = itemTextStyle, color = itemTextColor) }
            metric.rsrq?.let { Text("RSRQ: $it dB", style = itemTextStyle, color = itemTextColor) }
            metric.rscp?.let { Text("RSCP: $it dBm (approx)", style = itemTextStyle, color = itemTextColor) }
            metric.rxlev?.let { Text("RxLev: $it dBm (approx)", style = itemTextStyle, color = itemTextColor) }
        }
    }
}
