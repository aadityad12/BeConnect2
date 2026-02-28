package com.beconnect.ui.gateway

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.beconnect.data.AlertDatabase
import com.beconnect.data.AlertPacket
import com.beconnect.databinding.ActivityGatewayBinding
import com.beconnect.demo.DemoAlerts
import com.beconnect.network.AlertFetcher
import com.beconnect.service.GatewayForegroundService
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class GatewayActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGatewayBinding
    private val db by lazy { AlertDatabase.getInstance(this) }
    private val dateFormat = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())

    private var currentAlert: AlertPacket? = null
    private var isBroadcasting = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGatewayBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.title = "Gateway Mode"

        binding.btnFetch.setOnClickListener { fetchAlert() }
        binding.btnDemo.setOnClickListener { loadDemo() }
        binding.btnBroadcast.setOnClickListener { toggleBroadcast() }
    }

    private fun fetchAlert() {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnFetch.isEnabled = false
        lifecycleScope.launch {
            AlertFetcher.fetchLatest()
                .onSuccess { alerts ->
                    binding.progressBar.visibility = View.GONE
                    binding.btnFetch.isEnabled = true
                    if (alerts.isEmpty()) {
                        Toast.makeText(this@GatewayActivity, "No active alerts. Try Demo Mode.", Toast.LENGTH_LONG).show()
                        return@onSuccess
                    }
                    showAlert(alerts.first())
                    alerts.forEach { db.alertDao().insert(it) }
                    db.alertDao().pruneOldAlerts()
                }
                .onFailure {
                    binding.progressBar.visibility = View.GONE
                    binding.btnFetch.isEnabled = true
                    Toast.makeText(
                        this@GatewayActivity,
                        "Fetch failed: ${it.message}. Use Demo Mode.",
                        Toast.LENGTH_LONG
                    ).show()
                }
        }
    }

    private fun loadDemo() {
        showAlert(DemoAlerts.alerts.first())
        Toast.makeText(this, "Demo alert loaded (marked Unverified)", Toast.LENGTH_SHORT).show()
    }

    private fun showAlert(alert: AlertPacket) {
        currentAlert = alert
        binding.tvSeverity.text = alert.severity
        binding.tvHeadline.text = alert.headline
        binding.tvExpires.text = "Expires: ${dateFormat.format(Date(alert.expires * 1000))}"
        binding.tvVerified.text = if (alert.verified) "✓ Verified" else "⚠ Unverified / Demo"
        binding.cardAlert.visibility = View.VISIBLE
        binding.btnBroadcast.isEnabled = true
    }

    private fun toggleBroadcast() {
        val alert = currentAlert ?: return
        if (isBroadcasting) {
            GatewayForegroundService.stop(this)
            isBroadcasting = false
            binding.btnBroadcast.text = "Start Broadcasting"
            binding.tvStatus.text = "Status: Stopped"
        } else {
            GatewayForegroundService.start(this, alert)
            isBroadcasting = true
            binding.btnBroadcast.text = "Stop Broadcasting"
            binding.tvStatus.text = "Status: Broadcasting via BLE…"
        }
    }

    override fun onDestroy() {
        if (isBroadcasting) GatewayForegroundService.stop(this)
        super.onDestroy()
    }
}
