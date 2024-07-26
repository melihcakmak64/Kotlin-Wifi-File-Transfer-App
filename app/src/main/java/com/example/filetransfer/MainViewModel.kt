package com.example.filetransfer

import android.Manifest
import android.app.Application
import android.content.Context.WIFI_SERVICE
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.text.format.Formatter
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.koushikdutta.async.http.server.AsyncHttpServer
import com.koushikdutta.async.http.server.AsyncHttpServerRequest
import com.koushikdutta.async.http.server.AsyncHttpServerResponse
import java.io.File

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val server = AsyncHttpServer()

    private val _fileList = MutableLiveData<List<File>>()
    val fileList: LiveData<List<File>> get() = _fileList

    private val _ipAddress = MutableLiveData<String>()
    val ipAddress: LiveData<String> get() = _ipAddress

    private val _isServerRunning = MutableLiveData<Boolean>().apply { value = false }
    val isServerRunning: LiveData<Boolean> get() = _isServerRunning

    init {
        requestPermissions()
    }

    fun startServer() {
        val port = 8080
        server.get("/files") { request: AsyncHttpServerRequest, response: AsyncHttpServerResponse ->
            val files = getFilesList("")
            val htmlResponse = buildString {
                append("<html><body><h1>File List</h1><ul>")
                files.forEach { file ->
                    val fileName = file.name
                    if (file.isDirectory) {
                        append("<li><a href=\"/files/${fileName}\">$fileName/</a></li>")
                    } else {
                        append("<li><a href=\"/files/${fileName}\">$fileName</a></li>")
                    }
                }
                append("</ul></body></html>")
            }
            response.send(htmlResponse)
        }

        server.get("/files/(.*)") { request: AsyncHttpServerRequest, response: AsyncHttpServerResponse ->
            val path = request.path.replace("/files/", "")
            val file = File("/storage/emulated/0/$path")

            if (file.isDirectory) {
                val filesInDirectory = file.listFiles() ?: emptyArray()
                val htmlResponse = buildString {
                    append("<html><body><h1>Contents of ${file.name}</h1><ul>")
                    filesInDirectory.forEach { subFile ->
                        val subFileName = subFile.name
                        if (subFile.isDirectory) {
                            append("<li><a href=\"/files/${path}/${subFileName}\">$subFileName/</a></li>")
                        } else {
                            append("<li><a href=\"/files/${path}/${subFileName}\">$subFileName</a></li>")
                        }
                    }
                    append("</ul><a href=\"/files\">Back</a></body></html>")
                }
                response.send(htmlResponse)
            } else if (file.exists()) {
                response.sendFile(file)
            } else {
                response.code(404).send("File not found")
            }
        }

        server.listen(port)
        val ipAddress = getIpAddress()

        _isServerRunning.value = true   // Update the server running status
        Log.d("Server", "Server started at http://$ipAddress:$port/files")
        _ipAddress.value = "http://$ipAddress:$port/files"  // Set the IP address
    }

    private fun getFilesList(path: String): List<File> {
        val directory = File("/storage/emulated/0/$path")
        return if (directory.exists() && directory.isDirectory) {
            directory.listFiles()?.toList() ?: emptyList()
        } else {
            emptyList()
        }
    }

    private fun getIpAddress(): String {
        val wifiManager = getApplication<Application>().applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        val ip = wifiManager.connectionInfo.ipAddress
        return Formatter.formatIpAddress(ip)
    }

    fun stopServer() {
        server.stop()
        _isServerRunning.value = false  // Update the server running status
        Log.d("Server", "Server stopped")
    }

    private fun requestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.INTERNET,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(getApplication(), it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(getApplication(), permissionsToRequest.toTypedArray(), 0)
        }
    }
}
