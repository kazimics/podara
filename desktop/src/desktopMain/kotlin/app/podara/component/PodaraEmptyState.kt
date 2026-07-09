package app.podara.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import app.podara.theme.DesignTokens
import app.podara.theme.PodaraTheme
import java.awt.Cursor

@Composable
fun PodaraEmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    iconTint: Color = PodaraTheme.colors.textMuted,
    actionText: String? = null,
    actionIcon: ImageVector? = null,
    onActionClick: (() -> Unit)? = null
) {
    val colors = PodaraTheme.colors
    val empty = DesignTokens.EmptyState
    val glass = DesignTokens.Glass
    val button = DesignTokens.Button

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .width(empty.PanelWidth)
                .shadow(
                    glass.CompactShadowElevation,
                    RoundedCornerShape(empty.PanelRadius),
                    ambientColor = glass.CompactShadowColor,
                    spotColor = glass.CompactShadowColor
                )
                .clip(RoundedCornerShape(empty.PanelRadius))
                .background(glass.CompactGradient)
                .border(glass.CompactBorderWidth, glass.CompactBorderColor, RoundedCornerShape(empty.PanelRadius))
                .padding(empty.PanelPadding),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(empty.IconSize), tint = iconTint)
                Spacer(modifier = Modifier.height(empty.Gap))
                Text(title, fontSize = empty.TitleSize, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                Spacer(modifier = Modifier.height(DesignTokens.Spacing.xs))
                Text(subtitle, fontSize = empty.SubtitleSize, color = colors.textSecondary)

                if (actionText != null && onActionClick != null) {
                    Spacer(modifier = Modifier.height(DesignTokens.Spacing.md))
                    Box(
                        modifier = Modifier
                            .height(button.Height)
                            .shadow(button.ShadowElevation, RoundedCornerShape(button.Radius), ambientColor = button.ShadowColor, spotColor = button.ShadowColor)
                            .clip(RoundedCornerShape(button.Radius))
                            .border(DesignTokens.Border.Width, button.BorderColor, RoundedCornerShape(button.Radius))
                            .background(button.Gradient)
                            .pointerHoverIcon(PointerIcon(Cursor(Cursor.HAND_CURSOR)))
                            .clickable { onActionClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(modifier = Modifier.matchParentSize().background(button.InnerHighlight))
                        Box(modifier = Modifier.matchParentSize().background(button.SpecularSheen))
                        Row(
                            modifier = Modifier.padding(horizontal = button.PaddingHorizontal),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (actionIcon != null) {
                                Icon(actionIcon, contentDescription = null, modifier = Modifier.size(button.IconSize), tint = button.IconColor)
                                Spacer(modifier = Modifier.width(DesignTokens.Spacing.sm))
                            }
                            Text(actionText, color = button.TextColor, fontSize = button.TextSize, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}
