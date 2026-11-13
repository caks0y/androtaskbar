package com.example.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import com.example.model.SafFileItem
import com.example.model.SafFolderRoot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class SafFileExplorerRepository(private val context: Context) {

    companion object {
        const val LOCAL_STORAGE_URI = "local://storage"
        const val DEVICE_STORAGE_URI = "device://primary"
        private const val LOCAL_SCHEME_PREFIX = "localfile://"
        private const val DEVICE_SCHEME_PREFIX = "devicefile://"
        private const val EXTERNAL_STORAGE_AUTHORITY = "com.android.externalstorage.documents"
    }

    private fun localRootDir(): File {
        val root = File(context.filesDir, "local_storage")
        if (!root.exists()) {
            root.mkdirs()
            val docs = File(root, "Documents").apply { mkdirs() }
            File(root, "Downloads").mkdirs()
            File(root, "Pictures").mkdirs()
            File(root, "Projects").mkdirs()
            File(docs, "Welcome.txt").writeText(
                "Windroid Local Storage\n" +
                    "----------------------\n" +
                    "Use this built-in File Explorer to browse local folders, create files/folders, " +
                    "or connect tablet main directories (Downloads, Documents, Pictures, DCIM) below."
            )
        }
        return root
    }

    @Suppress("DEPRECATION")
    private fun devicePrimaryRootDir(): File? {
        return runCatching {
            val external = Environment.getExternalStorageDirectory()
            if (external != null && external.exists() && external.isDirectory) external else null
        }.getOrNull()
    }

    fun isExternalPrimaryReadable(): Boolean {
        val dir = devicePrimaryRootDir() ?: return false
        return runCatching {
            dir.canRead() && !dir.listFiles().isNullOrEmpty()
        }.getOrDefault(false)
    }

    fun quickSafInitialUri(primarySubfolder: String?): Uri {
        val docId = if (primarySubfolder.isNullOrBlank()) "primary:" else "primary:$primarySubfolder"
        return DocumentsContract.buildDocumentUri(EXTERNAL_STORAGE_AUTHORITY, docId)
    }

    fun isLocalRoot(treeUriString: String): Boolean = treeUriString == LOCAL_STORAGE_URI

    fun isDeviceRoot(treeUriString: String): Boolean = treeUriString == DEVICE_STORAGE_URI

    fun isVirtualFileRoot(treeUriString: String): Boolean =
        isLocalRoot(treeUriString) || isDeviceRoot(treeUriString)

    fun localRootDisplay(label: String): SafFolderRoot {
        localRootDir()
        return SafFolderRoot(treeUriString = LOCAL_STORAGE_URI, displayName = label)
    }

    fun deviceRootDisplay(label: String): SafFolderRoot =
        SafFolderRoot(treeUriString = DEVICE_STORAGE_URI, displayName = label)

    fun persistTreePermission(treeUri: Uri) {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        runCatching {
            context.contentResolver.takePersistableUriPermission(treeUri, flags)
        }.onFailure {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    treeUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
        }
    }

    fun resolveRootDisplay(
        treeUriString: String,
        localLabel: String = "Local Storage",
        deviceLabel: String = "Internal Storage"
    ): SafFolderRoot {
        if (isLocalRoot(treeUriString)) {
            return localRootDisplay(localLabel)
        }
        if (isDeviceRoot(treeUriString)) {
            return deviceRootDisplay(deviceLabel)
        }
        val uri = Uri.parse(treeUriString)
        val docId = runCatching { DocumentsContract.getTreeDocumentId(uri) }
            .getOrElse { uri.lastPathSegment.orEmpty() }
        val docUri = runCatching { DocumentsContract.buildDocumentUriUsingTree(uri, docId) }
            .getOrNull()
        val name = (if (docUri != null) queryDisplayName(docUri) else null)
            ?: docId.substringAfter(':').ifBlank { "Mounted Folder" }
        return SafFolderRoot(treeUriString = treeUriString, displayName = name)
    }

    suspend fun listChildren(treeUriString: String, parentDocumentId: String? = null): List<SafFileItem> =
        withContext(Dispatchers.IO) {
            if (isLocalRoot(treeUriString)) {
                return@withContext listDirectoryChildren(
                    root = localRootDir(),
                    relativePath = parentDocumentId,
                    schemePrefix = LOCAL_SCHEME_PREFIX
                )
            }
            if (isDeviceRoot(treeUriString)) {
                val external = devicePrimaryRootDir() ?: return@withContext emptyList()
                return@withContext listDirectoryChildren(
                    root = external,
                    relativePath = parentDocumentId,
                    schemePrefix = DEVICE_SCHEME_PREFIX
                )
            }

            val treeUri = Uri.parse(treeUriString)
            val docId = parentDocumentId ?: runCatching {
                DocumentsContract.getTreeDocumentId(treeUri)
            }.getOrElse {
                DocumentsContract.getDocumentId(treeUri)
            }
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)

            val projection = arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_SIZE,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED
            )

            val results = mutableListOf<SafFileItem>()
            runCatching {
                context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                    val idIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                    val nameIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                    val mimeIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                    val sizeIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
                    val modIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)

                    while (cursor.moveToNext()) {
                        val childId = if (idIdx >= 0 && !cursor.isNull(idIdx)) {
                            cursor.getString(idIdx)
                        } else {
                            continue
                        }
                        val name = if (nameIdx >= 0 && !cursor.isNull(nameIdx)) {
                            cursor.getString(nameIdx)
                        } else {
                            childId.substringAfterLast('/')
                        }
                        if (name.isBlank()) continue
                        val mime = if (mimeIdx >= 0 && !cursor.isNull(mimeIdx)) {
                            cursor.getString(mimeIdx)
                        } else {
                            "application/octet-stream"
                        }
                        val size = if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) {
                            cursor.getLong(sizeIdx)
                        } else {
                            0L
                        }
                        val modified = if (modIdx >= 0 && !cursor.isNull(modIdx)) {
                            cursor.getLong(modIdx)
                        } else {
                            0L
                        }
                        val isDir = mime == DocumentsContract.Document.MIME_TYPE_DIR
                        val childUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, childId)

                        results.add(
                            SafFileItem(
                                uriString = childUri.toString(),
                                documentId = childId,
                                name = name,
                                mimeType = mime,
                                sizeBytes = size,
                                lastModifiedMillis = modified,
                                isDirectory = isDir
                            )
                        )
                    }
                }
            }

            results.sortedWith(
                compareByDescending<SafFileItem> { it.isDirectory }
                    .thenBy { it.name.lowercase() }
            )
        }

    private fun listDirectoryChildren(
        root: File,
        relativePath: String?,
        schemePrefix: String
    ): List<SafFileItem> {
        val target = resolveSafeLocalFile(root, relativePath) ?: root
        val children = runCatching { target.listFiles()?.toList().orEmpty() }.getOrDefault(emptyList())
        return children
            .filterNot { it.name.startsWith(".") }
            .map { file ->
                val rel = file.relativeTo(root).path
                val isDir = file.isDirectory
                SafFileItem(
                    uriString = "$schemePrefix$rel",
                    documentId = rel,
                    name = file.name,
                    mimeType = if (isDir) DocumentsContract.Document.MIME_TYPE_DIR else guessMimeType(file.name),
                    sizeBytes = if (isDir) 0L else file.length(),
                    lastModifiedMillis = file.lastModified(),
                    isDirectory = isDir
                )
            }.sortedWith(
                compareByDescending<SafFileItem> { it.isDirectory }
                    .thenBy { it.name.lowercase() }
            )
    }

    suspend fun createFolder(
        treeUriString: String,
        parentDocumentId: String?,
        folderName: String
    ): Boolean = withContext(Dispatchers.IO) {
        val cleanName = folderName.trim().replace("/", "_").replace("..", "")
        if (cleanName.isBlank()) return@withContext false

        if (isLocalRoot(treeUriString) || isDeviceRoot(treeUriString)) {
            val root = (if (isLocalRoot(treeUriString)) localRootDir() else devicePrimaryRootDir())
                ?: return@withContext false
            val parent = resolveSafeLocalFile(root, parentDocumentId) ?: return@withContext false
            val target = File(parent, cleanName)
            return@withContext runCatching {
                if (target.exists()) false else target.mkdirs()
            }.getOrDefault(false)
        }

        val treeUri = Uri.parse(treeUriString)
        val docId = parentDocumentId ?: DocumentsContract.getTreeDocumentId(treeUri)
        val parentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
        runCatching {
            DocumentsContract.createDocument(
                context.contentResolver,
                parentUri,
                DocumentsContract.Document.MIME_TYPE_DIR,
                cleanName
            ) != null
        }.getOrDefault(false)
    }

    suspend fun createTextFile(
        treeUriString: String,
        parentDocumentId: String?,
        fileName: String,
        content: String
    ): Boolean = withContext(Dispatchers.IO) {
        val raw = fileName.trim().replace("/", "_").replace("..", "")
        if (raw.isBlank()) return@withContext false
        val cleanName = if (raw.contains('.')) raw else "$raw.txt"

        if (isLocalRoot(treeUriString) || isDeviceRoot(treeUriString)) {
            val root = (if (isLocalRoot(treeUriString)) localRootDir() else devicePrimaryRootDir())
                ?: return@withContext false
            val parent = resolveSafeLocalFile(root, parentDocumentId) ?: return@withContext false
            val target = File(parent, cleanName)
            return@withContext runCatching {
                target.writeText(content)
                true
            }.getOrDefault(false)
        }

        val treeUri = Uri.parse(treeUriString)
        val docId = parentDocumentId ?: DocumentsContract.getTreeDocumentId(treeUri)
        val parentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
        runCatching {
            val createdUri = DocumentsContract.createDocument(
                context.contentResolver,
                parentUri,
                "text/plain",
                cleanName
            ) ?: return@withContext false
            context.contentResolver.openOutputStream(createdUri)?.use { out ->
                out.write(content.toByteArray())
            }
            true
        }.getOrDefault(false)
    }

    suspend fun deleteItem(treeUriString: String, item: SafFileItem): Boolean =
        withContext(Dispatchers.IO) {
            if (isLocalRoot(treeUriString) && item.uriString.startsWith(LOCAL_SCHEME_PREFIX)) {
                val root = localRootDir()
                val rel = item.uriString.removePrefix(LOCAL_SCHEME_PREFIX)
                val target = resolveSafeLocalFile(root, rel) ?: return@withContext false
                if (target.canonicalPath == root.canonicalPath) return@withContext false
                return@withContext target.deleteRecursively()
            }
            if (isDeviceRoot(treeUriString) && item.uriString.startsWith(DEVICE_SCHEME_PREFIX)) {
                val root = devicePrimaryRootDir() ?: return@withContext false
                val rel = item.uriString.removePrefix(DEVICE_SCHEME_PREFIX)
                val target = resolveSafeLocalFile(root, rel) ?: return@withContext false
                if (target.canonicalPath == root.canonicalPath) return@withContext false
                return@withContext runCatching { target.deleteRecursively() }.getOrDefault(false)
            }
            runCatching {
                DocumentsContract.deleteDocument(context.contentResolver, Uri.parse(item.uriString))
            }.getOrDefault(false)
        }

    fun isPreviewableText(item: SafFileItem): Boolean {
        if (item.isDirectory) return false
        val lower = item.name.lowercase()
        return item.mimeType.startsWith("text/") ||
            item.mimeType == "application/json" ||
            item.mimeType == "application/xml" ||
            lower.endsWith(".txt") ||
            lower.endsWith(".md") ||
            lower.endsWith(".json") ||
            lower.endsWith(".csv") ||
            lower.endsWith(".xml") ||
            lower.endsWith(".log")
    }

    suspend fun readTextPreview(item: SafFileItem): String? = withContext(Dispatchers.IO) {
        if (!isPreviewableText(item)) return@withContext null
        val file = resolveDirectFileFromUri(item.uriString)
        if (file != null) {
            return@withContext runCatching {
                file.bufferedReader().use { reader ->
                    val buffer = CharArray(32_768)
                    val read = reader.read(buffer)
                    if (read <= 0) "" else String(buffer, 0, read)
                }
            }.getOrNull()
        }
        runCatching {
            context.contentResolver.openInputStream(Uri.parse(item.uriString))?.use { stream ->
                stream.bufferedReader().use { reader ->
                    val buffer = CharArray(32_768)
                    val read = reader.read(buffer)
                    if (read <= 0) "" else String(buffer, 0, read)
                }
            }
        }.getOrNull()
    }

    fun openFile(item: SafFileItem): Boolean {
        val directFile = resolveDirectFileFromUri(item.uriString)
        val targetUri: Uri = if (directFile != null) {
            runCatching {
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", directFile)
            }.getOrElse { Uri.fromFile(directFile) }
        } else {
            Uri.parse(item.uriString)
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(targetUri, item.mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching {
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }

    private fun resolveDirectFileFromUri(uriString: String): File? {
        return when {
            uriString.startsWith(LOCAL_SCHEME_PREFIX) -> {
                val root = localRootDir()
                val rel = uriString.removePrefix(LOCAL_SCHEME_PREFIX)
                resolveSafeLocalFile(root, rel)
            }
            uriString.startsWith(DEVICE_SCHEME_PREFIX) -> {
                val root = devicePrimaryRootDir() ?: return null
                val rel = uriString.removePrefix(DEVICE_SCHEME_PREFIX)
                resolveSafeLocalFile(root, rel)
            }
            else -> null
        }
    }

    private fun resolveSafeLocalFile(root: File, relativePath: String?): File? {
        if (relativePath.isNullOrBlank()) return root
        val candidate = File(root, relativePath)
        val rootCanonical = root.canonicalPath
        val candidateCanonical = candidate.canonicalPath
        return if (candidateCanonical == rootCanonical ||
            candidateCanonical.startsWith("$rootCanonical${File.separator}")
        ) {
            candidate
        } else {
            null
        }
    }

    private fun guessMimeType(name: String): String {
        val ext = name.substringAfterLast('.', "").lowercase()
        if (ext.isEmpty()) return "application/octet-stream"
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
            ?: when (ext) {
                "md", "log" -> "text/plain"
                "json" -> "application/json"
                else -> "application/octet-stream"
            }
    }

    private fun queryDisplayName(documentUri: Uri): String? {
        return runCatching {
            context.contentResolver.query(
                documentUri,
                arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                val idx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                if (idx >= 0 && cursor.moveToFirst() && !cursor.isNull(idx)) cursor.getString(idx) else null
            }
        }.getOrNull()
    }
}
