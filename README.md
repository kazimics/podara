![podium](/images/title_light.png#gh-light-mode-only)
![podium](/images/title_dark.png#gh-dark-mode-only)
---
![GitHub](https://img.shields.io/github/license/aimok04/podium?style=for-the-badge) ![GitHub release (latest by date)](https://img.shields.io/github/v/release/aimok04/podium?style=for-the-badge)

English | [中文](README_zh.md)

**podium-windows** is a modern, open-source Podcast app for **Windows**, written in Kotlin using Compose Multiplatform.
The app uses **Material 3** design and audio playback powered by **libmpv**.

This project is a Windows desktop port of [aimok04/podium](https://github.com/aimok04/podium), an Android podcast app.

> [!CAUTION]
> Keep in mind that *podium-windows* is work in progress software.
> It is far from feature complete and can contain bugs.

> [!NOTE]
> *podium-windows* is still lacking some essential features.
> Please open up an issue if you have any ideas!

## Notable Features

- **Custom UI** — Immersive window with custom title bar, sidebar navigation, and premium gold gradient buttons.
- **Download & offline management** — download episodes with pause/resume support, manage downloaded content, view storage usage and file paths, single or batch delete, **speed limit per task**.
- **Discover** new podcasts on the *Discover* tab *(powered by Apple Podcasts)* with featured cards and trending sections.
- **Playback controls** - play/pause, seek, playback speed adjustment, 10s skip.
- **Sleep timer** - automatically pause after a set duration.
- **Queue management** — build and manage your playback queue with cover art, **drag-to-reorder** with real-time animation.
- **OPML import/export** - transfer your subscriptions between apps.
- **History** - track your listening history.
- **Subscription management** — unsubscribe from podcasts individually or in batch, **sort by name / update / listen time**.
- **Design system** — centralized design tokens for consistent spacing, colors, and typography.

## Installation

1. Clone the repository:
   ```
   git clone https://github.com/aimok04/podium.git
   cd podium-windows
   ```

2. Build and install:
   ```
   set JAVA_HOME="C:\Program Files\Microsoft\jdk-17.0.19.10-hotspot"
   gradlew.bat :desktop:packageMsi
   ```

3. Run the MSI installer from `desktop/build/compose/binaries/main/msi/`

## Building from Source

### Prerequisites
- JDK 17+

### Build Commands
```bash
# Run the app directly
gradlew.bat :desktop:run

# Build MSI installer
gradlew.bat :desktop:packageMsi

# Run tests
gradlew.bat :desktop:desktopTest
```

## Tech Stack

| Layer | Technology |
|-------|-----------|
| UI | Compose Multiplatform + Material 3 |
| Audio | libmpv (via JNA) |
| Image Loading | Coil 3 |
| Database | JDBC SQLite |
| Network | Ktor |
| RSS Parsing | rssparser |
| Serialization | kotlinx.serialization |
| Settings | Multiplatform Settings |

## Project Structure

```
podium-windows/
├── desktop/                   # Windows desktop entry point
│   ├── build.gradle.kts
│   └── src/
│       ├── desktopMain/kotlin/app/podiumpodcasts/podium/
│       │   ├── App.kt            # Main application composable
│       │   ├── Main.kt           # Entry point
│       │   ├── screen/           # UI screens
│       │   ├── player/           # Audio engine & player UI
│       │   ├── data/             # Database & data models
│       │   ├── api/              # Network API clients
│       │   ├── manager/          # Business logic
│       │   ├── theme/            # Design system
│       │   └── util/             # Utilities
│       └── desktopTest/          # Test classes
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── libs/                        # mpv + JNA native libraries
```

## License

[GNU General Public License v3.0](/LICENSE)
