package com.beconnect.ui.receiver

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.beconnect.data.AlertPacket
import com.beconnect.databinding.ActivityAlertDetailBinding
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.*

class AlertDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAlertDetailBinding
    private var tts: TextToSpeech? = null
    private val dateFormat = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAlertDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.title = "Alert"

        val json = intent.getStringExtra(EXTRA_ALERT_JSON) ?: run { finish(); return }
        val alert = Gson().fromJson(json, AlertPacket::class.java)

        binding.tvSeverity.text = alert.severity
        binding.tvHeadline.text = alert.headline
        binding.tvExpires.text = "Expires: ${dateFormat.format(Date(alert.expires * 1000))}"
        binding.tvInstructions.text = alert.instructions
        binding.tvSource.text = "Source: ${alert.sourceUrl}"
        binding.tvVerified.text = if (alert.verified) "✓ Verified (Official Source)" else "⚠ Unverified / Demo"

        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                binding.btnReadAloud.isEnabled = true
            } else {
                Log.w(TAG, "TTS init failed: $status")
            }
        }
        binding.btnReadAloud.setOnClickListener {
            tts?.speak(
                "${alert.severity} alert. ${alert.headline}. ${alert.instructions}",
                TextToSpeech.QUEUE_FLUSH, null, "alert_read"
            )
        }
    }

    override fun onDestroy() {
        tts?.shutdown()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_ALERT_JSON = "alert_json"
        private const val TAG = "AlertDetailActivity"
    }
}
