# DEMO.md — recording a demo

This is a script for Mitansh to follow to record a demo GIF/video of
FitFindr himself. No agent in this session has run the app on a device or
recorded anything — this is a script to follow, not a claim that a demo
exists.

## What can honestly be demoed right now (milestones 1–7)

As of this session, the app runs entirely against `FakeVlmEngine` — it does
not run a real model, and does not have a working live camera preview. A
demo recorded today would show:

1. The app launching to the Capture screen.
2. Tapping "Pick from gallery" and picking any photo (the Android Photo
   Picker — the photo's actual content does not matter, since
   `FakeVlmEngine` ignores it).
3. Navigating to the Result screen, showing the same fixed fixture outfit
   result every time (style label "smart casual", three garments, a
   palette), plus a working "Share as image" button that opens the real
   Android share sheet with a rendered PNG.
4. Opening History, showing the outfit that was just saved (real Room
   persistence now — milestone 6), tapping it to expand, and deleting it.
5. Opening Settings, showing the real model download UI (start/cancel/
   delete, progress bar) — tapping "Download model" will fail, honestly,
   since `ModelConfig.DOWNLOAD_URL` is still a placeholder (no real model
   is hosted yet, see `docs/PLAN.md`) — and the "Active accelerator" line,
   which honestly reads "Not applicable — running against FakeVlmEngine"
   because `MediaPipeVlmEngine` (milestone 5) is not the bound engine yet.

This is a legitimate demo of the navigation/UI/persistence/share skeleton
— it is **not** a demo of on-device AI inference or a real model download,
and should not be captioned as one. Caption it honestly, e.g. "FitFindr
v2, early build — UI, history and share-as-image against a fake inference
engine; real on-device model download/inference verified on a future
device session."

## Prerequisites

- A physical Android device (API 26+) or emulator, with the Android SDK
  set up (`sdkmanager`, `adb` on `PATH`).
- This repo checked out, on the branch/commit you want to demo.
- A screen recording tool: `adb shell screenrecord` (built into Android,
  no extra install) is the simplest option; a scrcpy-based capture on your
  desktop also works if you want a lower-friction file to convert to GIF.

## Steps

1. Build and install the debug APK:
   ```bash
   ./gradlew installDebug
   ```
2. Start recording, either:
   - On-device: `adb shell screenrecord /sdcard/fitfindr-demo.mp4`
     (Ctrl+C to stop, then `adb pull /sdcard/fitfindr-demo.mp4`), or
   - Desktop: mirror the device with `scrcpy` and record that window with
     your OS's usual screen recorder.
3. Open FitFindr from the launcher.
4. Tap "Pick from gallery," choose any photo.
5. On the Result screen, let it sit for a couple of seconds so the recording
   clearly shows the style label, palette, and garment cards.
6. Tap "Done" to go back, then open History and Settings briefly so the
   recording shows their current (placeholder) state honestly.
7. Stop the recording.

## Converting to GIF

```bash
# Requires ffmpeg
ffmpeg -i fitfindr-demo.mp4 -vf "fps=12,scale=480:-1:flags=lanczos" -c:v gif fitfindr-demo.gif
```

Trim to the shortest clip that shows the real flow — a demo GIF works best
under ~15 seconds.

## Where it goes

Once milestone 8 (release) actually happens, this demo (or a fresher one
covering the real model, once milestone 5 lands) belongs in the README next
to real screenshots. Until then, do not add a demo GIF to the README that
implies more is working than the "Status" table there says is working.
