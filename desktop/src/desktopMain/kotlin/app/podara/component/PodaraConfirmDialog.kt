package app.podara.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import app.podara.theme.DesignTokens
import java.awt.Cursor

enum class PodaraDialogActionStyle { Primary, Secondary, Destructive, Text }

enum class PodaraDialogSize { Compact, Standard, Wide }

@Composable
fun PodaraDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    size: PodaraDialogSize = PodaraDialogSize.Standard,
    minHeight: Dp = 0.dp,
    header: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
    actions: (@Composable RowScope.() -> Unit)? = null
) {
    val container = DesignTokens.Dialog.Container
    val motion = DesignTokens.Dialog.Motion
    val width = when (size) {
        PodaraDialogSize.Compact -> container.CompactWidth
        PodaraDialogSize.Standard -> container.StandardWidth
        PodaraDialogSize.Wide -> container.WideWidth
    }
    val shape = RoundedCornerShape(container.Radius)
    val density = LocalDensity.current
    val easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    Dialog(onDismissRequest = onDismissRequest) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(tween(motion.EnterDurationMs, easing = easing)) +
                scaleIn(
                    animationSpec = tween(motion.EnterDurationMs, easing = easing),
                    initialScale = motion.EnterStartScale
                ) +
                slideInVertically(
                    initialOffsetY = { with(density) { motion.EnterStartTranslationY.roundToPx() } },
                    animationSpec = tween(motion.EnterDurationMs, easing = easing)
                )
        ) {
            Column(
                modifier = modifier
                    .requiredWidth(width)
                    .heightIn(min = minHeight)
                    .shadow(
                        container.ShadowElevation,
                        shape,
                        ambientColor = container.ShadowColor,
                        spotColor = container.ShadowColor
                    )
                    .clip(shape)
                    .background(container.Background)
                    .border(container.BorderWidth, container.BorderColor, shape)
                    .padding(
                        start = container.PaddingHorizontal,
                        top = container.PaddingTop,
                        end = container.PaddingHorizontal,
                        bottom = container.PaddingBottom
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(container.ContentGap)
            ) {
                header?.invoke()
                title?.invoke()
                content()
                actions?.let {
                    Row(
                        modifier = Modifier.padding(top = DesignTokens.Dialog.Action.MarginTop),
                        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Dialog.Action.Gap),
                        verticalAlignment = Alignment.CenterVertically,
                        content = it
                    )
                }
            }
        }
    }
}

@Composable
fun PodaraDialogTitle(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Center
) {
    val typography = DesignTokens.Dialog.Typography
    Text(
        text = text,
        modifier = modifier.fillMaxWidth(),
        color = typography.TitleColor,
        fontSize = typography.TitleSize,
        lineHeight = typography.TitleLineHeight,
        fontWeight = typography.TitleWeight,
        textAlign = textAlign
    )
}

@Composable
fun PodaraDialogBody(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Start,
    color: Color = DesignTokens.Dialog.Typography.BodyColor
) {
    val typography = DesignTokens.Dialog.Typography
    Text(
        text = text,
        modifier = modifier.fillMaxWidth(),
        color = color,
        fontSize = typography.BodySize,
        lineHeight = typography.BodyLineHeight,
        textAlign = textAlign
    )
}

@Composable
fun PodaraDialogActionButton(
    label: String,
    onClick: () -> Unit,
    style: PodaraDialogActionStyle,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val tokens = DesignTokens.Dialog
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()
    val shape = RoundedCornerShape(DesignTokens.Dialog.Action.Radius)
    val stateBackground = when (style) {
        PodaraDialogActionStyle.Primary -> DesignTokens.Button.Gradient
        PodaraDialogActionStyle.Secondary -> Brush.verticalGradient(List(2) {
            when {
                isPressed -> DesignTokens.Dialog.Action.Secondary.PressedBackground
                isHovered -> DesignTokens.Dialog.Action.Secondary.HoverBackground
                else -> DesignTokens.Dialog.Action.Secondary.Background
            }
        })
        PodaraDialogActionStyle.Destructive -> Brush.verticalGradient(List(2) {
            when {
                isPressed -> DesignTokens.Dialog.Action.Destructive.PressedBackground
                isHovered -> DesignTokens.Dialog.Action.Destructive.HoverBackground
                else -> DesignTokens.Dialog.Action.Destructive.Background
            }
        })
        PodaraDialogActionStyle.Text -> Brush.verticalGradient(List(2) {
            when {
                isPressed -> DesignTokens.Dialog.Action.Text.PressedBackground
                isHovered -> DesignTokens.Dialog.Action.Text.HoverBackground
                else -> Color.Transparent
            }
        })
    }
    val textColor = when (style) {
        PodaraDialogActionStyle.Primary -> DesignTokens.Button.TextColor
        PodaraDialogActionStyle.Secondary -> DesignTokens.Dialog.Action.Secondary.TextColor
        PodaraDialogActionStyle.Destructive -> DesignTokens.Dialog.Action.Destructive.TextColor
        PodaraDialogActionStyle.Text -> DesignTokens.Dialog.Action.Text.TextColor
    }
    val shadowColor = when (style) {
        PodaraDialogActionStyle.Primary -> DesignTokens.Button.ShadowColor
        PodaraDialogActionStyle.Destructive -> DesignTokens.Dialog.Action.Destructive.ShadowColor
        else -> Color.Transparent
    }
    val shadowElevation = when (style) {
        PodaraDialogActionStyle.Primary, PodaraDialogActionStyle.Destructive -> DesignTokens.Button.ShadowElevation
        else -> 0.dp
    }
    val borderColor = if (style == PodaraDialogActionStyle.Primary) DesignTokens.Button.BorderColor else Color.Transparent

    Box(
        modifier = modifier
            .requiredWidth(DesignTokens.Dialog.Action.Width)
            .height(DesignTokens.Dialog.Action.Height)
            .shadow(
                elevation = if (isHovered && enabled) shadowElevation else 0.dp,
                shape = shape,
                ambientColor = shadowColor,
                spotColor = shadowColor
            )
            .clip(shape)
            .background(brush = stateBackground, alpha = if (enabled) 1f else DesignTokens.Dialog.Action.DisabledBackgroundAlpha)
            .border(DesignTokens.Border.Width, borderColor, shape)
            .pointerHoverIcon(PointerIcon(Cursor(Cursor.HAND_CURSOR)))
            .clickable(enabled = enabled, interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (style == PodaraDialogActionStyle.Primary) {
            Box(Modifier.matchParentSize().background(DesignTokens.Button.InnerHighlight))
            Box(Modifier.matchParentSize().background(DesignTokens.Button.SpecularSheen))
        }
        Text(
            text = label,
            color = if (enabled) textColor else textColor.copy(alpha = DesignTokens.Dialog.Action.DisabledContentAlpha),
            fontSize = DesignTokens.Dialog.Action.LabelSize,
            fontWeight = DesignTokens.Dialog.Action.LabelWeight
        )
    }
}

@Composable
fun PodaraConfirmDialog(
    title: String,
    description: AnnotatedString,
    confirmLabel: String,
    dismissLabel: String,
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.WarningAmber,
    confirmStyle: PodaraDialogActionStyle = PodaraDialogActionStyle.Destructive
) {
    val tokens = DesignTokens.Dialog
    PodaraDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        header = {
            Box(
                modifier = Modifier
                    .size(DesignTokens.Dialog.Icon.ContainerSize)
                    .clip(RoundedCornerShape(DesignTokens.Dialog.Icon.ContainerRadius))
                    .background(DesignTokens.Dialog.Icon.ContainerBackground)
                    .border(DesignTokens.Dialog.Icon.ContainerBorderWidth, DesignTokens.Dialog.Icon.ContainerBorderColor, RoundedCornerShape(DesignTokens.Dialog.Icon.ContainerRadius)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = DesignTokens.Dialog.Icon.Color, modifier = Modifier.size(DesignTokens.Dialog.Icon.Size))
            }
        },
        title = { PodaraDialogTitle(title) },
        content = {
            Text(
                text = description,
                modifier = Modifier.fillMaxWidth(),
                color = DesignTokens.Dialog.Typography.BodyColor,
                fontSize = DesignTokens.Dialog.Typography.BodySize,
                lineHeight = DesignTokens.Dialog.Typography.BodyLineHeight,
                textAlign = TextAlign.Center
            )
        },
        actions = {
            PodaraDialogActionButton(dismissLabel, onDismissRequest, PodaraDialogActionStyle.Secondary)
            PodaraDialogActionButton(confirmLabel, onConfirm, confirmStyle)
        }
    )
}
