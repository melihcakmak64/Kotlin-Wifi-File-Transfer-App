package com.example.wififiletransfer.services

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Environment
import android.text.format.Formatter
import android.util.Log
import com.example.wififiletransfer.utils.ZipUtil
import io.ktor.http.ContentDisposition
import io.ktor.http.HttpHeaders
import io.ktor.http.content.*
import io.ktor.server.application.call
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.files
import io.ktor.server.http.content.static
import io.ktor.server.request.path
import io.ktor.server.request.receiveMultipart
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondFile
import io.ktor.server.response.respondRedirect
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class FileTransferServer(private val context: Context) {
    private var server: CIOApplicationEngine? = null
    private val port = 8080
    private val cacheDir = context.cacheDir

    // Ana depolama dizinlerini tanımla
    private val rootPaths = getAccessibleRootPaths()

    fun startServer(onServerStarted: (String) -> Unit) {
        server = embeddedServer(CIO, port) {
            routing {
                // Statik dosya servisi - tüm erişilebilir yollar için
                rootPaths.forEach { (name, path) ->
                    static("/$name") {
                        files(path)
                    }
                }

                get("/") {
                    val htmlContent = buildMainHtml()
                    call.respondText(htmlContent, contentType = io.ktor.http.ContentType.Text.Html)
                }

                get("/download") {
                    val paths = call.request.queryParameters.getAll("paths") ?: emptyList()
                    val rootPath = call.request.queryParameters["root"] ?: "internal"

                    if (paths.isEmpty()) {
                        call.respondText("No files selected for download", contentType = io.ktor.http.ContentType.Text.Plain)
                        return@get
                    }

                    val baseDir = File(rootPaths[rootPath] ?: rootPaths["internal"]!!)
                    val filesToZip = paths.map { File(baseDir, it) }

                    val zipFile = File(cacheDir, "temp_${System.currentTimeMillis()}.zip")
                    ZipOutputStream(zipFile.outputStream()).use { zipOut ->
                        for (file in filesToZip) {
                            if (file.exists() && file.canRead()) {
                                if (file.isDirectory) {
                                    addDirectoryToZip(file, zipOut, file.name + "/")
                                } else {
                                    zipOut.putNextEntry(ZipEntry(file.name))
                                    file.inputStream().copyTo(zipOut)
                                    zipOut.closeEntry()
                                }
                            }
                        }
                    }

                    call.response.header(
                        HttpHeaders.ContentDisposition,
                        ContentDisposition.Attachment.withParameter(ContentDisposition.Parameters.FileName, "files.zip").toString()
                    )
                    call.respondFile(zipFile)
                    zipFile.deleteOnExit()
                }

                get("/browse") {
                    val path = call.parameters["path"] ?: ""
                    val rootPath = call.parameters["root"] ?: "internal"

                    val baseDir = File(rootPaths[rootPath] ?: rootPaths["internal"]!!)
                    val currentDir = if (path.isEmpty()) baseDir else File(baseDir, path)

                    if (currentDir.exists() && currentDir.isDirectory && currentDir.canRead()) {
                        val htmlContent = buildDirectoryHtml(currentDir, baseDir, rootPath)
                        call.respondText(htmlContent, contentType = io.ktor.http.ContentType.Text.Html)
                    } else {
                        call.respondText("Directory not found or access denied", contentType = io.ktor.http.ContentType.Text.Plain)
                    }
                }

                post("/upload") {
                    try {
                        val path = call.request.queryParameters["path"] ?: ""
                        val rootPath = call.request.queryParameters["root"] ?: "internal"

                        val baseDir = File(rootPaths[rootPath] ?: rootPaths["internal"]!!)
                        val uploadDir = if (path.isEmpty()) baseDir else File(baseDir, path)

                        if (!uploadDir.exists()) {
                            uploadDir.mkdirs()
                        }

                        val multipart = call.receiveMultipart()
                        var uploadedFiles = 0

                        multipart.forEachPart { part ->
                            try {
                                when (part) {
                                    is PartData.FileItem -> {
                                        val fileName = part.originalFileName
                                        if (!fileName.isNullOrBlank()) {
                                            val fileBytes = part.streamProvider().readBytes()
                                            val file = File(uploadDir, fileName)
                                            file.writeBytes(fileBytes)
                                            uploadedFiles++
                                        }
                                    }
                                    else -> {}
                                }
                            } catch (e: Exception) {
                                Log.e("Upload", "Error processing part: ${e.message}")
                            } finally {
                                part.dispose()
                            }
                        }

                        if (uploadedFiles > 0) {
                            call.respondRedirect("/browse?root=$rootPath&path=$path")
                        } else {
                            call.respondText("No files were uploaded", contentType = io.ktor.http.ContentType.Text.Plain)
                        }

                    } catch (e: Exception) {
                        Log.e("Upload", "Upload error: ${e.message}")
                        call.respondText("Upload failed: ${e.message}", contentType = io.ktor.http.ContentType.Text.Plain)
                    }
                }

                post("/delete") {
                    val parameters = call.receiveParameters()
                    val paths = parameters.getAll("paths") ?: emptyList()
                    val currentPath = parameters["currentPath"] ?: ""
                    val rootPath = parameters["root"] ?: "internal"

                    val baseDir = File(rootPaths[rootPath] ?: rootPaths["internal"]!!)

                    paths.forEach { relativePath ->
                        val file = File(baseDir, relativePath)
                        if (file.exists() && file.canWrite()) {
                            file.deleteRecursively()
                        }
                    }
                    call.respondRedirect("/browse?root=$rootPath&path=$currentPath")
                }
            }
        }
        server?.start(wait = false)

        val ipAddress = getIpAddress()
        Log.d("Server", "Server started at http://$ipAddress:$port")
        onServerStarted("http://$ipAddress:$port")
    }

    fun stopServer() {
        server?.stop(1000, 1000)
        Log.d("Server", "Server stopped")
    }

    private fun getAccessibleRootPaths(): Map<String, String> {
        val paths = mutableMapOf<String, String>()

        // Internal storage (her zaman erişilebilir)
        paths["internal"] = "/storage/emulated/0"

        // Android 11+ için MANAGE_EXTERNAL_STORAGE izni ile tüm dosyalar
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
            paths["root"] = "/storage"
            paths["system"] = "/system"
        }

        // External SD kart kontrol et
        val externalDirs = context.getExternalFilesDirs(null)
        externalDirs.forEachIndexed { index, dir ->
            if (dir != null && index > 0) { // İlki internal storage
                val sdCardPath = dir.absolutePath.substringBefore("/Android")
                if (File(sdCardPath).exists()) {
                    paths["sdcard$index"] = sdCardPath
                }
            }
        }

        return paths
    }

    private fun buildMainHtml(): String {
        return """
        <html>
        <head>
            <title>File Transfer Server</title>
            <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
        </head>
        <body class="p-4 bg-light">
            <div class="container">
                <h1 class="mb-4">📱 File Transfer Server</h1>
                <div class="row">
                    ${rootPaths.map { (key, path) ->
            val displayName = when(key) {
                "internal" -> "📱 Internal Storage"
                "root" -> "🔧 Root Directory"
                "system" -> "⚙️ System"
                else -> if (key.startsWith("sdcard")) "💾 SD Card" else "📁 $key"
            }
            """
                        <div class="col-md-6 mb-3">
                            <div class="card">
                                <div class="card-body">
                                    <h5 class="card-title">$displayName</h5>
                                    <p class="card-text"><small class="text-muted">$path</small></p>
                                    <a href="/browse?root=$key" class="btn btn-primary">Browse</a>
                                </div>
                            </div>
                        </div>
                        """
        }.joinToString("")}
                </div>
            </div>
        </body>
        </html>
        """.trimIndent()
    }

    private fun buildDirectoryHtml(currentDir: File, baseDir: File, rootPath: String): String {
        val currentPath = try {
            currentDir.relativeTo(baseDir).path
        } catch (e: Exception) {
            ""
        }

        val htmlBuilder = StringBuilder()
        htmlBuilder.append("""
        <html>
        <head>
            <title>File Browser - $rootPath</title>
            <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
        </head>
        <body class="p-4 bg-light">
            <div class="container">
                <div class="d-flex justify-content-between align-items-center mb-4">
                    <h1>📁 ${currentDir.name.ifEmpty { rootPath.uppercase() }}</h1>
                    <a href="/" class="btn btn-secondary">🏠 Home</a>
                </div>
                <form method="post" id="mainForm">
                    <input type="hidden" name="currentPath" value="$currentPath">
                    <input type="hidden" name="root" value="$rootPath">
                    <ul class="list-group mb-3">
        """.trimIndent())

        // Parent directory link
        if (currentDir != baseDir) {
            val parentPath = try {
                currentDir.parentFile?.relativeTo(baseDir)?.path ?: ""
            } catch (e: Exception) {
                ""
            }
            htmlBuilder.append("""
            <li class="list-group-item">
                <a href="/browse?root=$rootPath&path=$parentPath">⬅️ .. (up)</a>
            </li>
            """.trimIndent())
        }

        // Directory contents
        try {
            currentDir.listFiles()?.filter { it.canRead() }?.forEach { file ->
                val relativePath = try {
                    file.relativeTo(baseDir).path
                } catch (e: Exception) {
                    file.name
                }

                val checkbox = """<input class="form-check-input me-2" type="checkbox" name="paths" value="$relativePath">"""
                val icon = if (file.isDirectory) "📁" else "📄"
                val link = if (file.isDirectory)
                    """<a href="/browse?root=$rootPath&path=$relativePath">$icon ${file.name}/</a>"""
                else
                    """<a href="/$rootPath/$relativePath">$icon ${file.name}</a>"""

                htmlBuilder.append("""
                <li class="list-group-item d-flex align-items-center">
                    $checkbox $link
                </li>
                """.trimIndent())
            }
        } catch (e: Exception) {
            htmlBuilder.append("""
            <li class="list-group-item text-danger">
                ❌ Cannot read directory contents: ${e.message}
            </li>
            """.trimIndent())
        }

        htmlBuilder.append("""
                    </ul>
                    <div class="mb-3">
                        <button type="submit" formaction="/delete" class="btn btn-danger me-2">🗑️ Delete Selected</button>
                        <button type="button" onclick="downloadSelected()" class="btn btn-primary">⬇️ Download Selected</button>
                    </div>
                </form>

                <form action="/upload?root=$rootPath&path=$currentPath" method="post" enctype="multipart/form-data" class="mt-4">
                    <div class="mb-3">
                        <input class="form-control" type="file" name="files" multiple id="fileInput">
                    </div>
                    <button class="btn btn-success" type="submit">📤 Upload</button>
                </form>
            </div>
            
            <script>
                function downloadSelected() {
                    const checkboxes = document.querySelectorAll('input[name="paths"]:checked');
                    if (checkboxes.length === 0) {
                        alert('Please select at least one file or folder to download.');
                        return;
                    }
                    
                    const params = new URLSearchParams();
                    params.append('root', '$rootPath');
                    checkboxes.forEach(checkbox => {
                        params.append('paths', checkbox.value);
                    });
                    
                    window.location.href = '/download?' + params.toString();
                }
            </script>
        </body>
        </html>
        """.trimIndent())

        return htmlBuilder.toString()
    }

    private fun addDirectoryToZip(dir: File, zipOut: ZipOutputStream, basePath: String) {
        try {
            dir.listFiles()?.filter { it.canRead() }?.forEach { file ->
                val entryPath = basePath + file.name
                if (file.isDirectory) {
                    zipOut.putNextEntry(ZipEntry("$entryPath/"))
                    zipOut.closeEntry()
                    addDirectoryToZip(file, zipOut, "$entryPath/")
                } else {
                    zipOut.putNextEntry(ZipEntry(entryPath))
                    file.inputStream().copyTo(zipOut)
                    zipOut.closeEntry()
                }
            }
        } catch (e: Exception) {
            Log.e("Zip", "Error adding directory to zip: ${e.message}")
        }
    }

    private fun getIpAddress(): String {
        val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val ip = wifiManager.connectionInfo.ipAddress
        return Formatter.formatIpAddress(ip)
    }
}