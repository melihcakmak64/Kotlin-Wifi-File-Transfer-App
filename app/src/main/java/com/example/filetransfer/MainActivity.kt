package com.example.filetransfer

import android.Manifest
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Bundle
import android.text.format.Formatter
import android.util.Log
import android.widget.Button
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.filetransfer.databinding.ActivityMainBinding
import com.koushikdutta.async.http.server.AsyncHttpServer
import com.koushikdutta.async.http.server.AsyncHttpServerRequest
import com.koushikdutta.async.http.server.AsyncHttpServerResponse
import java.io.File

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

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








}
