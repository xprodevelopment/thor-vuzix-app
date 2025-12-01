# Thor Vuzix AI Assistant

Native Android app for Vuzix Blade 2 AR glasses with WebSocket connection to Thor AI service.

## Quick Start

### 1. Download APK

**GitHub Actions (Recommended):**
https://github.com/xprodevelopment/thor-vuzix-app/actions

1. Click latest "Build Android APK" workflow (✅ green checkmark)
2. Download artifact: `thor-vuzix-debug-apk.zip`
3. Unzip to get `app-debug.apk`

### 2. Install on Vuzix

```bash
# Transfer APK to Thor (if downloaded elsewhere)
scp app-debug.apk xproadmin@192.168.1.207:~/

# On Thor - Install to Vuzix
adb connect 192.168.1.34:5555
adb install -r app-debug.apk
```

### 3. Launch App

```bash
adb shell am start -n com.thor.vuzix/.MainActivity
```

The app should connect to Thor AI at `ws://192.168.1.207:8765` automatically.

---

## Architecture

```
Vuzix Blade 2 (192.168.1.34)
    ↓ WebSocket (JSON)
Thor AI Service (192.168.1.207:8765)
    ↓ HTTP API
Triton + vLLM (localhost:8000)
```

**Message Format:**
```json
{
  "title": "THOR AI",
  "body": "Response text here...",
  "timestamp": "2025-12-01T01:52:09.658116"
}
```

---

## Thor AI Service

The WebSocket server that handles Vuzix connections is located at:
`/home/xproadmin/AGIPROJECT/vuzix-thor-stream/thor_ai_service.py`

### Service Control

```bash
cd /home/xproadmin/AGIPROJECT/vuzix-thor-stream

./thor_service.sh start      # Start service
./thor_service.sh stop       # Stop service
./thor_service.sh restart    # Restart service
./thor_service.sh status     # Check status
./thor_service.sh test       # Test WebSocket connection
./thor_service.sh logs       # View logs (live tail)
```

### Service Status

```bash
# Check if running
ps aux | grep thor_ai_service

# Check port
netstat -ln | grep 8765

# View logs
tail -f /tmp/thor_ai_service.log
```

---

## Development

### Rebuild APK

**Using GitHub Actions:**
```bash
cd vuzix-app
git add .
git commit -m "Update"
git push
```

Wait 5-10 minutes, then download from GitHub Actions artifacts.

**Local Build (requires Android SDK):**
```bash
cd vuzix-app
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

### Testing

**View Vuzix app logs:**
```bash
adb logcat | grep -E "(Thor|MainActivity|WebSocket)"
```

**View Thor AI service logs:**
```bash
tail -f /tmp/thor_ai_service.log
```

**Test WebSocket manually:**
```python
cd /home/xproadmin/AGIPROJECT/vuzix-thor-stream
./thor_service.sh test
```

---

## Project Structure

```
vuzix-app/
├── .github/workflows/
│   └── build-apk.yml              # GitHub Actions CI/CD
├── app/
│   ├── src/main/
│   │   ├── java/com/thor/vuzix/
│   │   │   ├── MainActivity.java           # Main UI & lifecycle
│   │   │   └── ThorWebSocketClient.java    # WebSocket client
│   │   ├── res/
│   │   │   └── layout/activity_main.xml    # UI layout
│   │   └── AndroidManifest.xml
│   └── build.gradle
├── gradle/wrapper/
│   └── gradle-wrapper.properties
├── gradlew
└── README.md
```

---

## Network Configuration

| Device | IP Address | Port | Service |
|--------|------------|------|---------|
| Thor AGX | 192.168.1.207 | 8765 | WebSocket server |
| Thor AGX | 192.168.1.207 | 8000 | Triton inference |
| Vuzix Blade 2 | 192.168.1.34 | 5555 | ADB |

---

## Troubleshooting

### App Shows "DISCONNECTED"

**Check Thor AI service:**
```bash
./thor_service.sh status
./thor_service.sh restart
```

**Check network:**
```bash
# Test connectivity
adb shell ping 192.168.1.207

# Check firewall
sudo ufw allow 8765
```

### App Crashes

```bash
# View crash logs
adb logcat -b crash

# Reinstall
adb uninstall com.thor.vuzix
adb install -r app-debug.apk
```

### Can't Connect via ADB

```bash
# Reconnect
adb disconnect
adb connect 192.168.1.34:5555
adb devices
```

---

## Next Steps

- [ ] Integrate Triton LLM for actual AI responses
- [ ] Add speech recognition (SpeechRecognizer)
- [ ] Add camera streaming to Thor
- [ ] Add text-to-speech responses
- [ ] Add gesture controls
- [ ] Add settings UI

---

## Files

- `MainActivity.java` - Main app UI, WebSocket connection management
- `ThorWebSocketClient.java` - WebSocket client implementation
- `thor_ai_service.py` - Thor-side WebSocket server (in parent directory)
- `thor_service.sh` - Service control script (in parent directory)

---

**Status:** ✅ Working - Thor AI Service running, ready for connections
