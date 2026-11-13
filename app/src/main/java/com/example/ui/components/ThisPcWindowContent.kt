package com.example.ui.components

import android.net.Uri
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.NoteAdd
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.VideoFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.SafFileExplorerRepository
import com.example.model.SafFileItem
import com.example.model.SafFolderRoot
import com.example.model.SystemTelemetry
import com.example.ui.AppStrings
import com.example.ui.theme.LocalWinPadChrome
import com.example.viewmodel.FileExplorerUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class MainDirectoryShortcut(
    val label: String,
    val primarySubfolder: String?,
    val icon: ImageVector
)

@Composable
fun ThisPcWindowContent(
    explorer: FileExplorerUiState,
    telemetry: SystemTelemetry,
    strings: AppStrings,
    quickInitialUriProvider: (String?) -> Uri,
    onMountFolder: (Uri) -> Unit,
    onUnmountFolder: (String) -> Unit,
    onSelectRoot: (SafFolderRoot) -> Unit,
    onOpenItem: (SafFileItem) -> Unit,
    onNavigateUp: () -> Unit,
    onRefresh: () -> Unit,
    onBreadcrumbClick: (Int) -> Unit,
    onCreateFolder: (String) -> Unit,
    onCreateFile: (String, String) -> Unit,
    onDeleteItem: (SafFileItem) -> Unit,
    onClosePreview: () -> Unit
) {
    val chrome = LocalWinPadChrome.current
    val context = LocalContext.current
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var showNewFileDialog by remember { mutableStateOf(false) }
    var filterQuery by remember { mutableStateOf("") }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) onMountFolder(uri)
    }

    val quickDirectories = remember(strings) {
        listOf(
            MainDirectoryShortcut(strings.deviceStorageLabel, null, Icons.Outlined.Storage),
            MainDirectoryShortcut(strings.downloadsFolder, "Download", Icons.Outlined.Download),
            MainDirectoryShortcut(strings.documentsFolder, "Documents", Icons.Outlined.Description),
            MainDirectoryShortcut(strings.picturesFolder, "Pictures", Icons.Outlined.Image),
            MainDirectoryShortcut(strings.dcimFolder, "DCIM", Icons.Outlined.PhotoCamera),
            MainDirectoryShortcut(strings.musicFolder, "Music", Icons.Outlined.MusicNote),
            MainDirectoryShortcut(strings.moviesFolder, "Movies", Icons.Outlined.Movie)
        )
    }

    val openMainDirectory: (String?) -> Unit = { subfolder ->
        filterQuery = ""
        val matchedRoot = explorer.mountedRoots.firstOrNull { root ->
            if (subfolder == null) {
                root.treeUriString == SafFileExplorerRepository.DEVICE_STORAGE_URI ||
                    root.treeUriString.endsWith("primary%3A") ||
                    root.treeUriString.endsWith("primary:")
            } else {
                root.treeUriString.contains("primary%3A$subfolder", ignoreCase = true) ||
                    root.treeUriString.endsWith("primary:$subfolder", ignoreCase = true)
            }
        }
        if (matchedRoot != null) {
            onSelectRoot(matchedRoot)
        } else {
            folderPickerLauncher.launch(quickInitialUriProvider(subfolder))
        }
    }

    val filteredItems = remember(explorer.items, filterQuery) {
        if (filterQuery.isBlank()) {
            explorer.items
        } else {
            explorer.items.filter { it.name.contains(filterQuery, ignoreCase = true) }
        }
    }

    val hasExternalStorageConnected = remember(explorer.mountedRoots) {
        explorer.mountedRoots.any { it.treeUriString != SafFileExplorerRepository.LOCAL_STORAGE_URI }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .testTag("built_in_file_manager"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Storage Card (Real StatFs)
        Surface(
            color = chrome.cardSurface,
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, chrome.glassBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Storage,
                            contentDescription = strings.storageSummary,
                            tint = chrome.accent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${strings.storageSummary} • ${telemetry.deviceModel}",
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                    val usedGb = (telemetry.totalStorageGb - telemetry.freeStorageGb).coerceAtLeast(0f)
                    Text(
                        text = String.format(
                            Locale.getDefault(),
                            "%.1f GB / %.1f GB",
                            usedGb,
                            telemetry.totalStorageGb
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                val progress = if (telemetry.totalStorageGb > 0f) {
                    ((telemetry.totalStorageGb - telemetry.freeStorageGb) / telemetry.totalStorageGb)
                        .coerceIn(0f, 1f)
                } else 0f

                LinearProgressIndicator(
                    progress = { progress },
                    color = chrome.accent,
                    trackColor = chrome.glassBorder,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
            }
        }

        // Toolbar: Up, Refresh, New Folder, New File, Mount Folder
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            IconButton(
                onClick = onNavigateUp,
                enabled = explorer.breadcrumbs.size > 1,
                modifier = Modifier
                    .size(38.dp)
                    .testTag("explorer_up_button")
            ) {
                Icon(
                    imageVector = Icons.Outlined.ArrowUpward,
                    contentDescription = strings.upFolder,
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(
                onClick = onRefresh,
                modifier = Modifier
                    .size(38.dp)
                    .testTag("explorer_refresh_button")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = strings.refresh,
                    modifier = Modifier.size(18.dp)
                )
            }

            if (explorer.activeRoot != null) {
                OutlinedButton(
                    onClick = { showNewFolderDialog = true },
                    modifier = Modifier.testTag("new_folder_button")
                ) {
                    Icon(Icons.Outlined.CreateNewFolder, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(strings.newFolder, style = MaterialTheme.typography.labelMedium)
                }

                OutlinedButton(
                    onClick = { showNewFileDialog = true },
                    modifier = Modifier.testTag("new_file_button")
                ) {
                    Icon(Icons.Outlined.NoteAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(strings.newFile, style = MaterialTheme.typography.labelMedium)
                }
            }

            Button(
                onClick = { folderPickerLauncher.launch(quickInitialUriProvider(null)) },
                modifier = Modifier.testTag("mount_folder_button")
            ) {
                Icon(Icons.Outlined.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(5.dp))
                Text(strings.mountFolder, style = MaterialTheme.typography.labelMedium)
            }
        }

        // 1-Tap Main Device Directories Quick Access Strip (Downloads, Documents, Pictures, DCIM, Music, Movies)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = strings.quickMainFolders,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                quickDirectories.forEach { dir ->
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(9.dp))
                            .clickable { openMainDirectory(dir.primarySubfolder) }
                            .testTag("quick_dir_${dir.primarySubfolder ?: "root"}"),
                        color = chrome.cardSurface.copy(alpha = 0.78f),
                        shape = RoundedCornerShape(9.dp),
                        border = androidx.compose.foundation.BorderStroke(0.8.dp, chrome.glassBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = dir.icon,
                                contentDescription = dir.label,
                                tint = chrome.accent,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = dir.label,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Mounted Roots Strip (Device Storage / Mounted SAF Folders / Local Storage)
        if (explorer.mountedRoots.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                explorer.mountedRoots.forEach { root ->
                    val isSelected = root.treeUriString == explorer.activeRoot?.treeUriString
                    val isVirtual = root.treeUriString == SafFileExplorerRepository.LOCAL_STORAGE_URI ||
                        root.treeUriString == SafFileExplorerRepository.DEVICE_STORAGE_URI
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                filterQuery = ""
                                onSelectRoot(root)
                            }
                            .testTag(if (isVirtual) "explorer_root_${root.treeUriString}" else "explorer_root_${root.displayName}"),
                        color = if (isSelected) chrome.accent.copy(alpha = 0.22f) else chrome.cardSurface,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) chrome.accent else chrome.glassBorder
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(
                                start = 10.dp,
                                end = if (isVirtual) 10.dp else 4.dp,
                                top = 5.dp,
                                bottom = 5.dp
                            ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isVirtual) Icons.Outlined.Storage else Icons.Outlined.Folder,
                                contentDescription = null,
                                tint = chrome.accent,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = root.displayName,
                                style = MaterialTheme.typography.labelMedium
                            )
                            if (!isVirtual) {
                                IconButton(
                                    onClick = { onUnmountFolder(root.treeUriString) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.DeleteOutline,
                                        contentDescription = strings.unmount,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (!hasExternalStorageConnected) {
            Surface(
                color = chrome.accent.copy(alpha = 0.10f),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(0.8.dp, chrome.accent.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FolderOpen,
                        contentDescription = null,
                        tint = chrome.accent,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = strings.externalAccessHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Address Breadcrumbs + Quick Search Filter
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                modifier = Modifier.weight(1f),
                color = chrome.cardSurface.copy(alpha = 0.65f),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(0.8.dp, chrome.glassBorder)
            ) {
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    explorer.breadcrumbs.forEachIndexed { index, (_, title) ->
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (index == explorer.breadcrumbs.lastIndex) FontWeight.Bold else FontWeight.Normal,
                            color = if (index == explorer.breadcrumbs.lastIndex) chrome.accent else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    filterQuery = ""
                                    onBreadcrumbClick(index)
                                }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                        if (index < explorer.breadcrumbs.lastIndex) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = filterQuery,
                onValueChange = { filterQuery = it },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall,
                placeholder = {
                    Text(
                        text = strings.searchFilesHint,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                trailingIcon = {
                    if (filterQuery.isNotEmpty()) {
                        IconButton(onClick = { filterQuery = "" }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Outlined.Close, contentDescription = strings.clearText, modifier = Modifier.size(15.dp))
                        }
                    }
                },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .width(190.dp)
                    .testTag("explorer_filter_input")
            )
        }

        HorizontalDivider(color = chrome.glassBorder)

        // File List Body
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when {
                explorer.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                filteredItems.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = strings.emptyFolderContent,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = { folderPickerLauncher.launch(quickInitialUriProvider("Download")) }
                        ) {
                            Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.downloadsFolder, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.testTag("explorer_file_list")
                    ) {
                        items(
                            items = filteredItems,
                            key = { "${it.uriString}_${it.documentId}_${it.name}" }
                        ) { item ->
                            val icon = when {
                                item.isDirectory -> Icons.Outlined.Folder
                                item.mimeType.startsWith("image/") -> Icons.Outlined.Image
                                item.mimeType.startsWith("video/") -> Icons.Outlined.VideoFile
                                item.mimeType.startsWith("audio/") -> Icons.Outlined.AudioFile
                                item.mimeType.startsWith("text/") || item.mimeType == "application/json" -> {
                                    Icons.Outlined.Description
                                }
                                else -> Icons.AutoMirrored.Outlined.InsertDriveFile
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(chrome.cardSurface.copy(alpha = 0.45f))
                                    .clickable {
                                        filterQuery = ""
                                        onOpenItem(item)
                                    }
                                    .padding(start = 12.dp, end = 6.dp, top = 7.dp, bottom = 7.dp)
                                    .testTag("explorer_item_${item.name}"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (item.isDirectory) chrome.accent else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val sizeLabel = if (item.isDirectory) {
                                        "Folder"
                                    } else {
                                        Formatter.formatShortFileSize(context, item.sizeBytes)
                                    }
                                    val dateLabel = if (item.lastModifiedMillis > 0L) {
                                        SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                                            .format(Date(item.lastModifiedMillis))
                                    } else ""
                                    Text(
                                        text = if (dateLabel.isNotEmpty()) "$sizeLabel  •  $dateLabel" else sizeLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = { onDeleteItem(item) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.DeleteOutline,
                                        contentDescription = strings.deleteItem,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showNewFolderDialog) {
        var folderName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewFolderDialog = false },
            title = { Text(strings.newFolder) },
            text = {
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    singleLine = true,
                    label = { Text(strings.folderNameHint) }
                )
            },
            dismissButton = {
                TextButton(onClick = { showNewFolderDialog = false }) {
                    Text(strings.cancel)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCreateFolder(folderName)
                        showNewFolderDialog = false
                    }
                ) {
                    Text(strings.create)
                }
            }
        )
    }

    if (showNewFileDialog) {
        var fileName by remember { mutableStateOf("") }
        var fileContent by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewFileDialog = false },
            title = { Text(strings.newFile) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = fileName,
                        onValueChange = { fileName = it },
                        singleLine = true,
                        label = { Text(strings.fileNameHint) }
                    )
                    OutlinedTextField(
                        value = fileContent,
                        onValueChange = { fileContent = it },
                        minLines = 3,
                        maxLines = 6,
                        label = { Text(strings.fileContentHint) }
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewFileDialog = false }) {
                    Text(strings.cancel)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCreateFile(fileName, fileContent)
                        showNewFileDialog = false
                    }
                ) {
                    Text(strings.create)
                }
            }
        )
    }

    if (explorer.previewFileTitle != null && explorer.previewFileContent != null) {
        AlertDialog(
            onDismissRequest = onClosePreview,
            title = { Text(explorer.previewFileTitle) },
            text = {
                Surface(
                    color = chrome.cardSurface,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp, max = 320.dp)
                ) {
                    Text(
                        text = explorer.previewFileContent.ifEmpty { "(Empty file)" },
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .padding(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(onClick = onClosePreview) {
                    Text(strings.close)
                }
            }
        )
    }
}
