package com.example.wififiletransfer.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.wififiletransfer.services.FileTransferServer

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val fileTransferServer = FileTransferServer(application.applicationContext)

    private val _ipAddress = MutableLiveData<String>()
    val ipAddress: LiveData<String> get() = _ipAddress

    private val _isServerRunning = MutableLiveData<Boolean>().apply { value = false }
    val isServerRunning: LiveData<Boolean> get() = _isServerRunning

    fun startServer() {
        fileTransferServer.startServer { ipAddress ->
            _ipAddress.value = ipAddress
            _isServerRunning.value = true
        }
    }

    fun stopServer() {
        fileTransferServer.stopServer()
        _isServerRunning.value = false
    }
}
