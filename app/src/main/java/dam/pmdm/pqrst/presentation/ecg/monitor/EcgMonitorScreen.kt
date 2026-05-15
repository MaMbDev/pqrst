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

/**
 * Live ECG monitor screen (RF-03).
 *
 * Serves two modes:
 * - **Demo mode**: synthesises ECG waveforms from [DemoPattern] without hardware.
 * - **Bluetooth mode**: connects to an ESP32 via Classic Bluetooth SPP and streams
 *   the incoming signal into the chart in real time.
 *
 * Layout (top → bottom, scrollable):
 * 1. [StatusRow] — source chip (demo/BT/idle) and animated BPM display.
 * 2. [EcgChartWithPeaks] inside a warm-tinted surface — the live waveform.
 * 3. [ColorLegend] — maps chart colours to pattern name and peak label.
 * 4. [ActionButtons] — context-sensitive controls that change with [EcgMonitorUiState].
 * 5. [PatternDescriptionCard] — educational description of the active demo pattern.
 * 6. Educational disclaimer (required by RF-06 and the educational-only constraint).
 *
 * A [ModalBottomSheet] for the BT device picker is conditionally rendered on top.
 *
 * State is hoisted to [EcgMonitorViewModel]. The screen only handles UI events
 * (permission result, menu dismissal) and delegates all logic to the ViewModel.
 *
 * @param consultationId The ID of the consultation this recording will be linked to.
 *                       Passed through to [EcgMonitorViewModel] for future save operations.
 * @param onBack Callback invoked when the user taps the back arrow in [PqrstTopBar].
 * @param viewModel Hilt-injected ViewModel; overridable for tests.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcgMonitorScreen(
    consultationId: Long,
    onBack: () -> Unit,
    onSaved: () -> Unit = {},
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

    // Select the correct permission set: BLUETOOTH_SCAN/CONNECT on Android 12+,
    // ACCESS_FINE_LOCATION on older versions (required for BT discovery pre-S).
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

    // Open the BT sheet automatically when the ViewModel transitions to BtDeviceList
    // (e.g. on permission grant or when the user re-opens the picker).
    LaunchedEffect(uiState) {
        if (uiState is EcgMonitorUiState.BtDeviceList) showBtSheet = true
    }

    // Navigate away after a successful save.
    LaunchedEffect(uiState) {
        if (uiState is EcgMonitorUiState.Saved) {
            snackbarHostState.showSnackbar(context.getString(R.string.ecg_saved))
            onSaved()
        }
    }

    // Collect one-shot events from the ViewModel Channel.
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
            // Warm off-white background mimics ECG paper to aid pattern recognition.
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
                onSaveCapture = { viewModel.saveCapture(consultationId) },
                onCancelCapture = { viewModel.cancelCapture() },
            )

            // ── Pattern description card ───────────────────────────────────────
            // Only visible when a demo is active; cast is safe because we guard on non-null.
            val demoState = uiState as? EcgMonitorUiState.DemoRunning
            if (demoState != null) {
                PatternDescriptionCard(pattern = demoState.pattern)
            }

            // ── Educational disclaimer ─────────────────────────────────────────
            // Required by the educational-only constraint in CLAUDE.md and RF-06.
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
    // Rendered outside the Scaffold Column so it overlays the entire screen correctly.
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

/**
 * Horizontal row showing the active signal source chip and an animated BPM readout.
 *
 * The chip appearance (icon, background, border) varies with [uiState] to give users
 * an immediate visual cue about the current signal source without reading text.
 * The BPM counter uses [AnimatedVisibility] to fade in only when a demo is running
 * so it doesn't compete with the chip for attention in idle/BT states.
 *
 * @param uiState The current monitor state, used to derive chip appearance and BPM value.
 */
@Composable
private fun StatusRow(uiState: EcgMonitorUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Source chip — label and icon change per state
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

        // BPM display — visible during demo or active BLE capture
        val bpmToShow: Int? = when (uiState) {
            is EcgMonitorUiState.DemoRunning -> uiState.bpm
            is EcgMonitorUiState.BtConnected -> uiState.bpm.takeIf { uiState.isCapturing }
            else -> null
        }
        val showBpm = bpmToShow != null ||
            uiState is EcgMonitorUiState.DemoRunning ||
            (uiState is EcgMonitorUiState.BtConnected && uiState.isCapturing)
        AnimatedVisibility(visible = showBpm, enter = fadeIn(), exit = fadeOut()) {
            val bpmText = bpmToShow
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

/**
 * Small pill-shaped chip used in [StatusRow] to indicate the active signal source.
 *
 * Accepts a slot-based [icon] parameter so each call site can supply the correct icon
 * without creating separate composable variants.
 *
 * @param label Text shown next to the icon.
 * @param containerColor Background colour of the chip.
 * @param contentColor Foreground colour applied to the [label] text.
 * @param icon Composable slot for the leading icon.
 * @param border Optional border stroke drawn around the chip (used for demo patterns).
 */
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

/**
 * Horizontal legend mapping chart colours to their meaning.
 *
 * The signal colour and label adapt to the active [uiState]: in demo mode the label
 * includes the pattern name and its nominal BPM; in BT mode it shows the source device
 * label; otherwise a generic "Signal" label is used.
 *
 * @param uiState The current monitor state, used to derive the signal label and colour.
 */
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

/**
 * A single coloured circle + text label entry within [ColorLegend].
 *
 * @param color The fill colour of the indicator dot.
 * @param label The text shown beside the dot.
 */
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

/**
 * Context-sensitive action area that adapts its content to [uiState].
 *
 * - **Idle**: shows "Connect ESP32" and "Demo mode" buttons side-by-side.
 *   The demo button anchors the [DemoPatternDropdown] so the dropdown appears
 *   directly below it.
 * - **DemoRunning**: shows Pause/Resume and Stop buttons.
 * - **BtConnecting**: shows a spinner with connecting text.
 * - **BtConnected**: shows a waiting message and a Disconnect button.
 *
 * All callbacks are lambdas passed in from the parent to keep this composable
 * stateless and easily testable.
 *
 * @param uiState Current monitor state controlling which button set is shown.
 * @param onConnectEsp32 Called when the user taps the Bluetooth connect button.
 * @param onDemoClick Called when the user taps the Demo mode button (shows the dropdown).
 * @param showDemoMenu Whether the [DemoPatternDropdown] should be visible.
 * @param onDemoPatternSelected Called when the user selects a pattern from the dropdown.
 * @param onDemoMenuDismiss Called when the dropdown is dismissed without a selection.
 * @param onPauseDemo Called to suspend demo sample generation.
 * @param onResumeDemo Called to resume demo sample generation.
 * @param onStopDemo Called to end the demo and return to idle.
 * @param onDisconnectBt Called to close the Bluetooth socket and return to idle without saving.
 * @param onSaveCapture Called to stop capture and persist the buffer to the linked consultation.
 * @param onCancelCapture Called to discard the buffer and disconnect without saving.
 */
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
    onSaveCapture: () -> Unit = {},
    onCancelCapture: () -> Unit = {},
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
                // Box wraps both the button and its dropdown so the anchor position is correct.
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
                    // Resume button uses the pattern's own accent colour for visual continuity.
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
            if (uiState.isCapturing) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val bpmText = uiState.bpm?.let { stringResource(R.string.ecg_bpm, it) } ?: "FC: --"
                    Text(
                        text = stringResource(R.string.ecg_capturing, uiState.sampleCount, bpmText),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OutlinedButton(
                            onClick = onCancelCapture,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(stringResource(R.string.ecg_cancel_capture))
                        }
                        Button(
                            onClick = onSaveCapture,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1B5E20),
                                contentColor = Color.White,
                            ),
                        ) {
                            Icon(Icons.Default.Stop, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.ecg_stop_save))
                        }
                    }
                }
            } else {
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
        }
        is EcgMonitorUiState.Saving -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
                Text(
                    stringResource(R.string.ecg_saving),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        else -> {}
    }
}

/**
 * Dropdown menu listing all available [DemoPattern] entries.
 *
 * Each item shows a colour dot matching [DemoPattern.lineColor], the pattern name,
 * and a one-line description. This gives students a visual preview of the pattern
 * before they select it.
 *
 * @param expanded Whether the dropdown is currently visible.
 * @param onPatternSelected Called with the chosen [DemoPattern] when the user taps an item.
 * @param onDismiss Called when the dropdown is dismissed without a selection.
 */
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

/**
 * Content rendered inside the Bluetooth bottom sheet.
 *
 * Displays paired and nearby (discovered) devices in separate [DeviceSection] groups.
 * A loading spinner is shown if [uiState] has not yet transitioned to [EcgMonitorUiState.BtDeviceList].
 *
 * @param uiState Current monitor state. Cast to [EcgMonitorUiState.BtDeviceList] to access device lists.
 * @param onScanClick Called when the user taps the re-scan button.
 * @param onDeviceSelected Called with the [BtDeviceInfo] the user taps "Connect" on.
 * @param onDismiss Called when the user taps Cancel or the sheet scrim.
 */
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
            // Still waiting for the ViewModel to deliver BtDeviceList state
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

/**
 * A labelled group of Bluetooth devices inside [BtDevicePickerContent].
 *
 * Renders a section header followed by a [ListItem] for each device. Returns early
 * if the list is empty so no empty header is shown.
 *
 * @param title Section header text (e.g. "Paired devices" or "Nearby devices").
 * @param devices The list of [BtDeviceInfo] to display.
 * @param onDeviceSelected Called with the selected device when the user taps "Connect".
 */
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

/**
 * Dark-themed card shown below the chart while a demo is running.
 *
 * Displays a "DEMO · PATTERN_NAME" banner, a "Pattern features" header, and the
 * multi-line educational detail text from [DemoPattern.detailRes]. The card border
 * uses the pattern's [DemoPattern.lineColor] so the card visually ties into the
 * waveform colour on screen.
 *
 * @param pattern The [DemoPattern] currently being simulated.
 */
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

/**
 * Returns the waveform line colour appropriate for the current [uiState].
 *
 * Demo patterns use their own burgundy-derived colour; BT connected uses teal to
 * distinguish real hardware data; all other states use a neutral grey.
 */
@Composable
private fun signalColorFor(uiState: EcgMonitorUiState): Color = when (uiState) {
    is EcgMonitorUiState.DemoRunning -> Color(0xFF77202E)
    is EcgMonitorUiState.BtConnected -> Color(0xFF0097A7)
    else -> Color(0xFF9E9E9E)
}

/**
 * Returns the R-peak marker colour appropriate for the current [uiState].
 *
 * During demo mode each pattern's [DemoPattern.peakColor] is used so peaks stand out
 * in the same hue family as the waveform. In all other states a bright red is used
 * for maximum visibility.
 */
@Composable
private fun peakColorFor(uiState: EcgMonitorUiState): Color = when (uiState) {
    is EcgMonitorUiState.DemoRunning -> uiState.pattern.peakColor
    else -> Color(0xFFFF1744)
}

// ── Previews ──────────────────────────────────────────────────────────────────

/** Preview of [ColorLegend] for three distinct demo pattern states. */
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

/** Preview of the [DemoPatternDropdown] in its expanded state. */
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

/** Full-screen static preview of [EcgMonitorScreen] in the Idle state. */
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
