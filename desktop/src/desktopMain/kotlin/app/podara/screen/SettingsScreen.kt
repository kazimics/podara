package app.podara.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.podara.component.PodaraDropdownMenu
import app.podara.component.PodaraDropdownMenuItem
import app.podara.component.ToolbarPillButton
import app.podara.data.AppDatabase
import app.podara.manager.ExportManager
import app.podara.manager.ImportManager
import app.podara.manager.ImportResult
import app.podara.theme.DesignTokens
import app.podara.theme.PodaraTheme
import app.podara.theme.PodaraColors
import app.podara.util.Logger
import app.podara.util.Settings
import app.podara.util.Strings
import kotlinx.coroutines.launch
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    database: AppDatabase,
    onBack: () -> Unit,
    onDownloadPathChanged: ((String) -> Unit)? = null,
    onLanguageChanged: ((String) -> Unit)? = null,
    downloadSpeedLimitKbps: Int = 0,
    onDownloadSpeedLimitChanged: ((Int) -> Unit)? = null
) {
    val colors = PodaraTheme.colors
    val exportManager = remember { ExportManager(database) }
    val importManager = remember { ImportManager(database) }
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    var showExportDialog by remember { mutableStateOf(false) }
    var exportedOpml by remember { mutableStateOf<String?>(null) }
    var showCopiedSnackbar by remember { mutableStateOf(false) }
    var showImportResult by remember { mutableStateOf<ImportResult?>(null) }
    var isImporting by remember { mutableStateOf(false) }
    var downloadPath by remember { mutableStateOf(Settings.getDownloadPath()) }
    var currentLanguage by remember { mutableStateOf(Settings.getLanguage()) }
    var selectedLanguage by remember { mutableStateOf(currentLanguage) }
    var showLanguageMenu by remember { mutableStateOf(false) }
    var localSpeedLimitInput by remember { mutableStateOf("") }
    var showSpeedLimitDialog by remember { mutableStateOf(false) }
    var speedLimitError by remember { mutableStateOf<String?>(null) }
    // ── Close behavior dialog state ──
    var showCloseBehaviorDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(top = 28.dp, start = 32.dp, end = 32.dp, bottom = 8.dp)
    ) {
        Spacer(Modifier.height(4.dp))

        // ── Page header ──
        Text(
            text = Strings["settings_title"],
            fontSize = DesignTokens.PageHeader.TitleSize,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif,
            color = colors.textPrimary
        )

        Spacer(Modifier.height(24.dp))

        // ── Scrollable settings content ──
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // ── Language section ──
            SettingsSection(title = Strings["settings_language"], colors = colors) {
                SettingsRow(
                    icon = Icons.Default.Language,
                    title = Strings["settings_language"],
                    subtitle = if (currentLanguage == "zh") "简体中文" else "English",
                    action = {
                        LanguageSelector(
                            selectedLanguage = selectedLanguage,
                            expanded = showLanguageMenu,
                            onExpandedChange = { showLanguageMenu = it },
                            onLanguageSelected = { selectedLanguage = it },
                            onChange = {
                                if (currentLanguage != selectedLanguage) {
                                    currentLanguage = selectedLanguage
                                    Settings.setLanguage(selectedLanguage)
                                    Strings.updateLanguage(selectedLanguage)
                                    onLanguageChanged?.invoke(selectedLanguage)
                                }
                            }
                        )
                    },
                    colors = colors
                )
            }

            Spacer(Modifier.height(DesignTokens.Spacing.sm))

            // ── Data section ──
            SettingsSection(title = Strings["settings_data"], colors = colors) {
                SettingsRow(
                    icon = Icons.Default.FileDownload,
                    title = Strings["settings_export_opml"],
                    subtitle = Strings["settings_export_opml_desc"],
                    action = {
                        SettingsActionButton(
                            text = Strings["settings_export"],
                            color = colors.accent,
                            onClick = {
                                scope.launch {
                                    exportedOpml = exportManager.exportOpml()
                                    showExportDialog = true
                                }
                            }
                        )
                    },
                    colors = colors
                )

                SettingsRow(
                    icon = Icons.Default.FileUpload,
                    title = Strings["settings_import_opml"],
                    subtitle = Strings["settings_import_opml_desc"],
                    action = {
                        if (isImporting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = colors.accent
                            )
                        } else {
                            SettingsActionButton(
                                text = Strings["settings_import"],
                                color = colors.accent,
                                onClick = {
                                    scope.launch {
                                        isImporting = true
                                        try {
                                            val file = openFilePicker("Select OPML File", "opml", "xml")
                                            if (file != null) {
                                                val content = file.readText()
                                                val result = importManager.importOpml(content)
                                                showImportResult = result
                                            }
                                        } finally {
                                            isImporting = false
                                        }
                                    }
                                }
                            )
                        }
                    },
                    colors = colors
                )
            }

            Spacer(Modifier.height(DesignTokens.Spacing.sm))

            // ── Downloads section ──
            SettingsSection(title = Strings["settings_downloads"], colors = colors) {
                SettingsRow(
                    icon = Icons.Default.Folder,
                    title = Strings["settings_download_location"],
                    subtitle = downloadPath,
                    action = {
                        SettingsActionButton(
                            text = Strings["settings_change"],
                            color = colors.accent,
                            onClick = {
                                val dir = openDirectoryPicker("Select Download Folder")
                                if (dir != null) {
                                    Settings.setDownloadPath(dir)
                                    downloadPath = dir
                                    onDownloadPathChanged?.invoke(dir)
                                    Logger.i("Settings", "Download path changed to: $dir")
                                }
                            }
                        )
                    },
                    colors = colors
                )

                if (downloadPath != java.io.File(System.getProperty("user.home"), ".podara/downloads").absolutePath) {
                    SettingsActionText(
                        text = Strings["settings_reset_default"],
                        color = colors.textSecondary,
                        onClick = {
                            Settings.resetDownloadPath()
                            downloadPath = Settings.getDownloadPath()
                            onDownloadPathChanged?.invoke(downloadPath)
                            Logger.i("Settings", "Download path reset to default")
                        },
                        modifier = Modifier.padding(start = 64.dp)
                    )
                }

                Spacer(Modifier.height(DesignTokens.Spacing.xs))

                SettingsRow(
                    icon = Icons.Default.Star,
                    title = Strings["settings_download_speed_limit"],
                    subtitle = if (downloadSpeedLimitKbps <= 0) {
                        Strings["settings_download_speed_unlimited"]
                    } else {
                        "$downloadSpeedLimitKbps ${Strings["settings_download_speed_unit"]}"
                    },
                    action = {
                        SettingsActionButton(
                            text = Strings["settings_change"],
                            color = colors.accent,
                            onClick = {
                                localSpeedLimitInput = downloadSpeedLimitKbps.toString()
                                speedLimitError = null
                                showSpeedLimitDialog = true
                            }
                        )
                    },
                    colors = colors
                )
            }

            Spacer(Modifier.height(DesignTokens.Spacing.sm))

            // ── Close Behavior section ──
            SettingsSection(title = Strings["settings_close_behavior"], colors = colors) {
                SettingsRow(
                    icon = Icons.Default.ExitToApp,
                    title = Strings["settings_close_behavior"],
                    subtitle = when (Settings.getCloseAction()) {
                        "quit" -> Strings["settings_close_quit"]
                        "minimize_to_tray" -> Strings["settings_close_minimize_tray"]
                        else -> Strings["settings_close_ask"]
                    },
                    action = {
                        SettingsActionButton(
                            text = Strings["settings_change"],
                            color = colors.accent,
                            onClick = { showCloseBehaviorDialog = true }
                        )
                    },
                    colors = colors
                )
            }

            Spacer(Modifier.height(DesignTokens.Spacing.sm))

            // ── About section ──
            SettingsSection(title = Strings["settings_about"], colors = colors) {
                Text(
                    text = Strings["settings_about_desc"],
                    fontSize = 14.sp,
                    color = colors.textSecondary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = Strings.get("settings_version", "0.1.0"),
                    fontSize = 12.sp,
                    color = colors.textMuted
                )
            }
        }
    }

    // ── Download speed limit dialog ──
    if (showSpeedLimitDialog) {
        AlertDialog(
            onDismissRequest = { showSpeedLimitDialog = false; speedLimitError = null },
            shape = RoundedCornerShape(0),
            modifier = Modifier.widthIn(max = 380.dp),
            containerColor = colors.surface,
            title = { Text(Strings["settings_download_speed_limit"], color = colors.textPrimary) },
            text = {
                Column {
                    Text(
                        text = Strings["settings_download_speed_hint"],
                        fontSize = 13.sp,
                        color = colors.textSecondary
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = localSpeedLimitInput,
                        onValueChange = { localSpeedLimitInput = it; speedLimitError = null },
                        label = { Text(Strings["settings_download_speed_unit"]) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = speedLimitError != null,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.accent,
                            unfocusedBorderColor = colors.border,
                            cursorColor = colors.accent,
                            focusedLabelColor = colors.accent,
                            unfocusedLabelColor = colors.textSecondary,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary,
                            errorTextColor = colors.danger,
                            errorBorderColor = colors.danger,
                            errorLabelColor = colors.danger
                        )
                    )
                    if (speedLimitError != null) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = speedLimitError!!,
                            color = colors.danger,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val trimmed = localSpeedLimitInput.trim()
                    val parsed = trimmed.toIntOrNull()
                    if (parsed == null || parsed < 0 || trimmed != parsed.toString()) {
                        speedLimitError = Strings["settings_download_speed_error"]
                    } else {
                        Settings.setDownloadSpeedLimitKbps(parsed)
                        onDownloadSpeedLimitChanged?.invoke(parsed)
                        showSpeedLimitDialog = false
                        speedLimitError = null
                    }
                }) {
                    Text(Strings["dialog_ok"], color = colors.accent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSpeedLimitDialog = false; speedLimitError = null }) {
                    Text(Strings["dialog_cancel"], color = colors.textSecondary)
                }
            }
        )
    }

    // ── Close behavior dialog ──
    if (showCloseBehaviorDialog) {
        var selectedAction by remember { mutableStateOf(Settings.getCloseAction()) }

        AlertDialog(
            onDismissRequest = { showCloseBehaviorDialog = false },
            shape = RoundedCornerShape(0),
            modifier = Modifier.widthIn(max = 380.dp),
            containerColor = colors.surface,
            title = { Text(Strings["settings_close_behavior"], color = colors.textPrimary) },
            text = {
                Column {
                    Text(
                        text = Strings["settings_close_behavior_desc"],
                        fontSize = 13.sp,
                        color = colors.textSecondary
                    )
                    Spacer(Modifier.height(8.dp))

                    // Ask every time
                    val askInteractionSource = remember { MutableInteractionSource() }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .pointerHoverIcon(PointerIcon(java.awt.Cursor(java.awt.Cursor.HAND_CURSOR)))
                            .clickable(interactionSource = askInteractionSource, indication = null) {
                                selectedAction = "ask"
                            }
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedAction == "ask",
                            onClick = { selectedAction = "ask" },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = colors.accent,
                                unselectedColor = colors.textSecondary
                            )
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = Strings["settings_close_ask"],
                            fontSize = 14.sp,
                            color = colors.textPrimary
                        )
                    }

                    // Quit
                    val quitInteractionSource = remember { MutableInteractionSource() }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .pointerHoverIcon(PointerIcon(java.awt.Cursor(java.awt.Cursor.HAND_CURSOR)))
                            .clickable(interactionSource = quitInteractionSource, indication = null) {
                                selectedAction = "quit"
                            }
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedAction == "quit",
                            onClick = { selectedAction = "quit" },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = colors.accent,
                                unselectedColor = colors.textSecondary
                            )
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = Strings["settings_close_quit"],
                            fontSize = 14.sp,
                            color = colors.textPrimary
                        )
                    }

                    // Minimize to tray
                    val minimizeInteractionSource = remember { MutableInteractionSource() }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .pointerHoverIcon(PointerIcon(java.awt.Cursor(java.awt.Cursor.HAND_CURSOR)))
                            .clickable(interactionSource = minimizeInteractionSource, indication = null) {
                                selectedAction = "minimize_to_tray"
                            }
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedAction == "minimize_to_tray",
                            onClick = { selectedAction = "minimize_to_tray" },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = colors.accent,
                                unselectedColor = colors.textSecondary
                            )
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = Strings["settings_close_minimize_tray"],
                            fontSize = 14.sp,
                            color = colors.textPrimary
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    Settings.setCloseAction(selectedAction)
                    showCloseBehaviorDialog = false
                }) {
                    Text(Strings["dialog_ok"], color = colors.accent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCloseBehaviorDialog = false }) {
                    Text(Strings["dialog_cancel"], color = colors.textSecondary)
                }
            }
        )
    }

    // ── Export dialog ──
    if (showExportDialog && exportedOpml != null) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            shape = RoundedCornerShape(0),
            modifier = Modifier.widthIn(max = 380.dp),
            containerColor = colors.surface,
            title = { Text(Strings["opml_export_title"], color = colors.textPrimary) },
            text = {
                Column {
                    Text(
                        text = Strings["opml_export_copy_hint"],
                        fontSize = 13.sp,
                        color = colors.textSecondary
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = exportedOpml!!,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 200.dp, max = 400.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.border,
                            unfocusedBorderColor = colors.border,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    clipboardManager.setText(AnnotatedString(exportedOpml!!))
                    showExportDialog = false
                    showCopiedSnackbar = true
                }) {
                    Text(Strings["dialog_copy_to_clipboard"], color = colors.accent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text(Strings["dialog_close"], color = colors.textSecondary)
                }
            }
        )
    }

    if (showCopiedSnackbar) {
        Snackbar(
            modifier = Modifier.padding(16.dp),
            containerColor = colors.surface,
            contentColor = colors.textPrimary,
            action = {
                TextButton(onClick = { showCopiedSnackbar = false }) {
                    Text(Strings["dialog_ok"], color = colors.accent)
                }
            }
        ) {
            Text(Strings["opml_export_copied"])
        }
    }

    // ── Import result dialog ──
    showImportResult?.let { result ->
        when (result) {
            is ImportResult.Success -> {
                AlertDialog(
                    onDismissRequest = { showImportResult = null },
                    shape = RoundedCornerShape(0),
                    modifier = Modifier.widthIn(max = 380.dp),
                    containerColor = colors.surface,
                    title = { Text(Strings["opml_import_title"], color = colors.textPrimary) },
                    text = {
                        Column {
                            Text(
                                text = Strings.get("opml_import_added", result.added),
                                color = colors.textSecondary,
                                fontSize = 14.sp
                            )
                            Text(
                                text = Strings.get("opml_import_skipped", result.skipped),
                                color = colors.textSecondary,
                                fontSize = 14.sp
                            )
                            if (result.failed > 0) {
                                Text(
                                    text = Strings.get("opml_import_failed", result.failed),
                                    color = colors.danger,
                                    fontSize = 14.sp
                                )
                                result.errors.forEach { error ->
                                    Text(
                                        text = "  - $error",
                                        color = colors.danger,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showImportResult = null }) {
                            Text(Strings["dialog_ok"], color = colors.accent)
                        }
                    }
                )
            }
            is ImportResult.Error -> {
                AlertDialog(
                    onDismissRequest = { showImportResult = null },
                    containerColor = colors.surface,
                    title = { Text(Strings["opml_import_error"], color = colors.textPrimary) },
                    text = {
                        Text(
                            text = result.message,
                            color = colors.textSecondary,
                            fontSize = 14.sp
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = { showImportResult = null }) {
                            Text(Strings["dialog_ok"], color = colors.accent)
                        }
                    }
                )
            }
        }
    }
}

// ── Reusable card section ──
@Composable
private fun SettingsSection(
    title: String,
    colors: PodaraColors,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(12.dp)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = colors.surface)
    ) {
        Column(modifier = Modifier.padding(horizontal = DesignTokens.Spacing.lg, vertical = 18.dp)) {
            SectionHeader(title)
            Spacer(Modifier.height(DesignTokens.Spacing.sm))
            content()
        }
    }
}

// ── Reusable section header ──
@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = DesignTokens.SectionHeader.TitleSize,
        fontWeight = FontWeight.SemiBold,
        fontFamily = FontFamily.Serif,
        color = PodaraTheme.colors.textPrimary
    )
}

// ── Language selection controls ──
@Composable
private fun LanguageSelector(
    selectedLanguage: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onLanguageSelected: (String) -> Unit,
    onChange: () -> Unit
) {
    val colors = PodaraTheme.colors
    val toolbarButton = DesignTokens.ToolbarButton
    val selectedLabel = if (selectedLanguage == "zh") "简体中文" else "English"

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box {
            val interactionSource = remember { MutableInteractionSource() }
            val shape = RoundedCornerShape(toolbarButton.PillRadius)
            Box(
                modifier = Modifier
                    .height(32.dp)
                    .widthIn(min = 108.dp)
                    .clip(shape)
                    .background(if (expanded) toolbarButton.PillSelectedBackgroundColor else toolbarButton.PillSortBackgroundColor)
                    .pointerHoverIcon(PointerIcon(java.awt.Cursor(java.awt.Cursor.HAND_CURSOR)))
                    .clickable(interactionSource = interactionSource, indication = null) { onExpandedChange(true) }
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(toolbarButton.PillIconTextGap)
                ) {
                    Text(
                        text = selectedLabel,
                        color = if (expanded) toolbarButton.PillSelectedTextColor else colors.textPrimary,
                        fontSize = toolbarButton.PillTextSize,
                        lineHeight = toolbarButton.PillLineHeight,
                        fontWeight = if (expanded) toolbarButton.PillActiveTextWeight else toolbarButton.PillTextWeight
                    )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = Strings["settings_language"],
                        tint = if (expanded) toolbarButton.PillSelectedIconColor else toolbarButton.PillIconColor,
                        modifier = Modifier.size(toolbarButton.PillTrailingIconSize)
                    )
                }
            }

            PodaraDropdownMenu(
                expanded = expanded,
                onDismissRequest = { onExpandedChange(false) },
                items = listOf(
                    PodaraDropdownMenuItem(
                        label = "English",
                        isSelected = selectedLanguage == "en",
                        onClick = { onLanguageSelected("en"); onExpandedChange(false) }
                    ),
                    PodaraDropdownMenuItem(
                        label = "简体中文",
                        isSelected = selectedLanguage == "zh",
                        onClick = { onLanguageSelected("zh"); onExpandedChange(false) }
                    )
                )
            )
        }
        SettingsActionButton(
            text = Strings["settings_change"],
            color = colors.accent,
            onClick = onChange
        )
    }
}

// ── Settings action button ──
@Composable
private fun SettingsActionButton(
    text: String,
    color: Color,
    onClick: () -> Unit
) {
    val toolbarButton = DesignTokens.ToolbarButton

    ToolbarPillButton(
        label = text,
        contentDescription = text,
        onClick = onClick,
        height = 32.dp,
        radius = 8.dp,
        minWidth = 0.dp,
        horizontalPadding = 12.dp,
        textColor = color,
        hoverTextColor = color,
        defaultBackgroundColor = toolbarButton.PillSortBackgroundColor,
        hoverBackgroundColor = toolbarButton.PillSortBackgroundColor,
        pressedBackgroundColor = toolbarButton.PillSelectedBackgroundColor,
        defaultBorderColor = Color.Transparent,
        hoverBorderColor = Color.Transparent
    )
}

// ── Text button without MD hover background ──
@Composable
private fun SettingsActionText(
    text: String,
    color: Color,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Text(
        text = text,
        color = if (enabled) color else color.copy(alpha = 0.38f),
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        modifier = modifier
            .pointerHoverIcon(PointerIcon(java.awt.Cursor(java.awt.Cursor.HAND_CURSOR)))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
    )
}

// ── Reusable settings row ──
@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    action: @Composable () -> Unit,
    colors: PodaraColors
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val rowBg by animateColorAsState(if (isHovered) colors.elevated else Color.Transparent, tween(150))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(rowBg)
            .pointerHoverIcon(PointerIcon(java.awt.Cursor(java.awt.Cursor.HAND_CURSOR)))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { }
            )
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.textSecondary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = colors.textMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        action()
    }
}

// ── Platform helpers ──

private fun openFilePicker(title: String, vararg extensions: String): File? {
    val frame = Frame()
    val dialog = FileDialog(frame, title, FileDialog.LOAD)
    dialog.file = "*.${extensions.first()}"
    dialog.isVisible = true

    val fileName = dialog.file
    val dir = dialog.directory
    dialog.dispose()
    frame.dispose()

    return if (fileName != null && dir != null) {
        File(dir, fileName)
    } else {
        null
    }
}

private fun openDirectoryPicker(title: String): String? {
    val chooser = javax.swing.JFileChooser()
    chooser.dialogTitle = title
    chooser.fileSelectionMode = javax.swing.JFileChooser.DIRECTORIES_ONLY
    chooser.isAcceptAllFileFilterUsed = false

    val result = chooser.showOpenDialog(null)
    return if (result == javax.swing.JFileChooser.APPROVE_OPTION) {
        chooser.selectedFile.absolutePath
    } else {
        null
    }
}
