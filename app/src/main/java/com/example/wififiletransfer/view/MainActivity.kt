package com.example.wififiletransfer.view

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.wififiletransfer.databinding.ActivityMainBinding
import com.example.wififiletransfer.utils.PermissionsManager
import kotlin.getValue
import com.example.wififiletransfer.viewmodels.MainViewModel
import androidx.core.net.toUri

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    private lateinit var permissionsManager: PermissionsManager

    private val storageAccessLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (permissionsManager.needsAllFilesAccess()) {
            binding.statusTextView.text = "Storage access denied"
        } else {
            binding.statusTextView.text = "Storage access granted"
        }
    }

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            binding.statusTextView.text = "Basic permissions granted"
        } else {
            binding.statusTextView.text = "Some permissions denied"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        permissionsManager = PermissionsManager(this)

        requestPermissions()

        viewModel.ipAddress.observe(this) { ipAddress ->
            binding.ipAddressTextView.text = "IP Address: $ipAddress"
        }

        viewModel.isServerRunning.observe(this) { isRunning ->
            binding.startServerButton.text = if (isRunning) "Stop Server" else "Start Server"
            if (!isRunning) binding.ipAddressTextView.text = ""
        }

        binding.startServerButton.setOnClickListener {
           checkAndStartOrStopServer()
        }
    }

    private fun requestPermissions() {
        when {
            permissionsManager.needsAllFilesAccess() -> {
                permissionsManager.showStorageAccessDialog(
                    onGrantAccess = {
                        permissionsManager.createAllFilesAccessIntent()?.let {
                            storageAccessLauncher.launch(it)
                        }
                    },
                )
            }
            permissionsManager.needsLegacyStoragePermissions() -> {
                requestPermissionsLauncher.launch(permissionsManager.getLegacyPermissionsToRequest())
            }
            else -> {
                binding.statusTextView.text = "All required permissions already granted"
            }
        }
    }


    private fun showPermissionRequiredDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permission Needed")
            .setMessage("You have denied the All Files Access permission. Please enable it manually in app settings for full functionality.")
            .setPositiveButton("Open Settings") { _, _ ->
                // Ayarlar sayfasını aç
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = "package:$packageName".toUri()
                }
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun checkAndStartOrStopServer() {
        when {
            permissionsManager.needsAllFilesAccess() -> {
                permissionsManager.showStorageAccessDialog(
                    onGrantAccess = {
                        permissionsManager.createAllFilesAccessIntent()?.let {
                            storageAccessLauncher.launch(it)
                        }
                    },
                )
            }
            permissionsManager.needsLegacyStoragePermissions() -> {
                requestPermissionsLauncher.launch(permissionsManager.getLegacyPermissionsToRequest())
            }
            else -> {
                if (viewModel.isServerRunning.value == true) {
                    viewModel.stopServer()
                } else {
                    viewModel.startServer()
                }
            }
        }
    }




}
