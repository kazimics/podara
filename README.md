![podium](/images/title_light.png#gh-light-mode-only)
![podium](/images/title_dark.png#gh-dark-mode-only)
---
![GitHub](https://img.shields.io/github/license/kazimics/podium-windows?style=for-the-badge)
![GitHub release (latest by date)](https://img.shields.io/github/v/release/kazimics/podium-windows?style=for-the-badge)
![GitHub downloads](https://img.shields.io/github/downloads/kazimics/podium-windows/total?style=for-the-badge)

English | [中文](README_zh.md)

**podium-windows** is a modern, open-source Podcast app for **Windows**, written in Kotlin using Compose Multiplatform.
The app uses **Material 3** design and audio playback powered by **libmpv**.

> [!NOTE]
> This is the 1.0.0-alpha release. Features are mostly complete, but you may still encounter edge-case bugs.
> Please [open an issue](https://github.com/kazimics/podium-windows/issues) if you find one!

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

## Download

Download the latest installer from the [Releases page](https://github.com/kazimics/podium-windows/releases).

| Format | Description |
|--------|-------------|
| **MSI** | Windows Installer — installs and registers in Add/Remove Programs |
| **EXE** | Portable executable — run directly without installation |

## Building from Source

### Prerequisites
- JDK 17+

### Build Commands
```bash
# Run the app directly
./gradlew :desktop:run

# Build MSI installer
./gradlew :desktop:packageMsi

# Build EXE installer
./gradlew :desktop:packageExe

# Run tests
./gradlew :desktop:desktopTest
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
