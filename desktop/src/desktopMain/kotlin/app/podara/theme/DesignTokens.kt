package app.podara.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object DesignTokens {

    // ── Spacing ──
    object Spacing {
        val xs = 4.dp
        val sm = 8.dp
        val md = 16.dp
        val lg = 24.dp
        val xl = 32.dp
    }

    // ── Common Border ──
    object Border {
        val Width = 1.dp
        val SecondaryColor = Color(0x14FFFFFF)
    }

    // ── Glass surfaces ──
    object Glass {
        val CompactRadius = 12.dp
        val CompactGradient = Brush.verticalGradient(
            colors = listOf(Color.White.copy(alpha = 0.085f), Color.White.copy(alpha = 0.035f), Color.White.copy(alpha = 0.018f))
        )
        val CompactBorderWidth = 0.6.dp
        val CompactBorderColor = Color.White.copy(alpha = 0.10f)
        val CompactShadowElevation = 8.dp
        val CompactShadowColor = Color.Black.copy(alpha = 0.30f)
        val HoverOverlayColor = Color.White.copy(alpha = 0.04f)
        val SelectedOverlayColor = Color(0x26E0B183)
        val SelectedBorderColor = Color(0x52E0B183)
    }

    // ── Toolbar icon buttons ──
    object ToolbarButton {
        val Size = 32.dp
        val Radius = 8.dp
        val IconSize = 16.dp
        val TextSize = 13.sp
        val StrongTextSize = 15.sp
        val Gap = 6.dp
        val BorderWidth = 0.6.dp
        val BorderColor = Color.White.copy(alpha = 0.10f)
        val BackgroundColor = Color.Transparent
        val HoverBackgroundColor = Color.White.copy(alpha = 0.07f)
        val DangerHoverBackgroundColor = Color(0x26FF5A5F)
        val PillHeight = 37.dp
        val PillRadius = 11.dp
        val PillPaddingHorizontal = 13.dp
        val PillIconTextGap = 7.dp
        val PillIconSize = 17.dp
        val PillTrailingIconSize = 15.dp
        val PillTextSize = 13.sp
        val PillLineHeight = 19.sp
        val PillTextWeight = FontWeight(450)
        val PillActiveTextWeight = FontWeight.Medium
        val ManageMinWidth = 78.dp
        val SortMinWidth = 139.dp
        val PillDefaultBackgroundColor = Color.Transparent
        val PillSortBackgroundColor = Color.White.copy(alpha = 0.02f)
        val PillHoverBackgroundColor = Color.White.copy(alpha = 0.06f)
        val PillPressedBackgroundColor = Color.White.copy(alpha = 0.10f)
        val PillSelectedBackgroundColor = Color(0x1FE0B183)
        val PillDefaultBorderColor = Color.White.copy(alpha = 0.08f)
        val PillSortBorderColor = Color.White.copy(alpha = 0.10f)
        val PillHoverBorderColor = Color.White.copy(alpha = 0.14f)
        val PillSelectedBorderColor = Color(0x73E0B183)
        val PillTextColor = Color(0xFFC4C6CD)
        val PillHoverTextColor = Color.White
        val PillSelectedTextColor = Color(0xFFE0B183)
        val PillIconColor = Color(0xFF9A9DA6)
        val PillHoverIconColor = Color.White
        val PillSelectedIconColor = Color(0xFFE0B183)
        val PillHoverShadowElevation = 4.dp
        val PillHoverShadowColor = Color.Black.copy(alpha = 0.25f)
    }

    // ── Dropdown menus ──
    object DropdownMenu {
        val Width = 192.dp
        val Radius = 14.dp
        val Padding = 7.dp
        val ItemHeight = 38.dp
        val ItemRadius = 9.dp
        val ItemPaddingHorizontal = 10.dp
        val ItemIconTextGap = 10.dp
        val ItemIconSize = 16.dp
        val OffsetY = 5.dp
        val ShadowElevation = 10.dp
        val ShadowColor = Color.Black.copy(alpha = 0.45f)
        val BackgroundColor = Color(0xFF1B1D22)
        val BorderColor = Color.White.copy(alpha = 0.08f)
        val HoverBackgroundColor = Color.White.copy(alpha = 0.06f)
        val SelectedBackgroundColor = Color(0x2EE0B183)
        val TextColor = Color(0xFFB8BBC4)
        val HoverTextColor = Color.White
        val SelectedTextColor = Color(0xFFE0B183)
        val IconColor = Color(0xFF8B8E97)
        val LabelSize = ToolbarButton.PillTextSize
        val LabelLineHeight = 18.sp
        val LabelWeight = FontWeight(450)
        val SelectedLabelWeight = FontWeight.Medium
        val DividerHeight = 1.dp
        val DividerColor = Color.White.copy(alpha = 0.06f)
        val DividerMarginVertical = 8.dp
        val EnterMs = 150
    }

    // ── Status badges ──
    object Badge {
        val Radius = 9.dp
        val PaddingHorizontal = 6.dp
        val PaddingVertical = 2.dp
        val TextSize = 11.sp
        val AccentBackgroundColor = Color(0x26E0B183)
        val AccentBorderColor = Color(0x40E0B183)
        val AccentTextColor = Color(0xFFE0B183)
    }

    // ── Empty states ──
    object EmptyState {
        val PanelWidth = 400.dp
        val PanelRadius = 18.dp
        val PanelPadding = 24.dp
        val IconSize = 56.dp
        val TitleSize = 20.sp
        val SubtitleSize = 14.sp
        val Gap = 12.dp
    }

    // ── Navigation active glass ──
    object Navigation {
        object ActiveGlass {
            val Radius = 13.dp
            val BaseColor = Color(0xFF211F1E)
            val LeftGlow = Brush.radialGradient(
                colors = listOf(Color(0x66C7924F), Color(0x22C7924F), Color.Transparent),
                center = Offset(8f, 42f),
                radius = 58f
            )
            val TopGlow = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.08f), Color.Transparent),
                center = Offset(142f, 0f),
                radius = 90f
            )
            val RightGlow = Brush.radialGradient(
                colors = listOf(Color(0x22D3A05F), Color.Transparent),
                center = Offset(204f, 4f),
                radius = 62f
            )
            val Border = Brush.linearGradient(
                colors = listOf(Color(0x60D3A05F), Color.White.copy(alpha = 0.06f), Color(0x1AD3A05F)),
                start = Offset(0f, 48f),
                end = Offset(200f, 0f)
            )
            val BorderWidth = 0.6.dp
            val ShadowElevation = 5.dp
            val ShadowColor = Color.Black.copy(alpha = 0.14f)
            val InnerPaddingHorizontal = 14.dp
        }
    }

    // ── Hero surfaces ──
    object Hero {
        val WaveformGold = Color(0xFFC88A35)
        val LeftAmbientGlowColors = listOf(Color(0xE8F4DEAA), Color(0x8CDEB66F), Color.Transparent)
        val LeftAmbientCenterFactor = Offset(-0.28f, 2.92f)
        val LeftAmbientRadiusFactor = 0.92f
        val CornerGlowColors = listOf(
            Color(0xB8F6DCA6),
            Color(0x8CDEAA62),
            Color(0x5CC9954C),
            Color(0x2EC9954C),
            Color(0x10C9954C),
            Color(0x04C9954C),
            Color.Transparent
        )
        val CornerGlowCenterFactor = Offset(1.06f, 1.12f)
        val CornerGlowRadiusFactor = 0.18f
    }

    // ── Button: primary ──
    object Button {
        val Height = 40.dp
        val Radius = 10.dp
        val IconSize = 20.dp
        val TextSize = 14.sp
        val PaddingHorizontal = 16.dp
        val Gradient = Brush.verticalGradient(
            colorStops = arrayOf(
                0.00f to Color(0xFFE8BE8D),
                0.32f to Color(0xFFC89363),
                0.62f to Color(0xFFAF7951),
                1.00f to Color(0xFF96623F)
            ),
            startY = 0f,
            endY = 48f
        )
        val InnerHighlight = Brush.verticalGradient(
            colors = listOf(Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.03f), Color.Transparent),
            startY = 0f,
            endY = 24f
        )
        val SpecularSheen = Brush.linearGradient(
            colors = listOf(Color.White.copy(alpha = 0.10f), Color.White.copy(alpha = 0.02f), Color.Transparent),
            start = Offset(0f, 0f),
            end = Offset(160f, 54f)
        )
        val BorderColor = Color.White.copy(alpha = 0.18f)
        val TextColor = Color(0xFFFFFBF5)
        val IconColor = Color.White
        val ShadowElevation = 10.dp
        val ShadowColor = Color.Black.copy(alpha = 0.24f)
    }

    // ── Button: icon (circular secondary) ──
    object IconButton {
        val Size = 40.dp
        val IconSize = 20.dp
    }

    // ── MiniPlayer ──
    object MiniPlayer {
        val Height = 88.dp
        val PaddingHorizontal = 20.dp
        object Speed { val Size = 36.dp; val TextSize = 14.sp }
        object RewindForward { val Size = 40.dp; val IconSize = 22.dp }
        object PlayPause { val Size = 56.dp; val IconSize = 26.dp }
        object Volume { val IconSize = 22.dp }
        object QueueFullscreen { val Size = 32.dp; val IconSize = 20.dp }
        object Time { val TextSize = 12.sp }
        object Slider { val Height = 20.dp }
    }

    // ── Sidebar ──
    object Sidebar {
        val Width = 240.dp
        val PaddingVertical = 20.dp
        val PaddingHorizontal = 20.dp
        val NavItemHeight = 48.dp
        val NavItemPadding = 12.dp
        val NavIconSize = 20.dp
        val NavTextSize = 14.sp
        val NavSpacing = 10.dp
        val LogoSize = 40.dp
        val LogoRadius = 10.dp
        val LogoIconSize = 18.dp
        val LogoTextSize = 20.sp
        val DividerPadding = 20.dp
    }

    // ── Search Bar ──
    object SearchBar {
        val Width = 320.dp
        val Height = 40.dp
        val Radius = 10.dp
        val PaddingHorizontal = 12.dp
        val IconSize = 16.dp
        val TextSize = 13.sp
        val Gap = 8.dp
        val ShortcutRadius = 5.dp
        val ShortcutTextSize = 11.sp
        val ShortcutPaddingH = 6.dp
        val ShortcutPaddingV = 2.dp
        val ClearIconSize = 14.dp
    }

    // ── Featured Card ──
    object FeaturedCard {
        val Height = 250.dp
        val Radius = 18.dp
        val Padding = 20.dp
        val CoverRadius = 12.dp
        val ContentGap = 24.dp
        val TextGap = 10.dp
        val ButtonGap = 10.dp
        val NavButtonSize = 40.dp
        val NavButtonGap = 6.dp
        val NavIconSize = 16.dp
        val NavPadding = 12.dp
        val ShadowElevation = 8.dp
        val ShadowColor = Color.Black.copy(alpha = 0.4f)
    }

    // ── Podcast Card ──
    object PodcastCard {
        val Width = 150.dp
        val ImageSize = 150.dp
        val ImageRadius = 14.dp
        val Spacing = 12.dp
        val Gap = 10.dp
        val TitleSize = 13.sp
        val AuthorSize = 11.sp
    }

    // ── Episode Row ──
    object EpisodeRow {
        val Height = 88.dp
        val PaddingHorizontal = 14.dp
        val PaddingVertical = 8.dp
        val LineGap = 2.dp
        val CoverSize = 64.dp
        val CoverRadius = 10.dp
        val Spacing = 14.dp
        val TitleSize = 14.sp
        val AuthorSize = 12.sp
        val DescSize = 11.sp
        val IconSize = 20.dp
    }

    // ── Subscription rows ──
    object SubscriptionRow {
        val Height = 96.dp
        val PaddingHorizontal = 14.dp
        val PaddingVertical = 12.dp
        val CoverSize = 64.dp
        val CoverRadius = 12.dp
        val Spacing = 14.dp
        val MetaGap = 6.dp
        val MetaEndPadding = 4.dp
        val TitleSize = 14.sp
        val AuthorSize = 12.sp
        val DescSize = 11.sp
        val LineGap = 2.dp
        val CheckboxSize = 24.dp
        val ActionButtonSize = 32.dp
        val ActionIconSize = 16.dp
    }

    // ── Queue Panel ──
    object QueuePanel {
        val Width = 320.dp
        val PaddingTop = 20.dp
        val PaddingHorizontal = 24.dp
        val RowHeight = 80.dp
        val CoverSize = 56.dp
        val CoverRadius = 8.dp
        val Spacing = 12.dp
        val TitleSize = 13.sp
        val HeaderTitleSize = 18.sp
        val ClearTextSize = 13.sp
        val DragHandleSize = 16.dp
        val ActiveCoverBadge = 20.dp
    }

    // ── Page Header ──
    object PageHeader {
        val TitleSize = 32.sp
        val SubtitleSize = 14.sp
        val PaddingHorizontal = 32.dp
        val PaddingTop = 28.dp
        val Gap = 4.dp
    }

    // ── Section Header ──
    object SectionHeader {
        val TitleSize = 20.sp
        val LinkSize = 13.sp
        val PaddingHorizontal = 32.dp
    }

    // ── Card Background ──
    object Card {
        val Gradient = Brush.linearGradient(
            colors = listOf(Color(0xFF1C1C1E), Color(0xFF15171B)),
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        )
    }

    // ── Animation durations ──
    object Animation {
        val HoverMs = 150
        val NormalMs = 300
        val SlowMs = 500
    }
}
