package com.example.wififiletransfer.view

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.wififiletransfer.viewmodels.MainViewModel
import com.example.wififiletransfer.databinding.ActivityMainBinding
import androidx.core.net.toUri

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    // Storage Access Framework için launcher
    private val storageAccessLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                // Tam erişim verildi
                binding.statusTextView.text = "Storage access granted"
            } else {
                // Erişim reddedildi
                binding.statusTextView.text = "Storage access denied"
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestPermissions()

        viewModel.ipAddress.observe(this) { ipAddress ->
            binding.ipAddressTextView.text = "IP Address: $ipAddress"
        }

        viewModel.isServerRunning.observe(this) { isRunning ->
            if (isRunning) {
                binding.startServerButton.text = "Stop Server"
            } else {
                binding.startServerButton.text = "Start Server"
                binding.ipAddressTextView.text = ""
            }
        }

        binding.startServerButton.setOnClickListener {
            if (viewModel.isServerRunning.value == true) {
                viewModel.stopServer()
            } else {
                viewModel.startServer()
            }
        }
    }

    private fun requestPermissions() {
        val permissions = mutableListOf<String>()

        // Temel izinler
        permissions.add(Manifest.permission.INTERNET)
        permissions.add(Manifest.permission.ACCESS_WIFI_STATE)
        permissions.add(Manifest.permission.ACCESS_NETWORK_STATE)

        // Android 13+ için medya izinleri
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            permissions.add(Manifest.permission.READ_MEDIA_VIDEO)
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            // Android 13 altı için eski izinler
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }

        // İzin verilen izinleri filtrele
        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), 0)
        }

        // Android 11+ için All Files Access izni
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                showStorageAccessDialog()
            }
        }
    }

    private fun showStorageAccessDialog() {
        AlertDialog.Builder(this)
            .setTitle("Storage Access Required")
            .setMessage("To access all files on your device, please grant 'All Files Access' permission in the next screen.")
            .setPositiveButton("Grant Access") { _, _ ->
                requestAllFilesAccess()
            }
            .setNegativeButton("Cancel") { _, _ ->
                binding.statusTextView.text = "Limited file access - only media files visible"
            }
            .show()
    }

    private fun requestAllFilesAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = "package:$packageName".toUri()
                storageAccessLauncher.launch(intent)
            } catch (e: Exception) {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                storageAccessLauncher.launch(intent)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
        if (allGranted) {
            binding.statusTextView.text = "Basic permissions granted"
        } else {
            binding.statusTextView.text = "Some permissions denied"
        }
    }
}