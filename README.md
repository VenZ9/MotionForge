# MotionForge 🎬✨
### Pro Mobile Motion Graphics & Video Compositor for Android

[![Build Android APK](https://github.com/VenZ9/MotionForge/actions/workflows/build-apk.yml/badge.svg)](https://github.com/VenZ9/MotionForge/actions/workflows/build-apk.yml)
[![Platform](https://img.shields.io/badge/Platform-Android_8.0+-3DDC84.svg?logo=android&logoColor=white)](https://android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF.svg?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack_Compose-Material_3-4285F4.svg?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Free Forever](https://img.shields.io/badge/Free_Forever-No_Watermarks-success.svg)](#free-forever)

**MotionForge** bridges the gap between **Alight Motion**'s intuitive touch-first mobile workflow and **Adobe After Effects**' desktop-grade compositing power. Built 100% from scratch in **Kotlin** and **Jetpack Compose**, MotionForge delivers real-time previews, hierarchical layer parenting, custom Bézier easing curves with an interactive graph editor, procedural mathematical expressions, and hardware-accelerated genuine **MP4 H.264** video rendering directly on your device.

---

## 🌟 Key Highlights & Feature Matrix

| Feature Category | Capabilities |
| :--- | :--- |
| **Multitrack Editing** | Unlimited video, image, audio, typography, vector shape, and adjustment layers with in/out trimming, split-at-playhead, duplication, solo/mute/lock toggles. |
| **Keyframe Animation** | Sub-frame precision keyframing for Position $(X, Y)$, Scale $(X, Y)$, Rotation, Opacity, and Anchor points with real-time diamond indicators on the timeline. |
| **Bézier Graph Editor** | Interactive curve canvas with draggable control handles ($CP_1, CP_2$) and velocity curves. Presets include Linear, Ease In, Ease Out, Ease In-Out, Snappy, and Overshoot. |
| **Parenting & Rigging** | Hierarchical parent-child linking. Child transforms inherit parent translation, rotation, and scaling recursively without cyclical lock. |
| **Visual Effects Engine** | **Chroma Key** (green/blue screen removal with tolerance & spill suppression), **Color Grading** (exposure, contrast, saturation, temperature, tint, vignette), **Glow** (luminous bloom), **Gaussian/Radial Blur**, **Camera Shake**, **RGB Split** (chromatic aberration), **Dynamic Particles** (stars, sparks, snow, dust, confetti, fire), and **Motion Blur**. |
| **Blending & Masking** | 13 blending modes (Normal, Multiply, Screen, Overlay, Darken, Lighten, Color Dodge, Hard Light, Difference, Add, etc.) and shapes/alpha masks with inversion. |
| **Motion Expressions** | Procedural math expressions: `wiggle(freq, amp)`, `time * speed`, `sin(time * freq) * amp`, `bounce`, and arithmetic formula evaluation. |
| **Text & Shape Motion** | Vector shape generation (Rectangles, Rounded Rects, Stars, Polygons, Triangles) and kinetic typography animators (Typewriter, Wave, Scale Bounce, Cyber Glitch). |
| **Hardware MP4 Exporter** | Genuine hardware-accelerated **H.264 / AVC** encoder via Android `MediaCodec` and `MediaMuxer`. Supports 720p, 1080p, and 4K export at 24, 30, and 60 FPS with live progress and cancel support. |
| **Workflow & Usability** | Non-destructive Undo/Redo history stack, project autosave debounce, safe margin guides (Action Safe 90%, Title Safe 80%), and preloaded demo projects. |

---

## 📱 User Interface Architecture

MotionForge provides an original, premium OLED dark theme designed specifically for modern mobile creators:

1. **Project Dashboard**:
   - Quick aspect ratio presets: 16:9 Landscape (1080p), 9:16 Shorts/TikTok/Reels (1080x1920), 1:1 Square, 4:5 Instagram, 21:9 Cinema, and 4K UHD.
   - Project cards with resolution badges, frame rates, layer counts, and duplicate/delete management.
   - Built-in starter templates (*Cyberpunk Neon Intro* and *Kinetic Typography*).

2. **Workstation Editor**:
   - **Top Bar**: Project title, live timecode display (`MM:SS:CS`), Undo/Redo actions, safe margins toggle, and Export trigger.
   - **Preview Canvas**: Dynamic aspect ratio container with interactive touch gizmos (drag to translate, corner scale, rotation), and safe margin guide overlays.
   - **Player Controls**: Frame-by-frame stepping (-1 / +1 frame), keyframe jumping, play/pause, split clip, duplicate layer, and layer addition menu.
   - **Multitrack Timeline**: Time ruler with sub-ticks, scrubbing playhead, color-coded layer strips, drag-to-shift timing, and keyframe diamond markers.
   - **Contextual Inspector**: Bottom tabs for Transform & Keyframes, Effects Stack, Layer Properties, Blending & Masks, Parenting, and Expressions.
   - **Graph Editor Modal**: Full-screen Bézier curve editor with draggable handles, velocity visualization, and live eased ball preview.
   - **Export Dialog**: Resolution selector, FPS selector, bitrate controls, live progress indicator, and direct Share/Open video intents.

---

## 🚀 Downloading and Installing the APK

Every push, pull request, and manual dispatch triggers an automated build in GitHub Actions that compiles the debug APK.

### Step-by-Step Installation:

1. Open the [MotionForge GitHub Actions Workflow](https://github.com/VenZ9/MotionForge/actions).
2. Click on the latest workflow run (e.g., **Build MotionForge APK**).
3. Scroll down to the **Artifacts** section at the bottom of the summary page.
4. Download the **`MotionForge-Debug-APK`** zip file.
5. Extract the `.zip` archive on your Android device to find `app-debug.apk`.
6. Tap the APK file and select **Install**. *(If prompted, enable "Install unknown apps" for your browser or file manager).*
7. Launch **MotionForge** from your app drawer!

---

## 🛠️ Building Locally

### Prerequisites:
- Android Studio Ladybug (2024.2+) or JDK 17+
- Android SDK 35 (Android 15) with Build Tools 35.0.0

### Build Commands:
```bash
# Clone the repository
git clone https://github.com/VenZ9/MotionForge.git
cd MotionForge

# Make gradlew executable
chmod +x gradlew

# Run unit tests
./gradlew test

# Assemble Debug APK
./gradlew assembleDebug

# Output APK path:
# app/build/outputs/apk/debug/app-debug.apk
```

---

## 💎 Free Forever Pledge

MotionForge is dedicated to free, accessible creative tools:
- 🚫 **No Subscriptions**
- 🚫 **No Premium Feature Tiers**
- 🚫 **No Watermarks on Export**
- 🚫 **No Export Resolution/FPS Limits**
- 🚫 **No Paid Third-Party APIs**

---

## 📄 License

This project is licensed under the [MIT License](LICENSE).
