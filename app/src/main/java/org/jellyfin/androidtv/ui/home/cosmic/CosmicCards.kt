package org.jellyfin.androidtv.ui.home.cosmic

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import org.koin.compose.koinInject

/* ---------- Basics ---------- */

@Composable
fun CosmicText(
	text: String,
	modifier: Modifier = Modifier,
	size: TextUnit = 14.sp,
	weight: FontWeight = FontWeight.Normal,
	color: Color = CosmicStyle.ink,
	maxLines: Int = Int.MAX_VALUE,
	lineHeight: TextUnit = TextUnit.Unspecified,
	letterSpacing: TextUnit = TextUnit.Unspecified,
	align: TextAlign = TextAlign.Start,
) = BasicText(
	text = text,
	modifier = modifier,
	style = TextStyle(
		color = color,
		fontSize = size,
		fontWeight = weight,
		lineHeight = lineHeight,
		letterSpacing = letterSpacing,
		textAlign = align,
	),
	maxLines = maxLines,
	overflow = TextOverflow.Ellipsis,
)

/** A picture of the server that fades in; when one fails the next of [fallbacks] is tried. */
@Composable
fun CosmicImage(
	url: String?,
	modifier: Modifier = Modifier,
	contentScale: ContentScale = ContentScale.Crop,
	alignment: Alignment = Alignment.Center,
	fallbacks: List<String> = emptyList(),
	fadeMillis: Int = 300,
) {
	val sources = remember(url, fallbacks) { listOfNotNull(url) + fallbacks }
	var index by remember(sources) { mutableIntStateOf(0) }
	val source = sources.getOrNull(index) ?: return
	val context = LocalContext.current
	val imageLoader = koinInject<ImageLoader>()
	val request = remember(source, fadeMillis) {
		ImageRequest.Builder(context).data(source).crossfade(fadeMillis).build()
	}

	AsyncImage(
		model = request,
		contentDescription = null,
		imageLoader = imageLoader,
		modifier = modifier,
		contentScale = contentScale,
		alignment = alignment,
		onError = { if (index < sources.lastIndex) index++ },
	)
}

private val posterBrush = Brush.linearGradient(listOf(CosmicStyle.cardTop, CosmicStyle.cardBottom))

/** The rounded picture of a card: gradient while it loads and a soft shadow. */
private fun Modifier.posterFrame(shape: Shape): Modifier = this
	.shadow(8.dp, shape)
	.clip(shape)
	.background(posterBrush)

/** A pill button of the banner and of "what do we watch today": white for the main one. */
@Composable
fun CosmicButton(
	text: String,
	onClick: () -> Unit,
	modifier: Modifier = Modifier,
	main: Boolean = false,
	textColor: Color? = null,
	borderColor: Color? = null,
	focusRequester: FocusRequester? = null,
	onFocused: () -> Unit = {},
) {
	val interaction = remember { MutableInteractionSource() }
	val focused = rememberFocused(interaction)
	LaunchedEffect(focused) { if (focused) onFocused() }
	val shape = RoundedCornerShape(99.dp)
	val background = when {
		main && focused -> Color(0xFFE8ECFF)
		main -> Color.White
		focused -> Color.White.copy(alpha = 0.18f)
		else -> Color.White.copy(alpha = 0.08f)
	}

	Box(
		modifier = modifier
			.let { if (focusRequester != null) it.focusRequester(focusRequester) else it }
			.cosmicGrow(focused)
			.cosmicRing(focused, 99.dp, offset = 4.dp)
			.clip(shape)
			.background(background)
			.border(1.dp, if (main) Color.Transparent else borderColor ?: Color.White.copy(alpha = 0.35f), shape)
			.clickable(interactionSource = interaction, indication = null, onClick = onClick)
			.padding(horizontal = 30.dp, vertical = 14.dp),
	) {
		CosmicText(
			text = text,
			size = 18.sp,
			weight = FontWeight.ExtraBold,
			color = textColor ?: if (main) CosmicStyle.night else Color.White,
			maxLines = 1,
		)
	}
}

/** "★ 7.8" in yellow, the year and up to three genres, in soft white. */
@Composable
fun CosmicMeta(rating: Float?, year: Int?, genres: List<String>?, modifier: Modifier = Modifier) {
	Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
		if (rating != null && rating > 0) CosmicText("★ %.1f".format(rating), size = 14.sp, weight = FontWeight.Bold, color = CosmicStyle.star)
		if (year != null && year > 0) CosmicText(year.toString(), size = 14.sp, weight = FontWeight.Bold, color = CosmicStyle.soft)
		genres.orEmpty().take(3).forEach { CosmicText(it, size = 14.sp, weight = FontWeight.Bold, color = CosmicStyle.soft, maxLines = 1) }
	}
}

/* ---------- Cards ---------- */

enum class CosmicShape { Poster, Square, Round, Wide }

private val SQUARE_TYPES = setOf("AudioBook", "MusicAlbum", "Audio", "MusicArtist")

fun cosmicShapeOf(shape: String, type: String): CosmicShape = when (shape) {
	"round" -> CosmicShape.Round
	"square" -> CosmicShape.Square
	"wide" -> CosmicShape.Wide
	else -> if (type in SQUARE_TYPES) CosmicShape.Square else CosmicShape.Poster
}

/**
 * A card of a row: poster (2:3), square, round (artists) or wide (16:9), with its rating, "on air", progress or
 * finished mark, and the title and year under it. On focus the picture grows upwards with the white ring.
 */
@Composable
fun CosmicCard(
	imageUrl: String?,
	title: String,
	subtitle: String?,
	shape: CosmicShape,
	accent: Color,
	onClick: () -> Unit,
	modifier: Modifier = Modifier,
	rating: Float? = null,
	onAir: Boolean = false,
	progress: Double? = null,
	done: Boolean = false,
	onAirLabel: String = "",
) {
	val interaction = remember { MutableInteractionSource() }
	val focused = rememberFocused(interaction)
	val width = if (shape == CosmicShape.Wide) CosmicStyle.wide else CosmicStyle.card
	val round = shape == CosmicShape.Round
	val pictureShape = if (round) CircleShape else RoundedCornerShape(14.dp)
	val ratio = when (shape) {
		CosmicShape.Poster -> 2f / 3f
		CosmicShape.Wide -> 16f / 9f
		else -> 1f
	}

	Column(
		modifier = modifier
			.width(width)
			.clickable(interactionSource = interaction, indication = null, onClick = onClick),
		verticalArrangement = Arrangement.spacedBy(4.dp),
	) {
		Box(
			modifier = Modifier
				.fillMaxWidth()
				.aspectRatio(ratio)
				.cosmicGrow(focused, 1.08f, fromBottom = true)
				.cosmicRing(focused, 14.dp, round = round)
				.posterFrame(pictureShape),
		) {
			CosmicImage(imageUrl, Modifier.fillMaxSize())
			if (rating != null && rating > 0 && !round) {
				CosmicBadge("★ %.1f".format(rating), CosmicStyle.star, Color(0xC7060916), Modifier.align(Alignment.TopEnd))
			}
			if (onAir) {
				CosmicBadge(onAirLabel, Color.White, Color(0xFFE53935), Modifier.align(Alignment.BottomStart))
			}
			if (done) {
				Box(
					modifier = Modifier
						.padding(8.dp)
						.size(26.dp)
						.shadow(4.dp, CircleShape)
						.background(Color(0xFF2EC4B6), CircleShape),
					contentAlignment = Alignment.Center,
				) { CosmicText("✓", size = 13.sp, weight = FontWeight.Black, color = CosmicStyle.night) }
			} else if (progress != null && progress > 0) {
				CosmicProgress(progress.toFloat(), accent, Modifier.align(Alignment.BottomCenter))
			}
		}

		val align = if (round) TextAlign.Center else TextAlign.Start
		CosmicText(
			text = title,
			modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
			size = 13.8.sp,
			weight = FontWeight.ExtraBold,
			maxLines = 2,
			lineHeight = 17.sp,
			align = align,
		)
		if (!subtitle.isNullOrBlank()) {
			CosmicText(subtitle, Modifier.fillMaxWidth(), 12.sp, FontWeight.SemiBold, CosmicStyle.soft, maxLines = 1, align = align)
		}
	}
}

@Composable
private fun CosmicBadge(text: String, color: Color, background: Color, modifier: Modifier = Modifier) {
	CosmicText(
		text = text,
		modifier = modifier
			.padding(8.dp)
			.background(background, RoundedCornerShape(8.dp))
			.padding(horizontal = 7.dp, vertical = 2.dp),
		size = 11.5.sp,
		weight = FontWeight.ExtraBold,
		color = color,
		maxLines = 1,
	)
}

/** The thin bar of what was watched, at the bottom of a picture. [value] goes from 0 to 1. */
@Composable
fun CosmicProgress(value: Float, accent: Color, modifier: Modifier = Modifier) {
	Box(
		modifier = modifier
			.padding(start = 10.dp, end = 10.dp, bottom = 8.dp)
			.fillMaxWidth()
			.height(4.dp)
			.clip(RoundedCornerShape(2.dp))
			.background(Color.White.copy(alpha = 0.25f)),
	) {
		Box(Modifier.fillMaxHeight().fillMaxWidth(value.coerceIn(0f, 1f)).background(accent))
	}
}

/** Top 10: the big outlined number next to the poster, in the colour of the library. */
@Composable
fun CosmicRankCard(
	rank: Int,
	imageUrl: String?,
	rating: Float?,
	accent: Color,
	onClick: () -> Unit,
	modifier: Modifier = Modifier,
) {
	val interaction = remember { MutableInteractionSource() }
	val focused = rememberFocused(interaction)
	val numberSize = (CosmicStyle.card.value * 0.78f).sp

	Row(
		modifier = modifier
			.width(CosmicStyle.card * 1.42f)
			.clickable(interactionSource = interaction, indication = null, onClick = onClick),
		verticalAlignment = Alignment.Bottom,
	) {
		Box(
			modifier = Modifier
				.weight(1f)
				.offset(x = 10.dp),
			contentAlignment = Alignment.BottomEnd,
		) {
			val style = TextStyle(
				fontSize = numberSize,
				fontWeight = FontWeight.Black,
				letterSpacing = (-0.08).em,
				lineHeight = numberSize * 0.8f,
				textAlign = TextAlign.End,
			)
			// The outline first and the dark fill over it, like "paint-order: stroke" on the web
			BasicText(rank.toString(), style = style.copy(color = accent, drawStyle = Stroke(width = 12f, join = StrokeJoin.Round)), maxLines = 1)
			BasicText(rank.toString(), style = style.copy(color = CosmicStyle.night), maxLines = 1)
		}
		Box(
			modifier = Modifier
				.width(CosmicStyle.card)
				.aspectRatio(2f / 3f)
				.cosmicGrow(focused, 1.08f, fromBottom = true)
				.cosmicRing(focused, 14.dp)
				.posterFrame(RoundedCornerShape(14.dp)),
		) {
			CosmicImage(imageUrl, Modifier.fillMaxSize())
			if (rating != null && rating > 0) {
				CosmicBadge("★ %.1f".format(rating), CosmicStyle.star, Color(0xC7060916), Modifier.align(Alignment.TopEnd))
			}
		}
	}
}

/**
 * Continue watching, next up and upcoming releases: a landscape picture with a play circle on focus, the progress
 * and the title and what is left under it. Square covers (audiobooks, songs) are shown whole over a blurred copy.
 */
@Composable
fun CosmicWideCard(
	imageUrl: String?,
	title: String,
	subtitle: String?,
	accent: Color,
	onClick: () -> Unit,
	modifier: Modifier = Modifier,
	progress: Float = 0f,
	cover: Boolean = false,
	showPlay: Boolean = true,
	dateLabel: String? = null,
	fallbacks: List<String> = emptyList(),
) {
	val interaction = remember { MutableInteractionSource() }
	val focused = rememberFocused(interaction)
	val zoom by animateFloatAsState(if (focused) 1.04f else 1f, tween(300), label = "zoom")
	val play by animateFloatAsState(if (focused) 1f else 0f, tween(200), label = "play")
	val shape = RoundedCornerShape(14.dp)

	Column(
		modifier = modifier
			.width(CosmicStyle.wide)
			.cosmicGrow(focused)
			.clickable(interactionSource = interaction, indication = null, onClick = onClick),
		verticalArrangement = Arrangement.spacedBy(2.dp),
	) {
		Box(
			modifier = Modifier
				.fillMaxWidth()
				.aspectRatio(16f / 9f)
				.cosmicRing(focused, 14.dp, offset = 4.dp)
				.posterFrame(shape),
		) {
			if (cover) {
				CosmicImage(imageUrl, Modifier.fillMaxSize().scale(1.2f).blur(18.dp).alpha(0.55f))
				CosmicImage(imageUrl, Modifier.fillMaxSize().scale(zoom), contentScale = ContentScale.Fit)
			} else {
				CosmicImage(imageUrl, Modifier.fillMaxSize().scale(zoom), fallbacks = fallbacks)
			}

			if (showPlay) {
				Box(
					modifier = Modifier
						.align(Alignment.Center)
						.size(48.dp)
						.scale(0.85f + 0.15f * play)
						.alpha(play)
						.background(Color.White.copy(alpha = 0.92f), CircleShape),
					contentAlignment = Alignment.Center,
				) { CosmicText("▶", size = 17.sp, color = CosmicStyle.night) }
			}

			if (progress > 0) CosmicProgress(progress, accent, Modifier.align(Alignment.BottomCenter))

			if (dateLabel != null) {
				CosmicText(
					text = dateLabel,
					modifier = Modifier
						.align(Alignment.BottomStart)
						.padding(10.dp)
						.background(Color(0xFF26C6DA), RoundedCornerShape(99.dp))
						.padding(horizontal = 10.dp, vertical = 3.dp),
					size = 12.sp,
					weight = FontWeight.ExtraBold,
					color = CosmicStyle.night,
					maxLines = 1,
				)
			}
		}
		CosmicText(title, Modifier.fillMaxWidth().padding(top = 4.dp), 13.8.sp, FontWeight.ExtraBold, maxLines = 2, lineHeight = 17.sp)
		if (!subtitle.isNullOrBlank()) CosmicText(subtitle, Modifier.fillMaxWidth(), 12.sp, FontWeight.SemiBold, CosmicStyle.soft, maxLines = 1)
	}
}

/** A genre of "browse by genre": a coloured tile with its name and how many titles it has. */
@Composable
fun CosmicGenreTile(name: String, count: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
	val interaction = remember { MutableInteractionSource() }
	val focused = rememberFocused(interaction)
	val color = CosmicStyle.hashColor(name)
	val shape = RoundedCornerShape(16.dp)

	Row(
		modifier = modifier
			.widthIn(min = 150.dp)
			.cosmicGrow(focused)
			.cosmicRing(focused, 16.dp, offset = 4.dp)
			.clip(shape)
			.background(Brush.linearGradient(listOf(color, lerp(color, Color(0xE60A0C1C), 1f / 1.3f))))
			.clickable(interactionSource = interaction, indication = null, onClick = onClick)
			.padding(horizontal = 16.dp, vertical = 14.dp),
		horizontalArrangement = Arrangement.SpaceBetween,
		verticalAlignment = Alignment.Bottom,
	) {
		CosmicText(name, size = 15.sp, weight = FontWeight.Black, maxLines = 1)
		Spacer(Modifier.width(10.dp))
		CosmicText(count, size = 12.sp, weight = FontWeight.Bold, color = Color.White.copy(alpha = 0.7f), maxLines = 1)
	}
}

/** A library under the banner: a pill in its colour, with its icon. */
@Composable
fun CosmicChip(text: String, color: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
	val interaction = remember { MutableInteractionSource() }
	val focused = rememberFocused(interaction)
	val shape = RoundedCornerShape(99.dp)

	CosmicText(
		text = text,
		modifier = modifier
			.cosmicGrow(focused)
			.cosmicRing(focused, 99.dp, offset = 4.dp)
			.clip(shape)
			.background(color.copy(alpha = if (focused) 0.38f else 0.18f))
			.border(1.dp, color.copy(alpha = 0.55f), shape)
			.clickable(interactionSource = interaction, indication = null, onClick = onClick)
			.padding(horizontal = 16.dp, vertical = 8.dp),
		size = 13.6.sp,
		weight = FontWeight.ExtraBold,
		maxLines = 1,
	)
}

/** "Ver todo ›" in the colour of the row or block. */
@Composable
fun CosmicMoreLink(text: String, color: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
	val interaction = remember { MutableInteractionSource() }
	val focused = rememberFocused(interaction)
	CosmicText(
		text = "$text ›",
		modifier = modifier
			.cosmicGrow(focused)
			.cosmicRing(focused, 8.dp, offset = 4.dp)
			.clickable(interactionSource = interaction, indication = null, onClick = onClick)
			.padding(horizontal = 4.dp, vertical = 2.dp),
		size = 13.6.sp,
		weight = FontWeight.ExtraBold,
		color = color,
		maxLines = 1,
	)
}

/** Space of [size] both ways, for the gaps between the pieces. */
@Composable
fun Gap(size: Dp) = Spacer(Modifier.size(size))
