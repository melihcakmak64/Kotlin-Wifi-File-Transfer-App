package com.example.wififiletransfer.utils

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
class PermissionsManager(private val context: Context) {

    fun needsAllFilesAccess(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()
    }

    fun needsLegacyStoragePermissions(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.R && (
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                        (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                )
    }

    fun getLegacyPermissionsToRequest(): Array<String> {
        val list = mutableListOf<String>()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                list.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                list.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
        return list.toTypedArray()
    }

    // Android 11+ için direkt settings dialog
    fun showAllFilesAccessDialog(onGrantAccess: () -> Unit, onCancel: () -> Unit = {}) {
        AlertDialog.Builder(context)
            .setTitle("Storage Access Required")
            .setMessage("To access all files on your device, please grant the 'All files access' permission in settings.")
            .setPositiveButton("Open Settings") { _, _ -> onGrantAccess() }
            .setNegativeButton("Cancel") { _, _ -> onCancel() }
            .show()
    }

    // Android 10 ve altı için önce permission request, sonra settings
    fun showLegacyStoragePermissionDialog(onRequestPermission: () -> Unit, onCancel: () -> Unit = {}) {

        /* AlertDialog.Builder(context)
            .setTitle("Storage Access Required")
            .setMessage("To access files on your device, please grant storage permissions.")
            .setPositiveButton("Grant Permission") { _, _ -> onRequestPermission() }
            .setNegativeButton("Cancel") { _, _ -> onCancel() }
            .show() */
        onRequestPermission()
    }

    // Permission reddedildikten sonra settings'e yönlendirme
    fun showPermissionDeniedDialog(onOpenSettings: () -> Unit, onCancel: () -> Unit = {}) {
        AlertDialog.Builder(context)
            .setTitle("Permission Denied")
            .setMessage("Storage permission is required for the app to work properly. Please enable it manually in app settings.")
            .setPositiveButton("Open Settings") { _, _ -> onOpenSettings() }
            .setNegativeButton("Cancel") { _, _ -> onCancel() }
            .show()
    }

    fun createAllFilesAccessIntent(): Intent? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return try {
                Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = "package:${context.packageName}".toUri()
                }
            } catch (e: Exception) {
                Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
            }
        }
        return null
    }

    fun createAppSettingsIntent(): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = "package:${context.packageName}".toUri()
        }
    }

    // Permission'ın daha önce reddedilip reddedilmediğini kontrol etmek için
    fun shouldShowLegacyPermissionRationale(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.R && (
                shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE) ||
                        (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                                shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE))
                )
    }

    private fun shouldShowRequestPermissionRationale(permission: String): Boolean {
        return if (context is androidx.fragment.app.FragmentActivity) {
            androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(context, permission)
        } else {
            false
        }
    }
}