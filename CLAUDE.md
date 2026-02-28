# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

BeConnect is an offline-first emergency alert distribution system. A "gateway" device with internet fetches official alerts and broadcasts them locally via BLE to nearby phones — so people receive critical guidance even when cellular/Wi-Fi is down.

**Architecture model:** Cloud pull + offline push (not a chat app; one-way trusted alert dissemination).

## App Structure

Single Android app (Kotlin) with two operating modes selectable at runtime:

- **Gateway mode** — fetches alerts from NWS CAP API → compresses to alert packet → BLE advertise metadata → GATT server serves full packet on request
- **Receiver mode** — BLE scan → filter by `BeConnect` service UUID → GATT client connects → downloads + reassembles chunks → displays alert → persists locally

```
app/src/main/java/com/beconnect/
├── ble/
│   ├── BleAdvertiser.kt        # Gateway: BLE advertise metadata
│   ├── GattServer.kt           # Gateway: serve alert packet over GATT
│   ├── BleScanner.kt           # Receiver: scan + filter by service UUID
│   ├── GattClient.kt           # Receiver: connect + chunked read
│   └── ChunkUtils.kt           # Shared: chunk/reassemble byte arrays
├── network/
│   ├── AlertFetcher.kt         # Fetch NWS CAP/ATOM feed
│   └── AlertParser.kt          # Parse CAP XML → AlertPacket
├── data/
│   ├── AlertPacket.kt          # Core data model (alert_id, severity, headline, expires, instructions, source_url, verified)
│   ├── AlertDatabase.kt        # Room DB
│   └── AlertDao.kt
├── ui/
│   ├── ModeSelectActivity.kt   # Choose Gateway or Receiver on launch
│   ├── gateway/
│   │   └── GatewayActivity.kt  # Fetch, preview, start advertising
│   └── receiver/
│       ├── ReceiverActivity.kt  # Scan, list beacons
│       └── AlertDetailActivity.kt
└── demo/
    └── DemoAlerts.kt           # Hardcoded fallback alert packets for demo mode
```

## Build & Run

```bash
# Build debug APK
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug

# Run all tests
./gradlew test

# Run a single test class
./gradlew test --tests "com.beconnect.ble.ChunkUtilsTest"

# Lint
./gradlew lint

# Build release APK (requires signing config)
./gradlew assembleRelease
```

Open in Android Studio: **File → Open → select repo root**.

Min SDK: **26** (BLE GATT + Room; BLE advertising requires SDK 21+, but SDK 26 gives background execution).
Target SDK: **34**.

## BLE Architecture Details

### Advertising payload (31-byte limit)
Only metadata fits in the advertisement:
```
[Service UUID 16-byte] + [alert_id hash 4-byte] + [severity 1-byte] + [unix_ts 4-byte]
```
Use `AdvertiseData.Builder().addServiceUuid()` + manufacturer-specific data for the 9 bytes of metadata.

### GATT packet transfer
- Service UUID: defined as constant `BleConstants.SERVICE_UUID`
- Characteristic UUID: `BleConstants.ALERT_CHAR_UUID`
- Full `AlertPacket` serialized as JSON → UTF-8 bytes → chunked at MTU-3 bytes
- Gateway writes chunk index + total + payload into characteristic; receiver requests chunk N by writing index to a control characteristic, then reads data characteristic
- After all chunks received, receiver reassembles and JSON-parses into `AlertPacket`

### Key BLE permissions (AndroidManifest.xml)
```xml
<uses-permission android:name="android.permission.BLUETOOTH_ADVERTISE" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.BLUETOOTH_SCAN"
    android:usesPermissionFlags="neverForLocation" />
<!-- For SDK < 31: -->
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
```
Runtime permission checks are required for all three on SDK 31+. On SDK 23–30, `ACCESS_FINE_LOCATION` is also required for scanning.

## Data Model

```kotlin
data class AlertPacket(
    val alertId: String,       // SHA-1 hash of headline+expires
    val severity: String,      // "Extreme" | "Severe" | "Moderate" | "Minor" | "Unknown"
    val headline: String,
    val expires: Long,         // Unix epoch seconds
    val instructions: String,
    val sourceUrl: String,
    val verified: Boolean,     // true only if fetched from NWS/official source
    val fetchedAt: Long        // Unix epoch seconds
)
```

## NWS API

Base URL: `https://api.weather.gov/alerts/active?status=actual&message_type=alert&limit=5`
Returns GeoJSON; each feature has `properties.severity`, `properties.headline`, `properties.instruction`, `properties.expires`, `properties.id`.
No API key required. Add header `User-Agent: BeConnect/1.0 (contact@example.com)` (NWS requires this).

Demo mode fallback: if fetch fails or network unavailable, `DemoAlerts.kt` provides 2–3 hardcoded `AlertPacket` objects.

## MVP Scope Boundaries

**In scope:**
- Gateway: fetch NWS → advertise → GATT serve
- Receiver: scan → connect → download → display → Room persistence (last 20 alerts)
- Demo mode fallback
- Single-hop BLE only

**Explicitly out of scope (do not add):**
- Multi-hop BLE mesh routing
- iOS support
- Cryptographic PKI / signature verification beyond `verified` boolean flag
- User-generated alert messages
- Real-time continuous sync

**Nice-to-haves (only if core is done):**
- `TextToSpeech` read-aloud (offline, Android built-in)
- Two-language toggle with hardcoded translations

## BLE Debugging Tips

- **Scanning returns no results:** Check that `ACCESS_FINE_LOCATION` is granted on SDK ≤ 30. On SDK 31+, check `BLUETOOTH_SCAN`. Location services must be enabled on the device even if you use `neverForLocation`.
- **Advertising fails silently:** Not all Android devices support BLE peripheral mode. Check `BluetoothAdapter.isMultipleAdvertisementSupported()` and surface a message if false. Emulators do not support BLE advertising — use physical devices.
- **MTU negotiation:** Call `gatt.requestMtu(512)` from the client after `onConnectionStateChange`; wait for `onMtuChanged` callback before reading. Default MTU is 23, giving only 20 bytes per read.
- **GATT disconnects mid-transfer:** Add retry logic with exponential backoff. `GATT_ERROR (133)` is common on first connect; retry once after 600ms.
- **Chunked read order:** Do not assume BLE delivers chunks in order if using notifications. Use explicit chunk index writes + reads (request/response pattern) for reliability.
- **Background advertising killed:** Use a foreground `Service` with a persistent notification to keep the gateway advertising when the app is backgrounded.
- **Permissions dialog not shown:** Permissions must be requested from an `Activity`, not a `Service`. Request all BLE permissions at app start in `ModeSelectActivity`.

## Demo Script (End-to-End)

1. Both phones have the app installed.
2. Gateway phone: Wi-Fi ON → tap "Gateway Mode" → tap "Fetch Alert" (or "Demo Mode") → tap "Start Broadcasting".
3. Receiver phone: disable Wi-Fi, keep Bluetooth ON → tap "Receiver Mode" → tap "Scan" → beacon appears in list.
4. Tap beacon → app connects, downloads, displays alert with severity + headline + instructions.
5. Total time target: under 60 seconds from scan to displayed alert.
