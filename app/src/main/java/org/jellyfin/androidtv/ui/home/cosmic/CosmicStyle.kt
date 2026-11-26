package org.jellyfin.androidtv.ui.home.cosmic

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*
 * The look of the Cosmic web home (quickHome.scss), measured on a TV: the web page there is 960 CSS pixels wide,
 * the same as the 960 dp of a TV screen, so CSS pixels are used as dp and rem as 16 sp.
 */
object CosmicStyle {
	val background = Color(0xFF080810)
	val ink = Color.White
	val soft = Color.White.copy(alpha = 0.68f)
	val line = Color.White.copy(alpha = 0.1f)
	val night = Color(0xFF0B0F1E)
	val shade = Color(0xFF060916)
	val star = Color(0xFFFFCA28)
	val cardTop = Color(0xFF1A2036)
	val cardBottom = Color(0xFF11152A)
	val focusGlow = Color(0x7300A4DC)

	val gutter = 31.dp
	val card = 172.dp
	val wide = 300.dp
	val cardGap = 14.dp

	val rowTitle = 21.sp
	val small = 12.sp

	/** The colour of each kind of library, and the icon of its block. */
	fun accentOf(collectionType: String?, name: String): Color = when (collectionType) {
		"movies" -> Color(0xFF00A4DC)
		"tvshows" -> Color(0xFFAA5CC3)
		"music" -> Color(0xFFEC407A)
		"books" -> Color(0xFFFFB300)
		"homevideos" -> Color(0xFF26A69A)
		"musicvideos" -> Color(0xFFEF5350)
		"photos" -> Color(0xFF66BB6A)
		else -> hashColor(name)
	}

	fun iconOf(collectionType: String?): String = when (collectionType) {
		"movies" -> "🎬"
		"tvshows" -> "📺"
		"music" -> "🎵"
		"books" -> "📚"
		"homevideos" -> "📹"
		"musicvideos" -> "🎤"
		"photos" -> "🖼️"
		else -> "✨"
	}

	/** The same colour the web gives a name (genres, libraries without a type). */
	fun hashColor(text: String): Color {
		var hash = 0
		for (char in text) hash = ((hash * 31) + char.code) % 360
		return Color.hsl(hash.toFloat(), 0.62f, 0.42f)
	}
}

/** Whether the element that uses [interactionSource] has the focus of the remote. */
@Composable
fun rememberFocused(interactionSource: MutableInteractionSource): Boolean {
	val focused by interactionSource.collectIsFocusedAsState()
	return focused
}

/** Grows the element when it has the focus, like the web on a TV; [fromBottom] keeps its title in place. */
@Composable
fun Modifier.cosmicGrow(focused: Boolean, scale: Float = 1.06f, fromBottom: Boolean = false): Modifier {
	val value by animateFloatAsState(if (focused) scale else 1f, tween(150), label = "grow")
	return graphicsLayer {
		scaleX = value
		scaleY = value
		if (fromBottom) transformOrigin = TransformOrigin(0.5f, 1f)
	}
}

/**
 * The focus of the web on a TV: a thick white ring 3 to 4 dp outside the element, over a blue glow of 10 dp.
 * Goes before any clip, so it is drawn outside the element.
 */
fun Modifier.cosmicRing(focused: Boolean, radius: Dp, offset: Dp = 3.dp, round: Boolean = false): Modifier = drawWithContent {
	drawContent()
	if (!focused) return@drawWithContent

	fun ring(inset: Float, width: Float, color: Color) {
		val r = if (round) size.minDimension / 2 + inset else radius.toPx() + inset
		drawRoundRect(
			color = color,
			topLeft = Offset(-inset, -inset),
			size = Size(size.width + inset * 2, size.height + inset * 2),
			cornerRadius = CornerRadius(r, r),
			style = Stroke(width),
		)
	}

	val glow = 10.dp.toPx()
	ring(glow / 2, glow, CosmicStyle.focusGlow)
	val line = 4.dp.toPx()
	ring(offset.toPx() + line / 2, line, Color.White)
}
