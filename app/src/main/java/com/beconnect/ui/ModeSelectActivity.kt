package com.beconnect.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.beconnect.databinding.ActivityModeSelectBinding
import com.beconnect.ui.gateway.GatewayActivity
import com.beconnect.ui.receiver.ReceiverActivity

class ModeSelectActivity : AppCompatActivity() {

    private lateinit var binding: ActivityModeSelectBinding
    private var pendingAction: (() -> Unit)? = null

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            if (results.values.all { it }) {
                pendingAction?.invoke()
            } else {
                Toast.makeText(this, "Bluetooth permissions are required", Toast.LENGTH_LONG).show()
            }
            pendingAction = null
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityModeSelectBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.title = "BeConnect"

        binding.btnGateway.setOnClickListener {
            withPermissions { startActivity(Intent(this, GatewayActivity::class.java)) }
        }
        binding.btnReceiver.setOnClickListener {
            withPermissions { startActivity(Intent(this, ReceiverActivity::class.java)) }
        }
    }

    private fun withPermissions(action: () -> Unit) {
        val needed = requiredPermissions().filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needed.isEmpty()) { action(); return }
        pendingAction = action
        permissionLauncher.launch(needed.toTypedArray())
    }

    private fun requiredPermissions(): List<String> = buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            add(Manifest.permission.BLUETOOTH_SCAN)
            add(Manifest.permission.BLUETOOTH_CONNECT)
            add(Manifest.permission.BLUETOOTH_ADVERTISE)
        }
        add(Manifest.permission.ACCESS_FINE_LOCATION)
    }
}
