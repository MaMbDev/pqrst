package dam.pmdm.pqrst.presentation.ecg.monitor

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dam.pmdm.pqrst.R
import dam.pmdm.pqrst.presentation.component.EcgChartWithPeaks
import dam.pmdm.pqrst.presentation.component.PqrstTopBar
import dam.pmdm.pqrst.ui.theme.PqrstBurgundy

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcgMonitorScreen(
    consultationId: Long,
    onBack: () -> Unit,
    viewModel: EcgMonitorViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val peaks by viewModel.peaks.collectAsState()
    val signalBuffer by viewModel.signalBuffer.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var showDemoMenu by remember { mutableStateOf(false) }
    var showBtSheet by remember { mutableStateOf(false) }
    val btSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Bluetooth permission launcher
    val btPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
    } else {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        if (grants.values.all { it }) {
            viewModel.startBluetoothScan()
            showBtSheet = true
        } else {
            snackbarHostState.apply {
                // Permission denied — snackbar handled via LaunchedEffect below
            }
        }
    }

    // Show BT sheet when the ViewModel transitions to a BT state
    LaunchedEffect(uiState) {
        if (uiState is EcgMonitorUiState.BtDeviceList) showBtSheet = true
    }

    // Collect one-shot events
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is EcgMonitorEvent.ShowError ->
                    snackbarHostState.showSnackbar(context.getString(event.messageRes))
            }
        }
    }

    Scaffold(
        topBar = {
            PqrstTopBar(
                title = stringResource(R.string.ecg_monitor_title),
                role = null,
                onMenuClick = {},
                onBackClick = onBack,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {

            // ── Status row ─────────────────────────────────────────────────────
            StatusRow(uiState = uiState)

            // ── ECG chart area ─────────────────────────────────────────────────
            Surface(
                color = Color(0xFFFFF3F3),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth(),
            ) {
                EcgChartWithPeaks(
                    signalBuffer = signalBuffer,
                    peaks = peaks,
                    signalColor = signalColorFor(uiState),
                    peakColor = peakColorFor(uiState),
                    isPaused = (uiState as? EcgMonitorUiState.DemoRunning)?.isPaused == true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                )
            }

            // ── Color legend ───────────────────────────────────────────────────
            ColorLegend(uiState = uiState)

            // ── Action buttons ─────────────────────────────────────────────────
            ActionButtons(
                uiState = uiState,
                onConnectEsp32 = {
                    permissionLauncher.launch(btPermissions)
                },
                onDemoClick = { showDemoMenu = true },
                showDemoMenu = showDemoMenu,
                onDemoPatternSelected = { pattern ->
                    showDemoMenu = false
                    viewModel.startDemo(pattern)
                },
                onDemoMenuDismiss = { showDemoMenu = false },
                onPauseDemo = { viewModel.pauseDemo() },
                onResumeDemo = { viewModel.resumeDemo() },
                onStopDemo = { viewModel.stopDemo() },
                onDisconnectBt = { viewModel.disconnectBt() },
            )

            // ── Pattern description ────────────────────────────────────────────
            val demoState = uiState as? EcgMonitorUiState.DemoRunning
            if (demoState != null) {
                PatternDescriptionCard(pattern = demoState.pattern)
            }

            // ── Educational disclaimer ─────────────────────────────────────────
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = stringResource(R.string.ecg_disclaimer),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Start,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    // ── Bluetooth device picker bottom sheet ───────────────────────────────────
    if (showBtSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                viewModel.dismissBtPicker()
                showBtSheet = false
            },
            sheetState = btSheetState,
        ) {
            BtDevicePickerContent(
                uiState = uiState,
                onScanClick = { viewModel.startBluetoothScan() },
                onDeviceSelected = { device ->
                    showBtSheet = false
                    viewModel.connectToDevice(device)
                },
                onDismiss = {
                    viewModel.dismissBtPicker()
                    showBtSheet = false
                },
            )
        }
    }
}

// ── Status row ────────────────────────────────────────────────────────────────

@Composable
private fun StatusRow(uiState: EcgMonitorUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Source chip
        when (uiState) {
            is EcgMonitorUiState.DemoRunning -> {
                StatusChip(
                    label = "DEMO · ${stringResource(uiState.pattern.labelRes)}",
                    containerColor = Color(0xFF2E2E2E),
                    contentColor = Color.White,
                    icon = { Icon(Icons.Default.MonitorHeart, null, tint = Color.White, modifier = Modifier.size(14.dp)) },
                    border = BorderStroke(1.5.dp, uiState.pattern.lineColor),
                )
            }
            is EcgMonitorUiState.BtConnected -> {
                StatusChip(
                    label = uiState.deviceName,
                    containerColor = Color(0xFF1B5E20).copy(alpha = 0.15f),
                    contentColor = Color(0xFF2E7D32),
                    icon = { Icon(Icons.Default.BluetoothConnected, null, modifier = Modifier.size(14.dp)) },
                )
            }
            is EcgMonitorUiState.BtConnecting -> {
                StatusChip(
                    label = stringResource(R.string.ecg_connecting, uiState.deviceName),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    icon = { CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp) },
                )
            }
            else -> {
                StatusChip(
                    label = stringResource(R.string.ecg_no_signal),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    icon = { Icon(Icons.Default.MonitorHeart, null, modifier = Modifier.size(14.dp)) },
                )
            }
        }

        // BPM display — always visible while a demo is running
        val demoRunning = uiState as? EcgMonitorUiState.DemoRunning
        AnimatedVisibility(visible = demoRunning != null, enter = fadeIn(), exit = fadeOut()) {
            val bpmText = demoRunning?.bpm
                ?.let { stringResource(R.string.ecg_bpm, it) }
                ?: stringResource(R.string.ecg_bpm_unknown)
            Text(
                text = bpmText,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun StatusChip(
    label: String,
    containerColor: Color,
    contentColor: Color,
    icon: @Composable () -> Unit,
    border: BorderStroke? = null,
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = containerColor,
        modifier = if (border != null)
            Modifier.border(border, MaterialTheme.shapes.small)
        else
            Modifier,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            icon()
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

// ── Color legend ──────────────────────────────────────────────────────────────

@Composable
private fun ColorLegend(uiState: EcgMonitorUiState) {
    val signalColor = signalColorFor(uiState)
    val peakColor = peakColorFor(uiState)
    val signalLabel = when (uiState) {
        is EcgMonitorUiState.DemoRunning ->
            "${stringResource(uiState.pattern.labelRes)} · ${uiState.pattern.nominalBpm} lpm"
        is EcgMonitorUiState.BtConnected -> stringResource(R.string.ecg_source_bt)
        else -> stringResource(R.string.ecg_legend_signal)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LegendItem(color = signalColor, label = signalLabel)
        LegendItem(color = peakColor, label = stringResource(R.string.ecg_legend_peak))
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color = color, shape = CircleShape),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ── Action buttons ────────────────────────────────────────────────────────────

@Composable
private fun ActionButtons(
    uiState: EcgMonitorUiState,
    onConnectEsp32: () -> Unit,
    onDemoClick: () -> Unit,
    showDemoMenu: Boolean,
    onDemoPatternSelected: (DemoPattern) -> Unit,
    onDemoMenuDismiss: () -> Unit,
    onPauseDemo: () -> Unit,
    onResumeDemo: () -> Unit,
    onStopDemo: () -> Unit,
    onDisconnectBt: () -> Unit,
) {
    when (uiState) {
        is EcgMonitorUiState.Idle -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onConnectEsp32,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Default.Bluetooth, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.ecg_connect_esp32))
                }
                Box(modifier = Modifier.weight(1f)) {
                    Button(
                        onClick = onDemoClick,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PqrstBurgundy,
                            contentColor = Color.White,
                        ),
                    ) {
                        Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.ecg_demo_mode))
                    }
                    DemoPatternDropdown(
                        expanded = showDemoMenu,
                        onPatternSelected = onDemoPatternSelected,
                        onDismiss = onDemoMenuDismiss,
                    )
                }
            }
        }
        is EcgMonitorUiState.DemoRunning -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (uiState.isPaused) {
                    Button(
                        onClick = onResumeDemo,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = uiState.pattern.lineColor),
                    ) {
                        Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.ecg_resume_demo))
                    }
                } else {
                    OutlinedButton(
                        onClick = onPauseDemo,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Default.Pause, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.ecg_pause_demo))
                    }
                }
                Button(
                    onClick = onStopDemo,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C), contentColor = Color.White),
                ) {
                    Icon(Icons.Default.Stop, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.ecg_stop_demo))
                }
            }
        }
        is EcgMonitorUiState.BtConnecting -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
                Text(
                    stringResource(R.string.ecg_connecting, uiState.deviceName),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        is EcgMonitorUiState.BtConnected -> {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.ecg_bt_waiting_signal),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedButton(
                    onClick = onDisconnectBt,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.ecg_disconnect))
                }
            }
        }
        else -> {}
    }
}

@Composable
private fun DemoPatternDropdown(
    expanded: Boolean,
    onPatternSelected: (DemoPattern) -> Unit,
    onDismiss: () -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DemoPattern.entries.forEach { pattern ->
            DropdownMenuItem(
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(pattern.lineColor, CircleShape),
                        )
                        Column {
                            Text(
                                text = stringResource(pattern.labelRes),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = stringResource(pattern.descRes),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                onClick = { onPatternSelected(pattern) },
            )
        }
    }
}

// ── Bluetooth device picker ───────────────────────────────────────────────────

@Composable
private fun BtDevicePickerContent(
    uiState: EcgMonitorUiState,
    onScanClick: () -> Unit,
    onDeviceSelected: (BtDeviceInfo) -> Unit,
    onDismiss: () -> Unit,
) {
    val btState = uiState as? EcgMonitorUiState.BtDeviceList

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.ecg_connect_esp32),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            if (btState?.isScanning == true) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                    Text(
                        text = stringResource(R.string.ecg_scanning),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                TextButton(onClick = onScanClick) {
                    Icon(
                        Icons.AutoMirrored.Filled.BluetoothSearching,
                        null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.ecg_scan))
                }
            }
        }

        if (btState == null) {
            Box(modifier = Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            DeviceSection(
                title = stringResource(R.string.ecg_paired_devices),
                devices = btState.paired,
                onDeviceSelected = onDeviceSelected,
            )
            if (btState.nearby.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                DeviceSection(
                    title = stringResource(R.string.ecg_nearby_devices),
                    devices = btState.nearby,
                    onDeviceSelected = onDeviceSelected,
                )
            }
            if (btState.paired.isEmpty() && btState.nearby.isEmpty() && !btState.isScanning) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.ecg_no_devices),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.cancel))
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun DeviceSection(
    title: String,
    devices: List<BtDeviceInfo>,
    onDeviceSelected: (BtDeviceInfo) -> Unit,
) {
    if (devices.isEmpty()) return
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 4.dp),
    )
    devices.forEach { device ->
        ListItem(
            headlineContent = { Text(device.name) },
            supportingContent = { Text(device.address, style = MaterialTheme.typography.labelSmall) },
            leadingContent = {
                Icon(Icons.Default.BluetoothConnected, null)
            },
            trailingContent = {
                FilledTonalButton(onClick = { onDeviceSelected(device) }) {
                    Text(stringResource(R.string.ecg_connect_action))
                }
            },
        )
    }
}

// ── Pattern description card ──────────────────────────────────────────────────

@Composable
private fun PatternDescriptionCard(pattern: DemoPattern) {
    val softWhite = Color(0xFFEEEEEE)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2E2E2E)),
        border = BorderStroke(width = 1.5.dp, color = pattern.lineColor),
    ) {
        // ── Demo banner ──
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Icon(
                imageVector = Icons.Default.MonitorHeart,
                contentDescription = null,
                tint = pattern.lineColor,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = "DEMO · ${stringResource(pattern.labelRes).uppercase()}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = softWhite,
            )
        }

        HorizontalDivider(color = pattern.lineColor)

        // ── Pattern features header ──
        Text(
            text = stringResource(R.string.ecg_pattern_features).uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = softWhite,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        HorizontalDivider(color = pattern.lineColor)

        // ── Pattern detail ──
        Text(
            text = stringResource(pattern.detailRes),
            style = MaterialTheme.typography.bodySmall,
            color = softWhite,
            lineHeight = MaterialTheme.typography.bodySmall.lineHeight * 1.4f,
            modifier = Modifier.padding(16.dp),
        )
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

@Composable
private fun signalColorFor(uiState: EcgMonitorUiState): Color = when (uiState) {
    is EcgMonitorUiState.DemoRunning -> Color(0xFF77202E)
    is EcgMonitorUiState.BtConnected -> Color(0xFF0097A7)
    else -> Color(0xFF9E9E9E)
}

@Composable
private fun peakColorFor(uiState: EcgMonitorUiState): Color = when (uiState) {
    is EcgMonitorUiState.DemoRunning -> uiState.pattern.peakColor
    else -> Color(0xFFFF1744)
}

// ── Previews ──────────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
private fun ColorLegendPreview() {
    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ColorLegend(uiState = EcgMonitorUiState.DemoRunning(DemoPattern.NORMAL, 216, 72))
            ColorLegend(uiState = EcgMonitorUiState.DemoRunning(DemoPattern.TACHYCARDIA, 430, 152))
            ColorLegend(uiState = EcgMonitorUiState.DemoRunning(DemoPattern.ATRIAL_FIBRILLATION, 350, 91))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DemoPatternDropdownPreview() {
    MaterialTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            DemoPatternDropdown(
                expanded = true,
                onPatternSelected = {},
                onDismiss = {},
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EcgMonitorScreenPreview() {
    MaterialTheme {
        Scaffold(
            topBar = {
                PqrstTopBar(
                    title = stringResource(R.string.ecg_monitor_title),
                    role = null,
                    onMenuClick = {},
                    onBackClick = {},
                )
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StatusRow(uiState = EcgMonitorUiState.Idle)
                Surface(
                    color = Color(0xFFFFF3F3),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    EcgChartWithPeaks(
                        signalBuffer = emptyList(),
                        peaks = emptyList(),
                        signalColor = Color(0xFF9E9E9E),
                        peakColor = Color(0xFFFF1744),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                    )
                }
                ColorLegend(uiState = EcgMonitorUiState.Idle)
                ActionButtons(
                    uiState = EcgMonitorUiState.Idle,
                    onConnectEsp32 = {},
                    onDemoClick = {},
                    showDemoMenu = false,
                    onDemoPatternSelected = {},
                    onDemoMenuDismiss = {},
                    onPauseDemo = {},
                    onResumeDemo = {},
                    onStopDemo = {},
                    onDisconnectBt = {},
                )
            }
        }
    }
}
