package com.miguelcaldas.mcsmsforwardermultichannel.ui.channels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelsScreen(onOpenChannel: (ChannelType) -> Unit, onOpenFilters: () -> Unit, viewModel: ChannelsViewModel = viewModel()) {
    val channels by viewModel.channels.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Channels") },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            channels.forEach { summary ->
                ChannelCard(
                    summary = summary,
                    onClick = {
                        onOpenChannel(summary.type)
                    },
                    onToggle = { enabled ->
                        viewModel.setEnabled(summary.type, enabled)
                    },
                )
            }

            Spacer(Modifier.height(4.dp))

            Card(onClick = onOpenFilters) {
                Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Senders, rules & template", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Choose which senders are allowed, the match rules, and the forwarding template. Applies to every channel.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChannelCard(summary: ChannelSummary, onClick: () -> Unit, onToggle: (Boolean) -> Unit) {
    Card(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painterResource(summary.type.iconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(summary.type.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    summary.status,
                    style = MaterialTheme.typography.bodyMedium,
                    color = toneColor(summary.tone),
                )
            }
            Spacer(Modifier.width(16.dp))
            Switch(
                checked = summary.enabled,
                onCheckedChange = { enabled ->
                    onToggle(enabled)
                },
            )
        }
    }
}

@Composable
private fun toneColor(tone: ChannelTone): Color {
    return when (tone) {
        ChannelTone.READY -> MaterialTheme.colorScheme.primary
        ChannelTone.INCOMPLETE -> MaterialTheme.colorScheme.error
        ChannelTone.DISABLED -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}
