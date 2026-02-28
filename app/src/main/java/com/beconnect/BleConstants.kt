package com.beconnect

import java.util.UUID

object BleConstants {
    /** Primary service UUID — used by receiver to filter BLE scans */
    val SERVICE_UUID: UUID = UUID.fromString("0000BCBC-0000-1000-8000-00805F9B34FB")

    /** Characteristic the receiver reads chunked alert data from */
    val ALERT_CHAR_UUID: UUID = UUID.fromString("0000BCB1-0000-1000-8000-00805F9B34FB")

    /** Characteristic the receiver writes the requested chunk index to */
    val CONTROL_CHAR_UUID: UUID = UUID.fromString("0000BCB2-0000-1000-8000-00805F9B34FB")

    /** Manufacturer ID used in advertisement payload for metadata */
    const val MANUFACTURER_ID = 0x1234

    /** Safe chunk size before MTU negotiation (MTU 23 − 3 ATT overhead = 20, minus 3 for frame header) */
    const val DEFAULT_CHUNK_SIZE = 17
}
