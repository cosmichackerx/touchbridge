package com.touchbridge.mobile.presentation.connect

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.touchbridge.mobile.R
import com.touchbridge.mobile.domain.model.DiscoveredDesktop
import com.touchbridge.mobile.presentation.common.DebugLogPanel

@Composable
fun ConnectScreen(
    onConnected: () -> Unit,
    modifier: Modifier = Modifier,
    autoHost: String? = null,
    autoPin: String? = null,
    viewModel: ConnectViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(autoHost, autoPin) {
        if (!autoHost.isNullOrBlank()) {
            viewModel.setManualHost(autoHost)
            if (!autoPin.isNullOrBlank()) viewModel.setPin(autoPin)
            viewModel.connect()
        }
    }

    if (uiState.navigateToTouchpad) {
        onConnected()
        viewModel.onNavigated()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.connect_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (uiState.isDiscovering) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.connect_discovering),
                    modifier = Modifier.padding(start = 12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.desktops.isEmpty() && !uiState.isDiscovering) {
            Text(
                text = stringResource(R.string.connect_no_devices),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(uiState.desktops, key = { it.host }) { desktop ->
                DesktopCard(
                    desktop = desktop,
                    onClick = { viewModel.selectDesktop(desktop) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = uiState.manualHost,
            onValueChange = viewModel::setManualHost,
            label = { Text(stringResource(R.string.connect_manual_hint)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = uiState.pin,
            onValueChange = viewModel::setPin,
            label = { Text(stringResource(R.string.connect_pin_hint)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        if (uiState.error != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = uiState.error!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = viewModel::connect,
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isConnecting
        ) {
            if (uiState.isConnecting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(stringResource(R.string.connect_button))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.debug_logs_title),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary
        )

        Spacer(modifier = Modifier.height(4.dp))

        DebugLogPanel()
    }
}

@Composable
private fun DesktopCard(
    desktop: DiscoveredDesktop,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.foundation.Canvas(modifier = Modifier.size(10.dp)) {
                drawCircle(color = androidx.compose.ui.graphics.Color(0xFF4ADE80))
            }
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(
                    text = desktop.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                val linkLabel = when (desktop.link.lowercase()) {
                    "usb" -> stringResource(R.string.connect_link_usb)
                    "lan" -> stringResource(R.string.connect_link_lan)
                    else -> null
                }
                Text(
                    text = if (linkLabel != null) "${desktop.host} · $linkLabel" else desktop.host,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}
