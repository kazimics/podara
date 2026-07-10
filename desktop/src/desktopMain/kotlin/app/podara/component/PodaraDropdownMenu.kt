package app.podara.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import app.podara.theme.DesignTokens
import java.awt.Cursor

data class PodaraDropdownMenuItem(
    val label: String,
    val icon: ImageVector? = null,
    val isSelected: Boolean = false,
    val onClick: () -> Unit
)

@Composable
fun PodaraDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    items: List<PodaraDropdownMenuItem>,
    modifier: Modifier = Modifier,
    width: Dp = DesignTokens.DropdownMenu.Width,
    offset: DpOffset = DpOffset(x = 0.dp, y = DesignTokens.DropdownMenu.OffsetY)
) {
    val dropdownMenu = DesignTokens.DropdownMenu
    val shape = RoundedCornerShape(dropdownMenu.Radius)

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        offset = offset,
        modifier = modifier
            .width(width)
            .shadow(
                dropdownMenu.ShadowElevation,
                shape,
                ambientColor = dropdownMenu.ShadowColor,
                spotColor = dropdownMenu.ShadowColor
            )
            .clip(shape)
            .background(dropdownMenu.BackgroundColor)
            .border(DesignTokens.Border.Width, dropdownMenu.BorderColor, shape)
            .padding(dropdownMenu.Padding),
        containerColor = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        items.forEach { item ->
            val interactionSource = remember(item) { MutableInteractionSource() }
            val isHovered by interactionSource.collectIsHoveredAsState()
            val backgroundColor by animateColorAsState(
                targetValue = when {
                    item.isSelected -> dropdownMenu.SelectedBackgroundColor
                    isHovered -> dropdownMenu.HoverBackgroundColor
                    else -> Color.Transparent
                },
                animationSpec = tween(DesignTokens.Animation.HoverMs)
            )
            val textColor = when {
                item.isSelected -> dropdownMenu.SelectedTextColor
                isHovered -> dropdownMenu.HoverTextColor
                else -> dropdownMenu.TextColor
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dropdownMenu.ItemHeight)
                    .clip(RoundedCornerShape(dropdownMenu.ItemRadius))
                    .background(backgroundColor)
                    .pointerHoverIcon(PointerIcon(Cursor(Cursor.HAND_CURSOR)))
                    .clickable(interactionSource = interactionSource, indication = null, onClick = item.onClick)
                    .padding(horizontal = dropdownMenu.ItemPaddingHorizontal),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item.icon?.let { icon ->
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (item.isSelected) dropdownMenu.SelectedTextColor else dropdownMenu.IconColor,
                        modifier = Modifier.size(dropdownMenu.ItemIconSize)
                    )
                    Spacer(modifier = Modifier.width(dropdownMenu.ItemIconTextGap))
                }
                Text(
                    text = item.label,
                    color = textColor,
                    fontSize = dropdownMenu.LabelSize,
                    lineHeight = dropdownMenu.LabelLineHeight,
                    fontWeight = if (item.isSelected) dropdownMenu.SelectedLabelWeight else dropdownMenu.LabelWeight,
                    modifier = Modifier.weight(1f)
                )
                if (item.isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = dropdownMenu.SelectedTextColor,
                        modifier = Modifier.size(dropdownMenu.ItemIconSize)
                    )
                }
            }
        }
    }
}
