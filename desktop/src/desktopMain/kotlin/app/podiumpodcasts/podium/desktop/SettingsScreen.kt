package app.podiumpodcasts.podium.desktop

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import app.podiumpodcasts.podium.data.AppDatabase
import app.podiumpodcasts.podium.manager.ExportManager
import app.podiumpodcasts.podium.manager.ImportManager
import app.podiumpodcasts.podium.manager.ImportResult
import app.podiumpodcasts.podium.utils.Logger
import app.podiumpodcasts.podium.utils.Settings
import app.podiumpodcasts.podium.utils.Strings
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
    onLanguageChanged: ((String) -> Unit)? = null
) {
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
    var showLanguageDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(Strings["settings_title"]) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = Strings["nav_back"])
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(Strings["settings_language"], style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            ListItem(
                headlineContent = { Text(Strings["settings_language"]) },
                supportingContent = {
                    Text(
                        text = if (currentLanguage == "zh") "简体中文" else "English",
                        style = MaterialTheme.typography.bodySmall
                    )
                },
                leadingContent = { Icon(Icons.Default.Language, null) },
                trailingContent = {
                    TextButton(onClick = { showLanguageDialog = true }) {
                        Text(Strings["settings_change"])
                    }
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text(Strings["settings_data"], style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            ListItem(
                headlineContent = { Text(Strings["settings_export_opml"]) },
                supportingContent = { Text(Strings["settings_export_opml_desc"]) },
                leadingContent = { Icon(Icons.Default.FileDownload, null) },
                trailingContent = {
                    TextButton(onClick = {
                        scope.launch {
                            exportedOpml = exportManager.exportOpml()
                            showExportDialog = true
                        }
                    }) {
                        Text(Strings["settings_export"])
                    }
                }
            )

            ListItem(
                headlineContent = { Text(Strings["settings_import_opml"]) },
                supportingContent = { Text(Strings["settings_import_opml_desc"]) },
                leadingContent = { Icon(Icons.Default.FileUpload, null) },
                trailingContent = {
                    TextButton(
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
                        },
                        enabled = !isImporting
                    ) {
                        if (isImporting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(Strings["settings_import"])
                        }
                    }
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text(Strings["settings_downloads"], style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            ListItem(
                headlineContent = { Text(Strings["settings_download_location"]) },
                supportingContent = {
                    Text(
                        text = downloadPath,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2
                    )
                },
                leadingContent = { Icon(Icons.Default.Folder, null) },
                trailingContent = {
                    TextButton(onClick = {
                        val dir = openDirectoryPicker("Select Download Folder")
                        if (dir != null) {
                            Settings.setDownloadPath(dir)
                            downloadPath = dir
                            onDownloadPathChanged?.invoke(dir)
                            Logger.i("Settings", "Download path changed to: $dir")
                        }
                    }) {
                        Text(Strings["settings_change"])
                    }
                }
            )

            if (downloadPath != Settings.getDownloadPath().let {
                    java.io.File(System.getProperty("user.home"), ".podium/downloads").absolutePath
                }) {
                TextButton(
                    onClick = {
                        Settings.resetDownloadPath()
                        downloadPath = Settings.getDownloadPath()
                        onDownloadPathChanged?.invoke(downloadPath)
                        Logger.i("Settings", "Download path reset to default")
                    },
                    modifier = Modifier.padding(start = 72.dp)
                ) {
                    Text(Strings["settings_reset_default"])
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text(Strings["settings_about"], style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Podium - Podcast Player", style = MaterialTheme.typography.bodyMedium)
            Text(Strings.get("settings_version", "0.1.0"), style = MaterialTheme.typography.bodySmall)
        }
    }

    if (showLanguageDialog) {
        val languages = listOf("en" to "English", "zh" to "简体中文")
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(Strings["settings_language"]) },
            text = {
                Column {
                    languages.forEach { (code, name) ->
                        ListItem(
                            headlineContent = { Text(name) },
                            leadingContent = {
                                RadioButton(
                                    selected = currentLanguage == code,
                                    onClick = {
                                        currentLanguage = code
                                        Settings.setLanguage(code)
                                        Strings.updateLanguage(code)
                                        showLanguageDialog = false
                                        onLanguageChanged?.invoke(code)
                                    }
                                )
                            },
                            modifier = Modifier.clickable {
                                currentLanguage = code
                                Settings.setLanguage(code)
                                Strings.updateLanguage(code)
                                showLanguageDialog = false
                                onLanguageChanged?.invoke(code)
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(Strings["dialog_cancel"])
                }
            }
        )
    }

    if (showExportDialog && exportedOpml != null) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text(Strings["opml_export_title"]) },
            text = {
                Column {
                    Text(Strings["opml_export_copy_hint"])
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = exportedOpml!!,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 200.dp, max = 400.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    clipboardManager.setText(AnnotatedString(exportedOpml!!))
                    showExportDialog = false
                    showCopiedSnackbar = true
                }) {
                    Text(Strings["dialog_copy_to_clipboard"])
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text(Strings["dialog_close"])
                }
            }
        )
    }

    if (showCopiedSnackbar) {
        Snackbar(
            modifier = Modifier.padding(16.dp),
            action = {
                TextButton(onClick = { showCopiedSnackbar = false }) {
                    Text(Strings["dialog_ok"])
                }
            }
        ) {
            Text(Strings["opml_export_copied"])
        }
    }

    showImportResult?.let { result ->
        when (result) {
            is ImportResult.Success -> {
                AlertDialog(
                    onDismissRequest = { showImportResult = null },
                    title = { Text(Strings["opml_import_title"]) },
                    text = {
                        Column {
                            Text(Strings.get("opml_import_added", result.added))
                            Text(Strings.get("opml_import_skipped", result.skipped))
                            if (result.failed > 0) {
                                Text(
                                    Strings.get("opml_import_failed", result.failed),
                                    color = MaterialTheme.colorScheme.error
                                )
                                result.errors.forEach { error ->
                                    Text(
                                        text = "  - $error",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showImportResult = null }) {
                            Text(Strings["dialog_ok"])
                        }
                    }
                )
            }
            is ImportResult.Error -> {
                AlertDialog(
                    onDismissRequest = { showImportResult = null },
                    title = { Text(Strings["opml_import_error"]) },
                    text = { Text(result.message) },
                    confirmButton = {
                        TextButton(onClick = { showImportResult = null }) {
                            Text(Strings["dialog_ok"])
                        }
                    }
                )
            }
        }
    }
}

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
