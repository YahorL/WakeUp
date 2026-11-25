package com.wakeup.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wakeup.ui.theme.Wake
import com.wakeup.ui.theme.pressable

/** The full-width gradient call-to-action: 60dp pill with a coloured glow beneath it. */
@Composable
fun PrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leading: WakeIcon? = null,
) {
    val colors = Wake.colors
    Row(
        modifier
            .fillMaxWidth()
            .height(60.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .pressable(enabled = enabled, onClick = onClick)
            .glow(colors.orb2.copy(alpha = 0.7f), blur = 44.dp, spread = (-14).dp, offsetY = 20.dp)
            .surface(CircleShape, colors.fill, border = white(0.22f), highlight = white(0.35f)),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            WakeIcon(leading, size = 20.dp, stroke = 2f)
            Spacer(Modifier.width(10.dp))
        }
        Label(label, 17f, weight = FontWeight.Medium, tracking = 0.01f, maxLines = 1)
    }
}

/** The near-white button used for "Continue to math" and "Done". */
@Composable
fun WhitePillButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 56.dp,
    shape: Shape = CircleShape,
    textSize: Float = 16f,
    trailing: WakeIcon? = null,
) {
    val colors = Wake.colors
    Row(
        modifier
            .height(height)
            .pressable(onClick = onClick)
            .glow(colors.orb2.copy(alpha = 0.6f), blur = 30.dp, spread = (-14).dp, offsetY = 14.dp, radius = 20.dp)
            .clip(shape)
            .background(colors.whitePill)
            .padding(horizontal = 30.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Label(label, textSize, weight = FontWeight.Medium, color = colors.onLight, maxLines = 1)
        if (trailing != null) {
            Spacer(Modifier.width(10.dp))
            WakeIcon(trailing, size = 18.dp, color = colors.onLight, stroke = 2f)
        }
    }
}

/** 44dp round glass button around a line icon. */
@Composable
fun GlassIconButton(
    icon: WakeIcon,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Box(
        modifier
            .size(44.dp)
            .alpha(if (enabled) 1f else 0.35f)
            .semantics { this.contentDescription = contentDescription }
            .pressable(enabled = enabled, onClick = onClick)
            .glass(GlassRecipe.CONTROL, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        WakeIcon(icon, size = 20.dp, stroke = 1.7f)
    }
}

/** The square icon tile heading a challenge: gradient + glow when [active], neutral otherwise. */
@Composable
fun ChallengeIconTile(
    icon: WakeIcon,
    active: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    animated: Boolean = false,
) {
    val colors = Wake.colors
    val shape = RoundedCornerShape(size / 3)
    val pop = if (animated) {
        Modifier.popOnToggle(active, onFrom = 0.8f, onPeak = 1.12f, offDip = 0.86f, onMillis = 600, offMillis = 350, tilt = true)
    } else Modifier
    Box(
        modifier
            .size(size)
            .then(pop)
            .then(
                if (active) {
                    Modifier
                        .glow(colors.orb2.copy(alpha = 0.7f), blur = 24.dp, spread = (-10).dp, offsetY = 10.dp, radius = size / 3)
                        .surface(shape, colors.fill, border = white(0f), highlight = white(0.3f))
                } else {
                    Modifier.clip(shape).background(white(0.10f))
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        WakeIcon(icon, size = size * 0.46f, stroke = 1.8f)
    }
}
