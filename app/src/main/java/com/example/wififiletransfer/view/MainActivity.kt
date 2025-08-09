package com.example.wififiletransfer.view

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.wififiletransfer.databinding.ActivityMainBinding
import com.example.wififiletransfer.utils.PermissionsManager
import com.example.wififiletransfer.viewmodels.MainViewModel

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    private lateinit var permissionsManager: PermissionsManager

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
         if(!allGranted) {
            // İzin reddedilmiş, ayarlara yönlendir
            permissionsManager.showPermissionDeniedDialog(
                onOpenSettings = {
                    startActivity(permissionsManager.createAppSettingsIntent())
                }
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        permissionsManager = PermissionsManager(this)


        viewModel.ipAddress.observe(this) { ip ->
            binding.ipAddressTextView.text = "IP Address: $ip"
        }

        viewModel.isServerRunning.observe(this) { isRunning ->
            binding.startServerButton.text = if (isRunning) "Stop Server" else "Start Server"
            if (!isRunning) binding.ipAddressTextView.text = ""
        }

        binding.startServerButton.setOnClickListener {
            checkAndStartOrStopServer()
        }
    }


    private fun checkAndStartOrStopServer() {
        when {
            permissionsManager.needsAllFilesAccess() -> {
                permissionsManager.showAllFilesAccessDialog(
                    onGrantAccess = {
                        permissionsManager.createAllFilesAccessIntent()
                    }
                )
            }
            permissionsManager.needsLegacyStoragePermissions() -> {
                if (permissionsManager.shouldShowLegacyPermissionRationale()) {
                    permissionsManager.showPermissionDeniedDialog(
                        onOpenSettings = {
                            startActivity(permissionsManager.createAppSettingsIntent())
                        }
                    )
                } else {
                    permissionsManager.showLegacyStoragePermissionDialog(
                        onRequestPermission = {
                            requestPermissionsLauncher.launch(permissionsManager.getLegacyPermissionsToRequest())
                        }
                    )
                }
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
