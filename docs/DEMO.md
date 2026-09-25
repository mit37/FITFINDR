# DEMO.md — recording a demo

This is a script for Mitansh to follow to record a demo GIF/video of
FitFindr himself. No agent in this session has run the app on a device or
recorded anything — this is a script to follow, not a claim that a demo
exists.

## What can honestly be demoed right now (milestones 1–3)

As of this session, the app runs entirely against `FakeVlmEngine` — it does
not run a real model, and does not have a working camera preview or history
screen. A demo recorded today would show:

1. The app launching to the Capture screen.
2. Tapping "Pick from gallery" and picking any photo (the Android Photo
   Picker — the photo's actual content does not matter, since
   `FakeVlmEngine` ignores it).
3. Navigating to the Result screen, showing the same fixed fixture outfit
   result every time (style label "smart casual", three garments, a
   palette).
4. Tapping into the History and Settings placeholder screens, which each
   show a one-line "coming in milestone N" message.

This is a legitimate demo of the navigation/UI skeleton — it is **not** a
demo of on-device AI inference, and should not be captioned as one. Caption
it honestly, e.g. "FitFindr v2, early build — UI skeleton against a fake
inference engine, real on-device model coming in a later milestone."

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
