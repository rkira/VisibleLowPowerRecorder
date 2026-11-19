# Visible Low-Power Recorder

A minimal Android CameraX sample with a persistent recording notification and a Stop action.

## Build on GitHub
1. Create a GitHub repository.
2. Upload all files in this folder (including `.github/workflows/build.yml`).
3. Push to the `main` branch, or open Actions and run **Build Android APK**.
4. Download the `visible-low-power-recorder-debug` artifact from the workflow run.

## Notes
- Default capture quality is CameraX `Quality.HD` (720p where supported); actual device selection is camera-dependent.
- CameraX uses Android's hardware video encoder when available.
- The app uses a foreground camera/microphone service and a persistent notification with a Stop action.
- Android/Realme may stop or restrict camera capture when the screen locks or the app leaves the foreground. Screen-off operation is not guaranteed across devices or OS versions.
- This sample intentionally has visible recording status; use only with the consent and authorization of everyone being recorded.
- Debug APKs are not Play Store release builds.
