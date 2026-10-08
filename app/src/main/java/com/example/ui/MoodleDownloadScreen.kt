package com.example.ui

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AddDownloadBehavior
import com.example.data.AppSettings
import com.example.data.DownloadEntity
import com.example.data.QueueSortOrder
import com.example.data.SpeedLimit
import com.example.data.ThemeMode
import com.example.model.DownloadState
import com.example.model.MoodleManifest
import com.example.ui.components.LottieDownloadGraphic
import com.example.ui.components.LottieScannerBeam
import com.example.ui.theme.BrandGradient
import com.example.ui.theme.BrandRadialGlowDark
import com.example.ui.theme.BrandRadialGlowLight
import com.example.ui.theme.BricolageGrotesqueFontFamily
import com.example.ui.theme.DmSansFontFamily
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ErrorRose
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber
import com.example.util.FileUtils
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoodleDownloadScreen(
    viewModel: DownloadViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val codeText by viewModel.codeText.collectAsStateWithLifecycle()
    val parseState by viewModel.parseState.collectAsStateWithLifecycle()

    val downloadingDownloads by viewModel.downloadingDownloads.collectAsStateWithLifecycle()
    val queuedDownloads by viewModel.queuedDownloads.collectAsStateWithLifecycle()
    val pausedDownloads by viewModel.pausedDownloads.collectAsStateWithLifecycle()
    val completedDownloads by viewModel.completedDownloads.collectAsStateWithLifecycle()
    val activeDownloads by viewModel.activeDownloads.collectAsStateWithLifecycle()

    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val showWelcomeSheet by viewModel.showWelcomeSheet.collectAsStateWithLifecycle()

    var showSettingsSheet by remember { mutableStateOf(false) }
    val activeListState = rememberLazyListState()

    LaunchedEffect(Unit) {
        viewModel.snackbarEvent.collectLatest { event ->
            if (event.actionLabel != null) {
                val result = snackbarHostState.showSnackbar(
                    message = event.message,
                    actionLabel = event.actionLabel,
                    duration = SnackbarDuration.Short
                )
                if (result == SnackbarResult.ActionPerformed) {
                    viewModel.selectTab(event.targetTab)
                }
            } else {
                snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDark) BrandRadialGlowDark else BrandRadialGlowLight),
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                onOpenCompleted = {
                    viewModel.selectTab(1)
                    scope.launch {
                        val totalActiveAndQueued =
                            downloadingDownloads.size + queuedDownloads.size + pausedDownloads.size

                        // Calcular el índice real de la sección "Completados".
                        // La lista contiene encabezados/acciones intercalados con las tarjetas.
                        if (completedDownloads.isNotEmpty()) {
                            var targetIndex = 0

                            if (totalActiveAndQueued > 1) {
                                targetIndex += 1 // acciones globales
                            }

                            if (totalActiveAndQueued == 0) {
                                targetIndex += 1 // "Sin descargas activas"
                            }

                            if (downloadingDownloads.isNotEmpty()) {
                                targetIndex += 1 + downloadingDownloads.size
                            }
                            if (queuedDownloads.isNotEmpty()) {
                                targetIndex += 1 + queuedDownloads.size
                            }
                            if (pausedDownloads.isNotEmpty()) {
                                targetIndex += 1 + pausedDownloads.size
                            }

                            var totalItems = 1 // spacer final

                            if (totalActiveAndQueued > 1) totalItems += 1
                            if (totalActiveAndQueued == 0) totalItems += 1
                            if (downloadingDownloads.isNotEmpty()) totalItems += 1 + downloadingDownloads.size
                            if (queuedDownloads.isNotEmpty()) totalItems += 1 + queuedDownloads.size
                            if (pausedDownloads.isNotEmpty()) totalItems += 1 + pausedDownloads.size
                            totalItems += 1 + completedDownloads.size // sección + tarjetas

                            val safeIndex = targetIndex.coerceIn(0, (totalItems - 1).coerceAtLeast(0))
                            activeListState.animateScrollToItem(safeIndex)
                        }
                    }
                },
                onOpenSettings = { showSettingsSheet = true }
            )
        },
        bottomBar = {
            FloatingPillNavigationBar(
                selectedIndex = selectedTab,
                activeCount = activeDownloads.size,
                onSelect = { viewModel.selectTab(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp, start = 24.dp, end = 24.dp)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .widthIn(max = 640.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            when (selectedTab) {
                0 -> DownloadInputTab(
                    codeText = codeText,
                    parseState = parseState,
                    downloadingCount = downloadingDownloads.size,
                    maxConcurrent = settings.maxConcurrentDownloads,
                    addBehavior = settings.addDownloadBehavior,
                    onCodeChanged = { viewModel.onCodeChanged(it) },
                    onStartDownload = { viewModel.startDownload() }
                )
                1 -> ActiveTasksTab(
                    downloadingDownloads = downloadingDownloads,
                    queuedDownloads = queuedDownloads,
                    pausedDownloads = pausedDownloads,
                    completedDownloads = completedDownloads,
                    listState = activeListState,
                    onPause = { viewModel.pauseDownload(it) },
                    onResume = { viewModel.resumeDownload(it) },
                    onRetry = { viewModel.retryDownload(it) },
                    onCancel = { viewModel.cancelDownload(it) },
                    onPauseAll = { viewModel.pauseAll() },
                    onResumeAll = { viewModel.resumeAll() },
                    onForceStartNow = { viewModel.forceStartNow(it) },
                    onMoveToTop = { viewModel.moveToTop(it) },
                    onDeleteCompleted = { viewModel.deleteCompletedItem(it) },
                    onDeleteSelectedCompleted = { viewModel.deleteSelectedCompleted(it) },
                    onClearCompleted = { viewModel.clearAllCompleted() },
                    onReDownload = { code, name -> viewModel.reDownload(code, name) },
                    onGoToDownload = { viewModel.selectTab(0) }
                )
            }
        }
    }

    if (showWelcomeSheet) {
        WelcomePermissionsBottomSheet(
            autoStartAcknowledged = settings.autoStartAcknowledged,
            onAutoStartAcknowledged = { viewModel.markAutoStartAcknowledged() },
            onDismiss = { dontShowAgain -> viewModel.dismissWelcomeSheet(dontShowAgain) }
        )
    }

    if (showSettingsSheet) {
        SettingsBottomSheet(
            settings = settings,
            onThemeChange = { viewModel.setThemeMode(it) },
            onDynamicColorChange = { viewModel.setDynamicColor(it) },
            onWifiOnlyChange = { viewModel.setWifiOnly(it) },
            onAutoRetryChange = { viewModel.setAutoRetry(it) },
            onMaxRetriesChange = { viewModel.setMaxRetries(it) },
            onMaxConcurrentDownloadsChange = { viewModel.setMaxConcurrentDownloads(it) },
            onMaxConcurrentPartsChange = { viewModel.setMaxConcurrentParts(it) },
            onAddDownloadBehaviorChange = { viewModel.setAddDownloadBehavior(it) },
            onAutoStartNextChange = { viewModel.setAutoStartNext(it) },
            onQueueSortOrderChange = { viewModel.setQueueSortOrder(it) },
            onSpeedLimitChange = { viewModel.setSpeedLimit(it) },
            onVibrateChange = { viewModel.setVibrateOnComplete(it) },
            onAutoClearChange = { viewModel.setAutoClearOnStart(it) },
            onShowProgressNotifsChange = { viewModel.setShowProgressNotifications(it) },
            onSoundOnCompleteChange = { viewModel.setSoundOnComplete(it) },
            onOpenWelcome = {
                showSettingsSheet = false
                viewModel.openWelcomeSheetManually()
            },
            onDismiss = { showSettingsSheet = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopAppBar(
    onOpenCompleted: () -> Unit,
    onOpenSettings: () -> Unit
) {
    CenterAlignedTopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(BrandGradient),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = "Download Chunk",
                    fontFamily = BricolageGrotesqueFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp,
                    letterSpacing = (-0.3).sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        actions = {
            IconButton(
                onClick = onOpenCompleted,
                modifier = Modifier
                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    .testTag("open_completed_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = "Ver archivos completados",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    .testTag("settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Ajustes",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = Color.Transparent
        )
    )
}

/**
 * Barra flotante de pestañas sin ripple pegado ni cuadros grises superpuestos.
 * Controlada exclusivamente por `selectedIndex`.
 */
@Composable
fun FloatingPillNavigationBar(
    selectedIndex: Int,
    activeCount: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(32.dp),
                    ambientColor = ElectricBlue.copy(alpha = 0.25f),
                    spotColor = ElectricCyan.copy(alpha = 0.25f)
                ),
            shape = RoundedCornerShape(32.dp),
            color = if (isDark) MaterialTheme.colorScheme.surface.copy(alpha = 0.94f) else Color(0xFFF1F5F9),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PillNavButton(
                    title = "Descargar",
                    icon = Icons.Default.Download,
                    badge = null,
                    isSelected = selectedIndex == 0,
                    isDark = isDark,
                    onClick = { onSelect(0) }
                )

                PillNavButton(
                    title = "En curso",
                    icon = Icons.Default.Layers,
                    badge = if (activeCount > 0) activeCount.toString() else null,
                    isSelected = selectedIndex == 1,
                    isDark = isDark,
                    onClick = { onSelect(1) }
                )
            }
        }
    }
}

@Composable
private fun PillNavButton(
    title: String,
    icon: ImageVector,
    badge: String?,
    isSelected: Boolean,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val targetBg = if (isSelected) {
        if (isDark) Color.White else Color(0xFF0F172A)
    } else {
        Color.Transparent
    }

    val targetFg = if (isSelected) {
        if (isDark) Color(0xFF0F172A) else Color.White
    } else {
        if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
    }

    val bg by animateColorAsState(targetValue = targetBg, animationSpec = tween(180), label = "pill_bg")
    val fg by animateColorAsState(targetValue = targetFg, animationSpec = tween(180), label = "pill_fg")

    Box(
        modifier = Modifier
            .defaultMinSize(minHeight = 44.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(bg)
            .clickable(
                interactionSource = remember(title) { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 22.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(17.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                fontFamily = DmSansFontFamily,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 14.sp,
                color = fg
            )

            if (badge != null) {
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isSelected) ElectricBlue else ElectricCyan)
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = badge,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = DmSansFontFamily
                    )
                }
            }
        }
    }
}

/**
 * Pantalla Descargar
 */
@Composable
fun DownloadInputTab(
    codeText: String,
    parseState: ParseUiState,
    downloadingCount: Int,
    maxConcurrent: Int,
    addBehavior: AddDownloadBehavior,
    onCodeChanged: (String) -> Unit,
    onStartDownload: () -> Unit
) {
    val context = LocalContext.current
    val isValid = parseState is ParseUiState.Valid
    val isInvalid = parseState is ParseUiState.Invalid
    val isValidating = parseState is ParseUiState.Validating

    val isQueueDestination = downloadingCount >= maxConcurrent || addBehavior == AddDownloadBehavior.ADD_TO_QUEUE_ONLY
    val primaryButtonLabel = if (isQueueDestination) "Agregar a la cola" else "Descargar"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Pega tu código.\nDescarga en partes.",
                fontFamily = BricolageGrotesqueFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 32.sp,
                lineHeight = 38.sp,
                letterSpacing = (-0.8).sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Reconstruye y descarga archivos segmentados desde bots de Moodle a tu almacenamiento.",
                fontFamily = DmSansFontFamily,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Código Moodle",
                                fontFamily = DmSansFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            if (isValidating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = ElectricCyan
                                )
                            } else if (isValid) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Código Válido",
                                    tint = SuccessGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Button(
                            onClick = {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                val clip = cm?.primaryClip
                                if (clip != null && clip.itemCount > 0) {
                                    val text = clip.getItemAt(0).text?.toString() ?: ""
                                    if (text.isNotBlank()) onCodeChanged(text)
                                    else Toast.makeText(context, "El portapapeles está vacío", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.defaultMinSize(minHeight = 44.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pegar", fontFamily = DmSansFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    OutlinedTextField(
                        value = codeText,
                        onValueChange = onCodeChanged,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .testTag("code_input_field"),
                        placeholder = {
                            Text(
                                text = "https://5.4.3.2.1:...",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        ),
                        trailingIcon = {
                            if (codeText.isNotEmpty()) {
                                IconButton(
                                    onClick = { onCodeChanged("") },
                                    modifier = Modifier.defaultMinSize(minWidth = 44.dp, minHeight = 44.dp)
                                ) {
                                    Icon(Icons.Default.Clear, contentDescription = "Borrar", tint = MaterialTheme.colorScheme.outline)
                                }
                            }
                        },
                        shape = RoundedCornerShape(18.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricCyan,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                        )
                    )

                    if (isValidating) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                        ) {
                            LottieScannerBeam(modifier = Modifier.fillMaxSize())
                        }
                    }

                    if (isInvalid && codeText.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = ErrorRose, modifier = Modifier.size(16.dp))
                            Text(
                                text = (parseState as ParseUiState.Invalid).message,
                                fontFamily = DmSansFontFamily,
                                fontSize = 12.sp,
                                color = ErrorRose
                            )
                        }
                    }

                    val manifestSizeText = if (isValid) {
                        " • ${FileUtils.formatBytes((parseState as ParseUiState.Valid).manifest.size)}"
                    } else ""

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 50.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                if (isValid) BrandGradient
                                else Brush.linearGradient(
                                    listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surfaceVariant)
                                )
                            )
                            .clickable(enabled = isValid, onClick = onStartDownload)
                            .padding(vertical = 14.dp)
                            .testTag("start_download_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (isQueueDestination) Icons.Default.Layers else Icons.Default.Download,
                                contentDescription = null,
                                tint = if (isValid) Color.White else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "$primaryButtonLabel$manifestSizeText",
                                fontFamily = DmSansFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = if (isValid) Color.White else MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
        }

        item {
            AnimatedVisibility(
                visible = isValid,
                enter = fadeIn(tween(300)) + expandVertically(tween(350)),
                exit = fadeOut(tween(200)) + shrinkVertically(tween(250))
            ) {
                if (parseState is ParseUiState.Valid) {
                    VerifiedManifestCard(manifest = parseState.manifest)
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}

@Composable
fun VerifiedManifestCard(manifest: MoodleManifest) {
    val fileIcon = getFileIconForExtension(manifest.filename)
    val isApk = manifest.filename.endsWith(".apk", ignoreCase = true)
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(ElectricBlue.copy(alpha = 0.35f))
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isApk) SuccessGreen.copy(alpha = 0.15f) else ElectricBlue.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = fileIcon,
                        contentDescription = null,
                        tint = if (isApk) SuccessGreen else ElectricBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = manifest.filename,
                        fontFamily = BricolageGrotesqueFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        lineHeight = 22.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        softWrap = true,
                        maxLines = 6
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = FileUtils.formatBytes(manifest.size),
                            fontFamily = DmSansFontFamily,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) ElectricCyan else Color(0xFF007399)
                        )
                        Text(text = "•", color = MaterialTheme.colorScheme.outline)
                        Text(
                            text = "${manifest.parts.size} partes",
                            fontFamily = DmSansFontFamily,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            val hashText = if (!manifest.sha256.isNullOrBlank() && manifest.sha256 != "null") {
                val cleanHash = manifest.sha256.trim()
                if (cleanHash.length > 16) "${cleanHash.take(10)}...${cleanHash.takeLast(8)}" else cleanHash
            } else {
                "Hash pendiente"
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = if (hashText == "Hash pendiente") MaterialTheme.colorScheme.outline else SuccessGreen,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = if (hashText == "Hash pendiente") "Hash pendiente" else "SHA-256: $hashText",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = if (hashText == "Hash pendiente") MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(ElectricBlue.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Listo",
                            fontFamily = DmSansFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) ElectricCyan else Color(0xFF007399)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Animación dinámica fluida estilo Lottie para el estado vacío
 */
@Composable
fun LottieStyleEmptyIllustration(reducedMotion: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "lottie_empty")

    val pulse1 by if (reducedMotion) remember { mutableStateOf(1f) } else {
        infiniteTransition.animateFloat(
            initialValue = 0.8f,
            targetValue = 1.35f,
            animationSpec = infiniteRepeatable(animation = tween(2200), repeatMode = RepeatMode.Restart),
            label = "pulse1"
        )
    }
    val alpha1 by if (reducedMotion) remember { mutableStateOf(0.2f) } else {
        infiniteTransition.animateFloat(
            initialValue = 0.5f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(animation = tween(2200), repeatMode = RepeatMode.Restart),
            label = "alpha1"
        )
    }

    val floatY by if (reducedMotion) remember { mutableStateOf(0f) } else {
        infiniteTransition.animateFloat(
            initialValue = -6f,
            targetValue = 6f,
            animationSpec = infiniteRepeatable(animation = tween(1800), repeatMode = RepeatMode.Reverse),
            label = "floatY"
        )
    }

    Box(
        modifier = Modifier.size(96.dp),
        contentAlignment = Alignment.Center
    ) {
        // Ondas concéntricas de pulso
        Box(
            modifier = Modifier
                .size((66 * pulse1).dp)
                .clip(CircleShape)
                .border(2.dp, ElectricCyan.copy(alpha = alpha1), CircleShape)
        )

        // Pod central flotante
        Box(
            modifier = Modifier
                .size(64.dp)
                .graphicsLayer { translationY = floatY }
                .shadow(14.dp, CircleShape, ambientColor = ElectricBlue, spotColor = ElectricCyan)
                .clip(CircleShape)
                .background(BrandGradient),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

/**
 * Pantalla En curso:
 * Con selección múltiple para borrar, sin botón "Instalar / Abrir",
 * y animaciones dinámicas fluidas.
 */
@Composable
fun ActiveTasksTab(
    downloadingDownloads: List<DownloadEntity>,
    queuedDownloads: List<DownloadEntity>,
    pausedDownloads: List<DownloadEntity>,
    completedDownloads: List<DownloadEntity>,
    listState: LazyListState,
    onPause: (String) -> Unit,
    onResume: (String) -> Unit,
    onRetry: (String) -> Unit,
    onCancel: (String) -> Unit,
    onPauseAll: () -> Unit,
    onResumeAll: () -> Unit,
    onForceStartNow: (String) -> Unit,
    onMoveToTop: (String) -> Unit,
    onDeleteCompleted: (String) -> Unit,
    onDeleteSelectedCompleted: (List<String>) -> Unit,
    onClearCompleted: () -> Unit,
    onReDownload: (String, String) -> Unit,
    onGoToDownload: () -> Unit
) {
    val context = LocalContext.current
    val reducedMotion = remember { isReducedMotion(context) }
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    val totalActiveAndQueued = downloadingDownloads.size + queuedDownloads.size + pausedDownloads.size

    // Estado de selección múltiple para completados
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Acciones globales si hay más de 1 activa
        if (totalActiveAndQueued > 1) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (downloadingDownloads.isNotEmpty()) {
                        OutlinedButton(
                            onClick = onPauseAll,
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 40.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pausar todo", fontFamily = DmSansFontFamily, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (pausedDownloads.isNotEmpty()) {
                        Button(
                            onClick = onResumeAll,
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 40.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue, contentColor = Color.White)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Reanudar todo", fontFamily = DmSansFontFamily, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Fila compacta "Nueva descarga" si no hay activas pero sí completados
        if (totalActiveAndQueued == 0 && completedDownloads.isNotEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Sin descargas activas",
                            fontFamily = DmSansFontFamily,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = onGoToDownload,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.defaultMinSize(minHeight = 36.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Nueva descarga", fontFamily = DmSansFontFamily, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Animación Lottie-style cuando no hay absolutamente nada
        if (totalActiveAndQueued == 0 && completedDownloads.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        LottieDownloadGraphic(sizeDp = 130.dp, reducedMotion = reducedMotion)

                        Text(
                            text = "Nada descargándose. Pega un código para empezar.",
                            fontFamily = DmSansFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Button(
                            onClick = onGoToDownload,
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.defaultMinSize(minHeight = 44.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Ir a Descargar", fontFamily = DmSansFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // Sección: Descargando
        if (downloadingDownloads.isNotEmpty()) {
            item {
                SectionHeader(title = "Descargando", count = downloadingDownloads.size, color = ElectricCyan)
            }
            items(downloadingDownloads, key = { it.id }) { task ->
                ActiveTaskCard(
                    task = task,
                    reducedMotion = reducedMotion,
                    isDark = isDark,
                    onPause = { onPause(task.id) },
                    onResume = { onResume(task.id) },
                    onRetry = { onRetry(task.id) },
                    onCancel = { onCancel(task.id) }
                )
            }
        }

        // Sección: En cola
        if (queuedDownloads.isNotEmpty()) {
            item {
                SectionHeader(title = "En cola", count = queuedDownloads.size, color = MaterialTheme.colorScheme.outline)
            }
            items(queuedDownloads, key = { it.id }) { task ->
                val indexInQueue = queuedDownloads.indexOf(task) + 1
                QueuedTaskCard(
                    task = task,
                    position = indexInQueue,
                    onStartNow = { onForceStartNow(task.id) },
                    onMoveToTop = { onMoveToTop(task.id) },
                    onRemove = { onCancel(task.id) }
                )
            }
        }

        // Sección: Pausadas
        if (pausedDownloads.isNotEmpty()) {
            item {
                SectionHeader(title = "Pausadas", count = pausedDownloads.size, color = WarningAmber)
            }
            items(pausedDownloads, key = { it.id }) { task ->
                ActiveTaskCard(
                    task = task,
                    reducedMotion = reducedMotion,
                    isDark = isDark,
                    onPause = { onPause(task.id) },
                    onResume = { onResume(task.id) },
                    onRetry = { onRetry(task.id) },
                    onCancel = { onCancel(task.id) }
                )
            }
        }

        // Sección: Completados
        if (completedDownloads.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(10.dp))
                if (isSelectionMode) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = ElectricCyan,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "${selectedIds.size} de ${completedDownloads.size} seleccionados",
                                        fontFamily = DmSansFontFamily,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                TextButton(
                                    onClick = {
                                        selectedIds = if (selectedIds.size == completedDownloads.size) {
                                            emptySet()
                                        } else {
                                            completedDownloads.map { it.id }.toSet()
                                        }
                                    }
                                ) {
                                    Text(
                                        text = if (selectedIds.size == completedDownloads.size) "Deseleccionar todo" else "Seleccionar todo",
                                        fontFamily = DmSansFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = ElectricCyan
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = {
                                        if (selectedIds.isNotEmpty()) {
                                            onDeleteSelectedCompleted(selectedIds.toList())
                                            selectedIds = emptySet()
                                            isSelectionMode = false
                                        }
                                    },
                                    enabled = selectedIds.isNotEmpty(),
                                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRose, contentColor = Color.White),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .defaultMinSize(minHeight = 40.dp)
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Borrar seleccionados", fontFamily = DmSansFontFamily, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        isSelectionMode = false
                                        selectedIds = emptySet()
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.defaultMinSize(minHeight = 40.dp)
                                ) {
                                    Text("Cancelar", fontFamily = DmSansFontFamily, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SectionHeader(title = "Completados", count = completedDownloads.size, color = SuccessGreen)

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(
                                onClick = { isSelectionMode = true },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.defaultMinSize(minHeight = 38.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                )
                            ) {
                                Icon(Icons.Default.CheckBox, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Seleccionar", fontFamily = DmSansFontFamily, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }

                            OutlinedButton(
                                onClick = onClearCompleted,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.defaultMinSize(minHeight = 38.dp)
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Borrar todo", fontFamily = DmSansFontFamily, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            items(completedDownloads, key = { it.id }) { item ->
                val isSelected = selectedIds.contains(item.id)
                CompletedFileCard(
                    item = item,
                    isSelectionMode = isSelectionMode,
                    isSelected = isSelected,
                    onToggleSelect = {
                        selectedIds = if (isSelected) selectedIds - item.id else selectedIds + item.id
                    },
                    onLongClick = {
                        isSelectionMode = true
                        selectedIds = selectedIds + item.id
                    },
                    onShare = {
                        item.mediaStoreUri?.let { uriStr ->
                            try {
                                val uri = Uri.parse(uriStr)
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = context.contentResolver.getType(uri) ?: "*/*"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Compartir archivo"))
                            } catch (_: Exception) {
                                Toast.makeText(context, "No se pudo compartir el archivo.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onDelete = { onDeleteCompleted(item.id) },
                    onReDownload = onReDownload
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(84.dp))
        }
    }
}

@Composable
fun SectionHeader(title: String, count: Int, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            fontFamily = BricolageGrotesqueFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(color.copy(alpha = 0.2f))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = count.toString(),
                fontFamily = DmSansFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = color
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ActiveTaskCard(
    task: DownloadEntity,
    reducedMotion: Boolean,
    isDark: Boolean,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onRetry: () -> Unit,
    onCancel: () -> Unit
) {
    val fileIcon = getFileIconForExtension(task.fileName)
    var isExpanded by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "part_pulse")
    val pulseAlpha by if (reducedMotion) {
        remember { mutableStateOf(1f) }
    } else {
        infiniteTransition.animateFloat(
            initialValue = 0.5f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(animation = tween(800), repeatMode = RepeatMode.Reverse),
            label = "pulse_alpha"
        )
    }

    val isPaused = task.status == DownloadState.PAUSED.name || task.status == DownloadState.PAUSING.name
    val isPausing = task.status == DownloadState.PAUSING.name
    val isError = task.status == DownloadState.ERROR.name

    val percent = if (task.totalBytes > 0) {
        ((task.downloadedBytes * 100) / task.totalBytes).toInt().coerceIn(0, 100)
    } else 0

    val progressFraction = if (task.totalBytes > 0) {
        (task.downloadedBytes.toFloat() / task.totalBytes.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = progressFraction,
        animationSpec = tween(durationMillis = 300),
        label = "task_progress_smooth"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (isError) ErrorRose.copy(alpha = 0.4f)
                else if (isPaused) WarningAmber.copy(alpha = 0.4f)
                else ElectricBlue.copy(alpha = 0.35f)
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .then(
                            if (isError) Modifier.background(ErrorRose.copy(alpha = 0.15f))
                            else if (isPaused) Modifier.background(WarningAmber.copy(alpha = 0.15f))
                            else Modifier.background(BrandGradient)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = fileIcon,
                        contentDescription = null,
                        tint = if (isError) ErrorRose else if (isPaused) WarningAmber else Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.fileName,
                        fontFamily = BricolageGrotesqueFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = if (isExpanded) 10 else 2,
                        lineHeight = 20.sp,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${FileUtils.formatBytes(task.downloadedBytes)} de ${FileUtils.formatBytes(task.totalBytes)}",
                        fontFamily = DmSansFontFamily,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                val isDownloading = task.status == DownloadState.DOWNLOADING.name
                if (isDownloading) {
                    val displaySpeed = if (task.speedBps > 0) {
                        "${FileUtils.formatBytes(task.speedBps)}/s"
                    } else {
                        "Descargando..."
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(ElectricCyan.copy(alpha = 0.15f))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = null,
                                tint = if (isDark) ElectricCyan else Color(0xFF007399),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = displaySpeed,
                                fontFamily = DmSansFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) ElectricCyan else Color(0xFF007399)
                            )
                        }
                    }
                } else if (isPausing) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(WarningAmber.copy(alpha = 0.15f))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "Pausando...",
                            fontFamily = DmSansFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = WarningAmber
                        )
                    }
                } else if (isPaused) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(WarningAmber.copy(alpha = 0.15f))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "Pausada",
                            fontFamily = DmSansFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = WarningAmber
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "$percent",
                        fontFamily = BricolageGrotesqueFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 40.sp,
                        lineHeight = 42.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "%",
                        fontFamily = BricolageGrotesqueFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = if (isError) ErrorRose else if (isPaused) WarningAmber else ElectricCyan,
                        modifier = Modifier.padding(start = 2.dp, bottom = 4.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (isError) "Error" else if (isPausing) "Pausando..." else if (isPaused) "Pausada" else "Parte ${task.currentPart} de ${task.totalParts}",
                        fontFamily = DmSansFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = if (isError) ErrorRose else if (isPaused) WarningAmber else MaterialTheme.colorScheme.onSurface
                    )

                    val etaText = when {
                        isError || isPaused || isPausing -> null
                        task.etaSeconds > 0 -> "~${FileUtils.formatDuration(task.etaSeconds)} restantes"
                        task.totalBytes > 0 && task.downloadedBytes > 0 -> "Calculando..."
                        else -> null
                    }
                    if (etaText != null) {
                        Text(
                            text = etaText,
                            fontFamily = DmSansFontFamily,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            AccurateTaskProgressBar(
                progressFraction = animatedProgress,
                isIndeterminate = task.totalBytes <= 0L,
                isPaused = isPaused,
                isError = isError,
                downloadedBytes = task.downloadedBytes,
                totalBytes = task.totalBytes,
                currentPart = task.currentPart,
                totalParts = task.totalParts,
                completedParts = task.completedParts,
                isDark = isDark
            )

            if (!task.errorMessage.isNullOrBlank()) {
                Text(
                    text = task.errorMessage,
                    fontFamily = DmSansFontFamily,
                    fontSize = 12.sp,
                    color = if (isError) ErrorRose else WarningAmber
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isPaused) {
                    Button(
                        onClick = onResume,
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 44.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue, contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reanudar", fontFamily = DmSansFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                } else if (isError) {
                    Button(
                        onClick = onRetry,
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 44.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = WarningAmber, contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reintentar", fontFamily = DmSansFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                } else {
                    OutlinedButton(
                        onClick = onPause,
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 44.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pausar", fontFamily = DmSansFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                OutlinedButton(
                    onClick = onCancel,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.defaultMinSize(minHeight = 44.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRose),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRose.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Cancelar", fontFamily = DmSansFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

/**
 * Barra de progreso continua, fluida y exacta que refleja fielmente los bytes descargados
 * respecto al total, con soporte para fragmentos e indicador indeterminado.
 */
@Composable
fun AccurateTaskProgressBar(
    progressFraction: Float,
    isIndeterminate: Boolean,
    isPaused: Boolean,
    isError: Boolean,
    downloadedBytes: Long,
    totalBytes: Long,
    currentPart: Int,
    totalParts: Int,
    completedParts: Int,
    isDark: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (isIndeterminate) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp)),
                color = ElectricCyan,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                val safeWidth = progressFraction.coerceIn(0f, 1f)
                if (safeWidth > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(safeWidth)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(5.dp))
                            .then(
                                when {
                                    isError -> Modifier.background(ErrorRose)
                                    isPaused -> Modifier.background(WarningAmber)
                                    else -> Modifier.background(BrandGradient)
                                }
                            )
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${FileUtils.formatBytes(downloadedBytes)} de ${FileUtils.formatBytes(totalBytes)}",
                fontFamily = DmSansFontFamily,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (totalParts > 1) {
                Text(
                    text = "Parte $currentPart de $totalParts ($completedParts completadas)",
                    fontFamily = DmSansFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDark) ElectricCyan else Color(0xFF007399)
                )
            }
        }
    }
}

@Composable
fun QueuedTaskCard(
    task: DownloadEntity,
    position: Int,
    onStartNow: () -> Unit,
    onMoveToTop: () -> Unit,
    onRemove: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "#$position",
                    fontFamily = BricolageGrotesqueFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = ElectricCyan
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.fileName,
                    fontFamily = BricolageGrotesqueFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 2,
                    lineHeight = 18.sp,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${FileUtils.formatBytes(task.totalBytes)} • ${task.totalParts} partes",
                    fontFamily = DmSansFontFamily,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Opciones")
                }

                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text("Iniciar ahora", fontFamily = DmSansFontFamily, fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null, tint = ElectricCyan) },
                        onClick = {
                            menuExpanded = false
                            onStartNow()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Mover al inicio", fontFamily = DmSansFontFamily) },
                        leadingIcon = { Icon(Icons.Default.ArrowUpward, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onMoveToTop()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Quitar de la cola", fontFamily = DmSansFontFamily, color = ErrorRose) },
                        leadingIcon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = ErrorRose) },
                        onClick = {
                            menuExpanded = false
                            onRemove()
                        }
                    )
                }
            }
        }
    }
}

/**
 * Tarjeta de archivo completado:
 * - Sin botón "Instalar / Abrir"
 * - Permite Compartir y Borrar
 * - Soporta selección múltiple
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CompletedFileCard(
    item: DownloadEntity,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onLongClick: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onReDownload: (String, String) -> Unit
) {
    val context = LocalContext.current
    val fileIcon = getFileIconForExtension(item.fileName)
    val isSuccess = item.status == DownloadState.COMPLETED.name
    val isWarning = item.status == "COMPLETED_WARN_HASH"
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    // Verificación en tiempo real si el archivo existe físicamente en el almacenamiento del dispositivo
    val fileExistsInStorage = remember(item.mediaStoreUri, item.savedPath, item.fileName) {
        FileUtils.isFileInStorage(context, item.mediaStoreUri, item.savedPath, item.fileName)
    }
    val actualStorageBytes = remember(fileExistsInStorage, item.mediaStoreUri, item.savedPath, item.fileName) {
        if (fileExistsInStorage) FileUtils.getFileStorageSize(context, item.mediaStoreUri, item.savedPath, item.fileName) else 0L
    }
    val displayBytes = if (actualStorageBytes > 0L) actualStorageBytes else item.totalBytes

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) {
                        onToggleSelect()
                    } else if (fileExistsInStorage) {
                        onShare()
                    } else {
                        Toast.makeText(context, "El archivo ya no se encuentra en el almacenamiento. Puedes descargarlo de nuevo.", Toast.LENGTH_SHORT).show()
                    }
                },
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) ElectricBlue.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (isSelected) ElectricCyan
                else if (!fileExistsInStorage) WarningAmber.copy(alpha = 0.5f)
                else if (isSuccess) SuccessGreen.copy(alpha = 0.35f)
                else WarningAmber.copy(alpha = 0.35f)
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Indicador animado de selección múltiple
                AnimatedVisibility(
                    visible = isSelectionMode,
                    enter = slideInHorizontally() + fadeIn(),
                    exit = slideOutHorizontally() + fadeOut()
                ) {
                    IconButton(
                        onClick = onToggleSelect,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isSelected) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                            contentDescription = if (isSelected) "Deseleccionar" else "Seleccionar",
                            tint = if (isSelected) ElectricCyan else MaterialTheme.colorScheme.outline
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (!fileExistsInStorage) WarningAmber.copy(alpha = 0.15f)
                            else if (isSuccess) SuccessGreen.copy(alpha = 0.15f)
                            else WarningAmber.copy(alpha = 0.15f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = fileIcon,
                        contentDescription = null,
                        tint = if (!fileExistsInStorage) WarningAmber else if (isSuccess) SuccessGreen else WarningAmber,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.fileName,
                        fontFamily = BricolageGrotesqueFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        lineHeight = 20.sp,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = FileUtils.formatBytes(displayBytes),
                            fontFamily = DmSansFontFamily,
                            fontSize = 12.sp,
                            color = if (isDark) ElectricCyan else Color(0xFF007399),
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(text = "•", color = MaterialTheme.colorScheme.outline)
                        if (fileExistsInStorage) {
                            Text(
                                text = "En almacenamiento",
                                fontFamily = DmSansFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = SuccessGreen
                            )
                        } else {
                            Text(
                                text = "No encontrado en disco",
                                fontFamily = DmSansFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = WarningAmber
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.defaultMinSize(minWidth = 44.dp, minHeight = 44.dp)
                ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.outline)
                }
            }

            // Acciones: Si el archivo existe -> Compartir / Borrar. Si fue borrado del disco -> Volver a descargar / Eliminar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (fileExistsInStorage) {
                    Button(
                        onClick = onShare,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 42.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Compartir", fontFamily = DmSansFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = onDelete,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.defaultMinSize(minHeight = 42.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRose),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRose.copy(alpha = 0.4f))
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Borrar", fontFamily = DmSansFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                } else {
                    Button(
                        onClick = { onReDownload(item.code, item.fileName) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 42.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricBlue,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Volver a descargar", fontFamily = DmSansFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = onDelete,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.defaultMinSize(minHeight = 42.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRose),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRose.copy(alpha = 0.4f))
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Eliminar", fontFamily = DmSansFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

/**
 * Ajustes completos con texto de batería corregido (sin "y datos")
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBottomSheet(
    settings: AppSettings,
    onThemeChange: (ThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onWifiOnlyChange: (Boolean) -> Unit,
    onAutoRetryChange: (Boolean) -> Unit,
    onMaxRetriesChange: (Int) -> Unit,
    onMaxConcurrentDownloadsChange: (Int) -> Unit,
    onMaxConcurrentPartsChange: (Int) -> Unit,
    onAddDownloadBehaviorChange: (AddDownloadBehavior) -> Unit,
    onAutoStartNextChange: (Boolean) -> Unit,
    onQueueSortOrderChange: (QueueSortOrder) -> Unit,
    onSpeedLimitChange: (SpeedLimit) -> Unit,
    onVibrateChange: (Boolean) -> Unit,
    onAutoClearChange: (Boolean) -> Unit,
    onShowProgressNotifsChange: (Boolean) -> Unit,
    onSoundOnCompleteChange: (Boolean) -> Unit,
    onOpenWelcome: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 6.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ajustes",
                        fontFamily = BricolageGrotesqueFontFamily,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.defaultMinSize(minWidth = 44.dp, minHeight = 44.dp)
                    ) {
                        Icon(Icons.Default.Clear, contentDescription = "Cerrar")
                    }
                }
            }

            // SECCIÓN: Cola y simultaneidad
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Cola y simultaneidad",
                        fontFamily = DmSansFontFamily,
                        style = MaterialTheme.typography.labelLarge,
                        color = ElectricCyan,
                        fontWeight = FontWeight.Bold
                    )

                    SettingOptionSelectorRow(
                        title = "Descargas simultáneas",
                        description = "Cuántos archivos se descargan a la vez.",
                        currentValue = "${settings.maxConcurrentDownloads} archivos",
                        options = listOf(1, 2, 3, 4),
                        selectedOption = settings.maxConcurrentDownloads,
                        onOptionSelected = onMaxConcurrentDownloadsChange
                    )

                    // Texto corregido: Sin mención a "datos" ni "gasta más datos"
                    SettingOptionSelectorRow(
                        title = "Partes simultáneas por archivo",
                        description = "Más partes a la vez puede aumentar la velocidad en archivos grandes.",
                        currentValue = "${settings.maxConcurrentParts} partes",
                        options = listOf(1, 2, 3, 4),
                        selectedOption = settings.maxConcurrentParts,
                        onOptionSelected = onMaxConcurrentPartsChange
                    )

                    SettingRadioGroupRow(
                        title = "Al agregar una descarga",
                        description = "Comportamiento al presionar el botón de descarga.",
                        currentLabel = settings.addDownloadBehavior.label,
                        options = AddDownloadBehavior.values().toList(),
                        selected = settings.addDownloadBehavior,
                        labelProvider = { it.label },
                        onSelect = onAddDownloadBehaviorChange
                    )

                    SettingToggleRow(
                        title = "Iniciar la siguiente automáticamente",
                        subtitle = "Arrancar la próxima descarga en cola al liberar un lugar",
                        icon = Icons.Default.Layers,
                        checked = settings.autoStartNext,
                        onCheckedChange = onAutoStartNextChange
                    )

                    SettingRadioGroupRow(
                        title = "Orden de la cola",
                        description = "Prioridad para procesar descargas en espera.",
                        currentLabel = settings.queueSortOrder.label,
                        options = QueueSortOrder.values().toList(),
                        selected = settings.queueSortOrder,
                        labelProvider = { it.label },
                        onSelect = onQueueSortOrderChange
                    )

                    SettingToggleRow(
                        title = "Reanudación automática",
                        subtitle = "Reintentar si la red se corta y continuar desde la última parte",
                        icon = Icons.Default.Refresh,
                        checked = settings.autoRetry,
                        onCheckedChange = onAutoRetryChange
                    )

                    if (settings.autoRetry) {
                        SettingOptionSelectorRow(
                            title = "Máximo de intentos",
                            description = "Intentos antes de marcar error definitivo.",
                            currentValue = "${settings.maxRetries} intentos",
                            options = listOf(1, 3, 5),
                            selectedOption = settings.maxRetries,
                            onOptionSelected = onMaxRetriesChange
                        )
                    }

                    SettingRadioGroupRow(
                        title = "Límite de velocidad",
                        description = "Controlar el ancho de banda usado por descarga.",
                        currentLabel = settings.speedLimit.label,
                        options = SpeedLimit.values().toList(),
                        selected = settings.speedLimit,
                        labelProvider = { it.label },
                        onSelect = onSpeedLimitChange
                    )
                }
            }

            // SECCIÓN: Descargas sin interrupciones
            item {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.height(4.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Descargas sin interrupciones",
                        fontFamily = DmSansFontFamily,
                        style = MaterialTheme.typography.labelLarge,
                        color = ElectricCyan,
                        fontWeight = FontWeight.Bold
                    )

                    Button(
                        onClick = onOpenWelcome,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 46.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(Icons.Default.BatteryChargingFull, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Verificar permisos y batería en segundo plano", fontFamily = DmSansFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }
            }

            // SECCIÓN: Tema visual
            item {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.height(4.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Tema visual",
                        fontFamily = DmSansFontFamily,
                        style = MaterialTheme.typography.labelLarge,
                        color = ElectricCyan,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeOptionButton(
                            title = "Sistema",
                            icon = Icons.Default.SettingsBrightness,
                            isSelected = settings.themeMode == ThemeMode.SYSTEM,
                            onClick = { onThemeChange(ThemeMode.SYSTEM) },
                            modifier = Modifier.weight(1f)
                        )
                        ThemeOptionButton(
                            title = "Claro",
                            icon = Icons.Default.LightMode,
                            isSelected = settings.themeMode == ThemeMode.LIGHT,
                            onClick = { onThemeChange(ThemeMode.LIGHT) },
                            modifier = Modifier.weight(1f)
                        )
                        ThemeOptionButton(
                            title = "Oscuro",
                            icon = Icons.Default.DarkMode,
                            isSelected = settings.themeMode == ThemeMode.DARK,
                            onClick = { onThemeChange(ThemeMode.DARK) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Spacer(modifier = Modifier.height(4.dp))
                        SettingToggleRow(
                            title = "Colores dinámicos Material You",
                            subtitle = "Adaptar paleta al fondo de pantalla",
                            icon = Icons.Default.ColorLens,
                            checked = settings.dynamicColor,
                            onCheckedChange = onDynamicColorChange
                        )
                    }
                }
            }

            // SECCIÓN: Avisos y Comportamiento
            item {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.height(4.dp))
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "Avisos y Comportamiento",
                        fontFamily = DmSansFontFamily,
                        style = MaterialTheme.typography.labelLarge,
                        color = ElectricCyan,
                        fontWeight = FontWeight.Bold
                    )

                    SettingToggleRow(
                        title = "Solo con Wi-Fi",
                        subtitle = "Descargar únicamente con conexión Wi-Fi",
                        icon = Icons.Default.Wifi,
                        checked = settings.wifiOnly,
                        onCheckedChange = onWifiOnlyChange
                    )

                    SettingToggleRow(
                        title = "Notificaciones de progreso",
                        subtitle = "Mostrar barra y porcentaje en la cortina del sistema",
                        icon = Icons.Default.Notifications,
                        checked = settings.showProgressNotifications,
                        onCheckedChange = onShowProgressNotifsChange
                    )

                    SettingToggleRow(
                        title = "Sonido al terminar",
                        subtitle = "Tono de notificación cuando el archivo esté listo",
                        icon = Icons.Default.VolumeUp,
                        checked = settings.soundOnComplete,
                        onCheckedChange = onSoundOnCompleteChange
                    )

                    SettingToggleRow(
                        title = "Vibración al terminar",
                        subtitle = "Respuesta háptica cuando el archivo esté listo",
                        icon = Icons.Default.Vibration,
                        checked = settings.vibrateOnComplete,
                        onCheckedChange = {
                            onVibrateChange(it)
                            if (it) triggerTestVibration(context)
                        }
                    )

                    SettingToggleRow(
                        title = "Limpiar campo al iniciar",
                        subtitle = "Vaciar automáticamente el código al pulsar descargar",
                        icon = Icons.Default.Clear,
                        checked = settings.autoClearOnStart,
                        onCheckedChange = onAutoClearChange
                    )
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    Text(
                        text = "Download Chunk • 2026 Pro Edition",
                        fontFamily = DmSansFontFamily,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

@Composable
fun SettingOptionSelectorRow(
    title: String,
    description: String,
    currentValue: String,
    options: List<Int>,
    selectedOption: Int,
    onOptionSelected: (Int) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, fontFamily = DmSansFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(text = currentValue, fontFamily = DmSansFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = ElectricCyan)
            }
            Text(text = description, fontFamily = DmSansFontFamily, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (opt in options) {
                    val isSel = opt == selectedOption
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSel) ElectricBlue else MaterialTheme.colorScheme.surface)
                            .clickable { onOptionSelected(opt) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = opt.toString(),
                            fontFamily = DmSansFontFamily,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp,
                            color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun <T> SettingRadioGroupRow(
    title: String,
    description: String,
    currentLabel: String,
    options: List<T>,
    selected: T,
    labelProvider: (T) -> String,
    onSelect: (T) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, fontFamily = DmSansFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(
                    text = currentLabel,
                    fontFamily = DmSansFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = ElectricCyan,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(text = description, fontFamily = DmSansFontFamily, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                for (opt in options) {
                    val isSel = opt == selected
                    val label = labelProvider(opt)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSel) ElectricBlue.copy(alpha = 0.12f) else Color.Transparent)
                            .clickable { onSelect(opt) }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = label,
                            fontFamily = DmSansFontFamily,
                            fontSize = 13.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSel) ElectricCyan else MaterialTheme.colorScheme.onSurface
                        )
                        if (isSel) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ThemeOptionButton(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (isSelected) ElectricBlue else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val fg = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = fg, modifier = Modifier.size(18.dp))
            Text(
                text = title,
                fontFamily = DmSansFontFamily,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = fg
            )
        }
    }
}

@Composable
fun SettingToggleRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 44.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(ElectricBlue.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(18.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontFamily = DmSansFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(text = subtitle, fontFamily = DmSansFontFamily, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = ElectricCyan,
                checkedTrackColor = ElectricBlue.copy(alpha = 0.4f)
            )
        )
    }
}

private fun getFileIconForExtension(filename: String): ImageVector {
    val ext = filename.substringAfterLast('.', "").lowercase()
    return when (ext) {
        "apk" -> Icons.Default.Android
        "zip", "rar", "7z", "tar", "gz" -> Icons.Default.FolderZip
        "mp4", "mkv", "avi", "mov", "webm" -> Icons.Default.VideoFile
        else -> Icons.AutoMirrored.Filled.InsertDriveFile
    }
}

private fun isReducedMotion(context: Context): Boolean {
    return try {
        val scale = android.provider.Settings.Global.getFloat(
            context.contentResolver,
            android.provider.Settings.Global.TRANSITION_ANIMATION_SCALE,
            1f
        )
        scale == 0f
    } catch (_: Exception) {
        false
    }
}

private fun triggerTestVibration(context: Context) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            v?.vibrate(120)
        }
    } catch (_: Exception) {}
}
