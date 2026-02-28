package com.beconnect.ui.receiver

import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.beconnect.ble.BeaconInfo
import com.beconnect.ble.BleScanner
import com.beconnect.ble.GattClient
import com.beconnect.data.AlertDatabase
import com.beconnect.data.AlertPacket
import com.beconnect.databinding.ActivityReceiverBinding
import com.google.gson.Gson
import kotlinx.coroutines.launch

class ReceiverActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReceiverBinding
    private lateinit var scanner: BleScanner
    private val db by lazy { AlertDatabase.getInstance(this) }
    private val gson = Gson()
    private val beaconAdapter = BeaconAdapter { connectToBeacon(it) }
    private var scanning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReceiverBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.title = "Receiver Mode"

        val manager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        scanner = BleScanner(manager.adapter)
        scanner.onBeaconsUpdated = { beacons ->
            runOnUiThread { beaconAdapter.submitList(beacons.sortedByDescending { it.rssi }) }
        }

        binding.rvBeacons.layoutManager = LinearLayoutManager(this)
        binding.rvBeacons.adapter = beaconAdapter
        binding.btnScan.setOnClickListener { toggleScan() }
    }

    private fun toggleScan() {
        if (scanning) {
            scanner.stop()
            scanning = false
            binding.btnScan.text = "Scan for Beacons"
            binding.tvStatus.text = "Scan stopped"
        } else {
            scanner.start()
            scanning = true
            binding.btnScan.text = "Stop Scan"
            binding.tvStatus.text = "Scanning for BeConnect beacons…"
        }
    }

    private fun connectToBeacon(beacon: BeaconInfo) {
        if (scanning) { scanner.stop(); scanning = false }
        binding.progressBar.visibility = View.VISIBLE
        binding.tvStatus.text = "Downloading alert from ${beacon.address}…"

        lifecycleScope.launch {
            val manager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val device = manager.adapter.getRemoteDevice(beacon.address)
            GattClient(this@ReceiverActivity).downloadAlert(device)
                .onSuccess { bytes ->
                    runCatching {
                        val alert = gson.fromJson(String(bytes, Charsets.UTF_8), AlertPacket::class.java)
                        db.alertDao().insert(alert)
                        db.alertDao().pruneOldAlerts()
                        binding.progressBar.visibility = View.GONE
                        startActivity(
                            Intent(this@ReceiverActivity, AlertDetailActivity::class.java)
                                .putExtra(AlertDetailActivity.EXTRA_ALERT_JSON, gson.toJson(alert))
                        )
                    }.onFailure {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(this@ReceiverActivity, "Failed to parse alert packet", Toast.LENGTH_LONG).show()
                    }
                }
                .onFailure {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this@ReceiverActivity, "Download failed: ${it.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    override fun onDestroy() {
        if (scanning) scanner.stop()
        super.onDestroy()
    }
}
