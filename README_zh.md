![Podara](/images/title_light.png#gh-light-mode-only)
![Podara](/images/title_dark.png#gh-dark-mode-only)
---
![GitHub](https://img.shields.io/github/license/kazimics/podara?style=for-the-badge)
![GitHub release (latest by date)](https://img.shields.io/github/v/release/kazimics/podara?style=for-the-badge)
![GitHub downloads](https://img.shields.io/github/downloads/kazimics/podara/total?style=for-the-badge)

[English](README.md) | 中文

**Podara** 是一个现代化的开源播客应用，面向 **Windows** 平台，使用 Kotlin 和 Compose Multiplatform 开发。

> [!CAUTION]
> 请注意，*Podara* 仍属于正在开发中的软件，功能远未完善，可能存在漏洞。


> [!NOTE]
> *Podara* 目前仍缺少一些核心功能
> 如果您有相关建议，欢迎提交问题！:)

## 主要功能

- **下载与离线管理** — 支持暂停/恢复下载、管理已下载内容、查看存储占用与文件路径、单条或批量删除，**支持每任务下载限速**。
- **发现** 新播客 — 在 *Discover* 标签页浏览 *(由 Apple Podcasts 提供支持)*，包含精选卡片和热门推荐。
- **睡眠定时器** — 设定时间后自动暂停播放。
- **队列管理** — 创建和管理你的播放队列，支持封面显示，**支持拖拽排序及实时动画**。
- **OPML 导入/导出** — 在不同应用之间迁移订阅。
- **播放历史** — 记录你的收听历史。
- **订阅管理** — 单个或批量取消订阅播客，**支持按名称/最近更新/最近收听排序**。

## 下载

从 [Releases 页面](https://github.com/kazimics/podara/releases) 下载最新安装包。

## 从源码构建

### 环境要求
- JDK 17+

### 构建命令
```bash
# 直接运行应用
./gradlew :desktop:run

# 构建 MSI 安装包
./gradlew :desktop:packageMsi

# 构建 EXE 安装包
./gradlew :desktop:packageExe

# 运行测试
./gradlew :desktop:desktopTest
```

## 技术栈

| 层级 | 技术 |
|------|------|
| UI | Compose Multiplatform + Material 3 |
| 音频播放 | libmpv (通过 JNA) |
| 图片加载 | Coil 3 |
| 数据库 | JDBC SQLite |
| 网络 | Ktor |
| RSS 解析 | rssparser |
| 序列化 | kotlinx.serialization |
| 设置管理 | Multiplatform Settings |

## 项目结构

```
podara/
├── desktop/                   # Windows 桌面入口
│   ├── build.gradle.kts
│   └── src/
│       ├── desktopMain/kotlin/app/podara/
│       │   ├── App.kt            # 主应用组合
│       │   ├── Main.kt           # 入口
│       │   ├── screen/           # UI 页面
│       │   ├── player/           # 音频引擎 & 播放器 UI
│       │   ├── data/             # 数据库 & 数据模型
│       │   ├── api/              # 网络 API 客户端
│       │   ├── manager/          # 业务逻辑
│       │   ├── theme/            # 设计系统
│       │   └── util/             # 工具类
│       └── desktopTest/          # 测试
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── libs/                        # mpv + JNA 本地库
```

## 许可证

[GNU General Public License v3.0](/LICENSE)

## 致谢

感谢 [podium](https://github.com/aimok04/podium) 项目带来的灵感和基础思路，为本项目提供了宝贵的参考。

感谢 [LINUX DO](https://linux.do/) 社区的支持与鼓励。
