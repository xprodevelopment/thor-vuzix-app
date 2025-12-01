# Thor AI - Vuzix Blade 2 Native App

Native Android app for Vuzix Blade 2 that connects to Thor AI service.

## Features
- Direct WebSocket connection to Thor (ws://192.168.1.207:8765)
- Simple HUD interface with status indicator
- Microphone button for voice commands
- Displays AI responses in real-time
- No HTTPS/certificate issues (uses native sockets)

## Building the App

### Option 1: Build on a Linux/Mac/Windows machine with Android SDK

1. **Install Android Studio** or just Android SDK command-line tools
2. **Navigate to project:**
   ```bash
   cd /home/xproadmin/AGIPROJECT/vuzix-thor-stream/vuzix-app
   ```
3. **Build APK:**
   ```bash
   ./gradlew assembleDebug
   ```
4. **Output:** `app/build/outputs/apk/debug/app-debug.apk`

### Option 2: Use pre-built Docker container

```bash
docker run --rm -v $(pwd):/project mingc/android-build-box bash -c "cd /project && ./gradlew assembleDebug"
```

### Option 3: Transfer to x86 machine and build there

Since Thor is ARM64 and Android SDK doesn't run natively here, transfer the `vuzix-app/` folder to any x86 machine with Android Studio.

## Installing on Vuzix

```bash
adb connect 192.168.1.34:5555
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Running the App

1. Ensure Thor AI service is running:
   ```bash
   python3 /home/xproadmin/AGIPROJECT/vuzix-thor-stream/thor-service/ai_service.py
   ```

2. Launch app on Vuzix (or it auto-launches if installed)

3. App will connect to `ws://192.168.1.207:8765`

4. Tap microphone button to send voice commands

## Troubleshooting

- **App won't connect:** Verify Thor AI service is running on port 8765
- **WebSocket error:** Check network connectivity between Vuzix and Thor
- **Permissions denied:** Grant camera/microphone permissions in Android settings

## Next Steps

- Add speech recognition for voice input
- Add camera streaming to Thor
- Improve UI for Vuzix display
- Add gesture controls
