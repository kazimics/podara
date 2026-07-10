package app.podara.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import app.podara.theme.DesignTokens
import java.awt.Cursor

@Composable
fun ToolbarPillButton(
    icon: ImageVector,
    label: String,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    minWidth: androidx.compose.ui.unit.Dp = DesignTokens.ToolbarButton.ManageMinWidth,
    iconColor: Color = DesignTokens.ToolbarButton.PillIconColor,
    hoverIconColor: Color = DesignTokens.ToolbarButton.PillHoverIconColor,
    textColor: Color = DesignTokens.ToolbarButton.PillTextColor,
    hoverTextColor: Color = DesignTokens.ToolbarButton.PillHoverTextColor,
    hoverBackgroundColor: Color = DesignTokens.ToolbarButton.PillHoverBackgroundColor,
    pressedBackgroundColor: Color = DesignTokens.ToolbarButton.PillPressedBackgroundColor,
    defaultBorderColor: Color = DesignTokens.ToolbarButton.PillDefaultBorderColor,
    hoverBorderColor: Color = DesignTokens.ToolbarButton.PillHoverBorderColor
) {
    val toolbarButton = DesignTokens.ToolbarButton
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()
    val shape = RoundedCornerShape(toolbarButton.PillRadius)
    val foregroundColor = if (isHovered || isPressed) hoverTextColor else textColor
    val iconTint = if (isHovered || isPressed) hoverIconColor else iconColor

    Box(
        modifier = modifier
            .height(toolbarButton.PillHeight)
            .widthIn(min = minWidth)
            .shadow(
                if (isHovered) toolbarButton.PillHoverShadowElevation else androidx.compose.ui.unit.Dp.Hairline,
                shape,
                ambientColor = toolbarButton.PillHoverShadowColor,
                spotColor = toolbarButton.PillHoverShadowColor
            )
            .clip(shape)
            .background(
                when {
                    isPressed -> pressedBackgroundColor
                    isHovered -> hoverBackgroundColor
                    else -> toolbarButton.PillDefaultBackgroundColor
                }
            )
            .border(
                toolbarButton.BorderWidth,
                if (isHovered || isPressed) hoverBorderColor else defaultBorderColor,
                shape
            )
            .pointerHoverIcon(PointerIcon(Cursor(Cursor.HAND_CURSOR)))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = toolbarButton.PillPaddingHorizontal),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = iconTint,
                modifier = Modifier.size(toolbarButton.PillIconSize)
            )
            Spacer(Modifier.width(toolbarButton.PillIconTextGap))
            Text(
                text = label,
                color = foregroundColor,
                fontSize = toolbarButton.PillTextSize,
                lineHeight = toolbarButton.PillLineHeight,
                fontWeight = toolbarButton.PillTextWeight
            )
        }
    }
}
