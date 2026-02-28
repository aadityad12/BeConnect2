package com.beconnect.ui.receiver

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.beconnect.ble.BeaconInfo
import com.beconnect.databinding.ItemBeaconBinding
import java.text.SimpleDateFormat
import java.util.*

class BeaconAdapter(private val onClick: (BeaconInfo) -> Unit) :
    ListAdapter<BeaconInfo, BeaconAdapter.ViewHolder>(DIFF) {

    inner class ViewHolder(private val b: ItemBeaconBinding) : RecyclerView.ViewHolder(b.root) {
        private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

        fun bind(beacon: BeaconInfo) {
            b.tvAddress.text = beacon.address
            b.tvRssi.text = "${beacon.rssi} dBm"
            b.tvLastSeen.text = "Last seen: ${timeFormat.format(Date(beacon.lastSeen))}"
            b.root.setOnClickListener { onClick(beacon) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemBeaconBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position))

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<BeaconInfo>() {
            override fun areItemsTheSame(a: BeaconInfo, b: BeaconInfo) = a.address == b.address
            override fun areContentsTheSame(a: BeaconInfo, b: BeaconInfo) = a == b
        }
    }
}
