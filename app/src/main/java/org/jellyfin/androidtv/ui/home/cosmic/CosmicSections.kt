package org.jellyfin.androidtv.ui.home.cosmic

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.data.cosmic.CosmicHomeBlock
import org.jellyfin.androidtv.data.cosmic.CosmicHomeCard
import org.jellyfin.androidtv.data.cosmic.CosmicHomeRepository
import org.jellyfin.androidtv.data.cosmic.CosmicShelf
import org.jellyfin.sdk.model.api.ImageType

/** Every 9 s the banner moves to the next title, unless the remote was used in the last 10 s. */
private const val HERO_DELAY = 9_000L
private const val HERO_IDLE_RESUME = 10_000L

/* ---------- Rows ---------- */

/** The title of a row: big and bold, after a short glowing bar in the colour of the row. */
@Composable
fun CosmicRowHeader(
	title: String,
	accent: Color,
	inset: Dp,
	hint: String? = null,
	more: (() -> Unit)? = null,
) {
	Row(
		modifier = Modifier
			.fillMaxWidth()
			.padding(horizontal = inset),
		verticalAlignment = Alignment.CenterVertically,
		horizontalArrangement = Arrangement.spacedBy(12.dp),
	) {
		Row(modifier = Modifier.height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
			Box(
				modifier = Modifier
					.padding(vertical = 4.dp)
					.width(5.dp)
					.fillMaxHeight()
					.shadow(6.dp, RoundedCornerShape(3.dp), ambientColor = accent, spotColor = accent)
					.background(accent, RoundedCornerShape(3.dp)),
			)
			Spacer(Modifier.width(9.dp))
			CosmicText(title, size = CosmicStyle.rowTitle, weight = FontWeight.Black, maxLines = 1, letterSpacing = (-0.01).em)
		}
		if (hint != null) CosmicText(hint, size = 12.8.sp, weight = FontWeight.SemiBold, color = CosmicStyle.soft, maxLines = 1)
		if (more != null) {
			Spacer(Modifier.weight(1f))
			CosmicMoreLink(stringResource(R.string.cosmic_see_all), accent, more)
		}
	}
}

/** A row that scrolls sideways; coming back to it puts the focus on the card that had it. */
@Composable
fun <T> CosmicRow(
	title: String,
	accent: Color,
	items: List<T>,
	key: (T) -> Any,
	inset: Dp = CosmicStyle.gutter,
	hint: String? = null,
	bottom: Dp = 30.dp,
	content: @Composable (index: Int, item: T) -> Unit,
) {
	if (items.isEmpty()) return

	Column(modifier = Modifier.padding(bottom = bottom)) {
		CosmicRowHeader(title, accent, inset, hint)
		LazyRow(
			modifier = Modifier.focusRestorer(),
			// Room for the grown card, its ring and its glow
			contentPadding = PaddingValues(start = inset, end = inset, top = 40.dp, bottom = 26.dp),
			horizontalArrangement = Arrangement.spacedBy(CosmicStyle.cardGap),
			verticalAlignment = Alignment.Bottom,
		) {
			itemsIndexed(items, key = { _, item -> key(item) }) { index, item -> content(index, item) }
		}
	}
}

/** A row prepared by the server (Top 10, gems, classics, a genre…), with the cards in the shape it asks for. */
@Composable
fun CosmicShelfRow(
	shelf: CosmicShelf,
	accent: Color,
	repository: CosmicHomeRepository,
	actions: CosmicHomeActions,
	inset: Dp = CosmicStyle.gutter,
	bottom: Dp = 30.dp,
) {
	val rank = shelf.shape == "rank"
	CosmicRow(
		title = shelfTitle(shelf.key),
		accent = accent,
		items = shelf.items,
		key = { it.id },
		inset = inset,
		hint = if (rank) stringResource(R.string.cosmic_top_hint) else null,
		bottom = bottom,
	) { index, card ->
		if (rank) {
			CosmicRankCard(
				rank = index + 1,
				imageUrl = repository.imageUrl(card.id, ImageType.PRIMARY, card.primaryTag, 360),
				rating = card.rating,
				accent = accent,
				onClick = { actions.open(card.id) },
			)
		} else {
			CosmicHomeCardView(card, cosmicShapeOf(shelf.shape, card.type), accent, repository, actions)
		}
	}
}

@Composable
fun CosmicHomeCardView(
	card: CosmicHomeCard,
	shape: CosmicShape,
	accent: Color,
	repository: CosmicHomeRepository,
	actions: CosmicHomeActions,
) {
	val wide = shape == CosmicShape.Wide
	CosmicCard(
		imageUrl = repository.imageUrl(card.id, ImageType.PRIMARY, card.primaryTag, if (wide) 720 else 360),
		title = card.name,
		subtitle = card.subtitle ?: card.year?.toString(),
		shape = shape,
		accent = accent,
		onClick = { actions.open(card.id) },
		rating = card.rating,
		onAir = card.note == "Continuing",
		onAirLabel = stringResource(R.string.cosmic_on_air),
		progress = card.progress,
		done = card.done,
	)
}

@Composable
private fun shelfTitle(key: String): String {
	if (key.startsWith("genre:")) return key.removePrefix("genre:")
	return stringResource(
		when (key) {
			"top10movies" -> R.string.cosmic_row_top10movies
			"top10series" -> R.string.cosmic_row_top10series
			"latestmovies" -> R.string.cosmic_row_latestmovies
			"releases" -> R.string.cosmic_row_releases
			"toprated" -> R.string.cosmic_row_toprated
			"gems" -> R.string.cosmic_row_gems
			"classics" -> R.string.cosmic_row_classics
			"newepisodes" -> R.string.cosmic_row_newepisodes
			"newseries" -> R.string.cosmic_row_newseries
			"onair" -> R.string.cosmic_row_onair
			"marathon" -> R.string.cosmic_row_marathon
			"movies" -> R.string.cosmic_row_movies
			"newalbums" -> R.string.cosmic_row_newalbums
			"artists" -> R.string.cosmic_row_artists
			"discoveralbums" -> R.string.cosmic_row_discoveralbums
			"mostplayed" -> R.string.cosmic_row_mostplayed
			"recentplayed" -> R.string.cosmic_row_recentplayed
			"latestbooks" -> R.string.cosmic_row_latestbooks
			"discoverbooks" -> R.string.cosmic_row_discoverbooks
			"discovervideos" -> R.string.cosmic_row_discovervideos
			else -> R.string.cosmic_row_latest
		}
	)
}

@Composable
private fun noteLabel(card: CosmicHomeCard): String? = when (card.note) {
	"NewSeries" -> stringResource(R.string.cosmic_new_series)
	"NewEpisodes" -> {
		val count = card.noteCount ?: 0
		if (count > 1) stringResource(R.string.cosmic_new_episodes, count) else stringResource(R.string.cosmic_new_episode)
	}
	"Movie" -> stringResource(R.string.cosmic_new_movie)
	else -> null
}

/* ---------- Banner ---------- */

/**
 * The banner on top, edge to edge and under the toolbar: the backdrop fades in and slowly zooms out, the logo (or
 * the title), rating, year, genres and overview rise in, and the small posters of the other titles sit at the
 * bottom. It turns by itself every 9 s while the remote is not being used.
 */
@Composable
fun CosmicHero(
	items: List<CosmicHomeCard>,
	repository: CosmicHomeRepository,
	actions: CosmicHomeActions,
	lastInteraction: MutableLongState,
	playFocusRequester: FocusRequester,
	onFocused: () -> Unit,
) {
	if (items.isEmpty()) return
	var index by remember(items) { mutableIntStateOf(0) }
	val item = items[index % items.size]

	LaunchedEffect(items) {
		while (items.size > 1) {
			delay(HERO_DELAY)
			if (System.currentTimeMillis() - lastInteraction.longValue >= HERO_IDLE_RESUME) index = (index + 1) % items.size
		}
	}

	Box(
		modifier = Modifier
			.fillMaxWidth()
			.height(420.dp),
		contentAlignment = Alignment.BottomStart,
	) {
		// The picture fades from one title to the next and slowly zooms out, fading away at the bottom
		Crossfade(targetState = item, animationSpec = tween(700), label = "hero") { shown ->
			val zoom = remember(shown.id) { Animatable(1.08f) }
			LaunchedEffect(shown.id) { zoom.animateTo(1f, tween(14_000, easing = LinearOutSlowInEasing)) }
			CosmicImage(
				url = repository.imageUrl(shown.id, ImageType.BACKDROP, shown.backdropTag, 1920),
				modifier = Modifier
					.fillMaxSize()
					.graphicsLayer {
						scaleX = zoom.value
						scaleY = zoom.value
						compositingStrategy = CompositingStrategy.Offscreen
					}
					.drawWithContent {
						drawContent()
						drawRect(Brush.verticalGradient(0.55f to Color.Black, 1f to Color.Transparent), blendMode = BlendMode.DstIn)
					},
				alignment = Alignment.TopCenter,
				fadeMillis = 0,
			)
		}

		// Dark on the left for the text and at the bottom for the rows
		Box(
			modifier = Modifier
				.fillMaxSize()
				.background(Brush.horizontalGradient(0f to CosmicStyle.shade.copy(alpha = 0.94f), 0.42f to CosmicStyle.shade.copy(alpha = 0.62f), 0.75f to CosmicStyle.shade.copy(alpha = 0.05f)))
				.background(Brush.verticalGradient(0.45f to CosmicStyle.shade.copy(alpha = 0f), 1f to CosmicStyle.shade.copy(alpha = 0.95f))),
		)

		Column(
			modifier = Modifier
				.padding(start = CosmicStyle.gutter, end = CosmicStyle.gutter, top = 38.dp, bottom = 104.dp)
				.widthIn(max = 680.dp),
		) {
			AnimatedContent(
				targetState = item,
				transitionSpec = { (fadeIn(tween(600)) + slideInVertically(tween(600)) { it / 12 }) togetherWith fadeOut(tween(200)) },
				label = "heroBody",
			) { shown ->
				Column {
					noteLabel(shown)?.let { note ->
						CosmicText(
							text = note.uppercase(),
							modifier = Modifier
								.padding(bottom = 12.dp)
								.background(Brush.horizontalGradient(listOf(Color(0xFFFF7043), Color(0xFFE91E63))), RoundedCornerShape(99.dp))
								.padding(horizontal = 12.dp, vertical = 4.dp),
							size = 12.sp,
							weight = FontWeight.ExtraBold,
							letterSpacing = 0.06.em,
							maxLines = 1,
						)
					}
					val logo = repository.imageUrl(shown.id, ImageType.LOGO, shown.logoTag, 600)
					if (logo != null) {
						CosmicImage(
							url = logo,
							modifier = Modifier
								.padding(bottom = 14.dp)
								.size(width = 360.dp, height = 120.dp),
							contentScale = ContentScale.Fit,
							alignment = Alignment.BottomStart,
						)
					} else {
						CosmicText(shown.name, Modifier.padding(bottom = 10.dp), 42.sp, FontWeight.Black, maxLines = 2, lineHeight = 44.sp, letterSpacing = (-0.02).em)
					}
					CosmicMeta(shown.rating, shown.year, shown.genres, Modifier.padding(bottom = 10.dp))
					if (!shown.overview.isNullOrBlank()) {
						CosmicText(shown.overview, Modifier.padding(bottom = 18.dp), 15.sp, color = Color.White.copy(alpha = 0.82f), maxLines = 3, lineHeight = 22.sp)
					}
				}
			}

			// The buttons stay while the titles change, so the remote does not lose its place
			Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
				CosmicButton("▶ " + stringResource(R.string.cosmic_play), { actions.play(item.id) }, main = true, focusRequester = playFocusRequester, onFocused = onFocused)
				CosmicButton(stringResource(R.string.cosmic_info), { actions.open(item.id) }, onFocused = onFocused)
			}
		}

		// The small posters of the other titles: one press shows it
		if (items.size > 1) {
			LazyRow(
				modifier = Modifier
					.align(Alignment.BottomStart)
					.padding(bottom = 14.dp)
					.focusRestorer(),
				contentPadding = PaddingValues(horizontal = CosmicStyle.gutter, vertical = 10.dp),
				horizontalArrangement = Arrangement.spacedBy(8.dp),
				verticalAlignment = Alignment.Bottom,
			) {
				itemsIndexed(items, key = { _, it -> it.id }) { i, pick ->
					HeroPick(
						imageUrl = repository.imageUrl(pick.id, ImageType.PRIMARY, pick.primaryTag, 120),
						active = i == index % items.size,
						onClick = { index = i },
						onFocused = onFocused,
					)
				}
			}
		}
	}
}

@Composable
private fun HeroPick(imageUrl: String?, active: Boolean, onClick: () -> Unit, onFocused: () -> Unit) {
	val interaction = remember { MutableInteractionSource() }
	val focused = rememberFocused(interaction)
	LaunchedEffect(focused) { if (focused) onFocused() }
	val alpha by animateFloatAsState(if (active) 1f else if (focused) 0.9f else 0.55f, tween(200), label = "pick")
	val lift by animateFloatAsState(if (active) -4f else 0f, tween(200), label = "lift")
	val shape = RoundedCornerShape(10.dp)

	Box(
		modifier = Modifier
			.graphicsLayer { translationY = lift * density }
			.cosmicRing(focused, 10.dp)
			.size(width = 46.dp, height = 68.dp)
			.alpha(alpha)
			.clip(shape)
			.background(Color(0xFF222222))
			.border(2.dp, if (active) Color.White else Color.Transparent, shape)
			.clickable(interactionSource = interaction, indication = null, onClick = onClick),
	) {
		CosmicImage(imageUrl, Modifier.fillMaxSize())
	}
}

/* ---------- What do we watch today? ---------- */

/**
 * One title the user has not seen yet, big: its backdrop (or its cover blurred), poster, logo, overview and the
 * buttons to play it, see it, or roll the dice for another.
 */
@Composable
fun CosmicSpotlight(
	items: List<CosmicHomeCard>,
	title: String,
	accent: Color,
	repository: CosmicHomeRepository,
	actions: CosmicHomeActions,
	inset: Dp = CosmicStyle.gutter,
	bottom: Dp = 30.dp,
) {
	if (items.isEmpty()) return
	// A different title each visit; "another" goes through the rest
	var index by remember(items) { mutableIntStateOf((0 until items.size).random()) }
	val item = items[index % items.size]
	val square = item.type in setOf("AudioBook", "MusicAlbum", "Audio", "MusicArtist")
	val shape = RoundedCornerShape(22.dp)

	Column(modifier = Modifier.padding(bottom = bottom)) {
		CosmicRowHeader(title, accent, inset, stringResource(R.string.cosmic_spotlight_help))
		Gap(12.dp)
		Box(
			modifier = Modifier
				.padding(horizontal = inset)
				.fillMaxWidth()
				.heightIn(min = 326.dp)
				.shadow(16.dp, shape)
				.clip(shape)
				.background(CosmicStyle.night)
				.border(1.dp, Color.White.copy(alpha = 0.07f), shape),
		) {
			Crossfade(targetState = item, animationSpec = tween(600), label = "spotBg", modifier = Modifier.matchParentSize()) { shown ->
				if (shown.backdropTag != null) {
					CosmicImage(
						url = repository.imageUrl(shown.id, ImageType.BACKDROP, shown.backdropTag, 1280),
						modifier = Modifier.fillMaxSize(),
						alignment = Alignment.TopCenter,
						fadeMillis = 0,
					)
				} else {
					// Books and audiobooks have no backdrop: their cover, blurred
					CosmicImage(
						url = repository.imageUrl(shown.id, ImageType.PRIMARY, shown.primaryTag, 120),
						modifier = Modifier.fillMaxSize().scale(1.25f).blur(28.dp).alpha(0.6f),
						fadeMillis = 0,
					)
				}
			}
			Box(
				modifier = Modifier
					.matchParentSize()
					.background(Brush.horizontalGradient(0f to CosmicStyle.shade.copy(alpha = 0.95f), 0.45f to CosmicStyle.shade.copy(alpha = 0.7f), 1f to CosmicStyle.shade.copy(alpha = 0.1f)))
					.background(Brush.verticalGradient(0.45f to Color.Transparent, 1f to CosmicStyle.shade.copy(alpha = 0.9f))),
			)

			Row(
				modifier = Modifier
					.align(Alignment.BottomStart)
					.padding(29.dp),
				horizontalArrangement = Arrangement.spacedBy(24.dp),
				verticalAlignment = Alignment.Bottom,
			) {
				val posterShape = RoundedCornerShape(14.dp)
				Crossfade(targetState = item, animationSpec = tween(600), label = "spotPoster") { shown ->
					Box(
						modifier = Modifier
							.size(if (square) 163.dp else 125.dp, if (square) 163.dp else 188.dp)
							.shadow(12.dp, posterShape)
							.border(2.dp, accent, posterShape)
							.clip(posterShape)
							.background(CosmicStyle.cardBottom),
					) {
						CosmicImage(repository.imageUrl(shown.id, ImageType.PRIMARY, shown.primaryTag, 360), Modifier.fillMaxSize(), fadeMillis = 0)
					}
				}

				Column(modifier = Modifier.widthIn(max = 620.dp)) {
					AnimatedContent(
						targetState = item,
						transitionSpec = { (fadeIn(tween(500)) + slideInVertically(tween(500)) { it / 12 }) togetherWith fadeOut(tween(150)) },
						label = "spotBody",
					) { shown ->
						Column {
							val logo = repository.imageUrl(shown.id, ImageType.LOGO, shown.logoTag, 500)
							if (logo != null) {
								CosmicImage(logo, Modifier.padding(bottom = 10.dp).size(300.dp, 90.dp), ContentScale.Fit, Alignment.BottomStart)
							} else {
								CosmicText(shown.name, Modifier.padding(bottom = 8.dp), 29.sp, FontWeight.Black, maxLines = 2, lineHeight = 32.sp)
							}
							CosmicMeta(shown.rating, shown.year, shown.genres, Modifier.padding(bottom = 10.dp))
							if (!shown.overview.isNullOrBlank()) {
								CosmicText(shown.overview, Modifier.padding(bottom = 16.dp), 14.4.sp, color = Color.White.copy(alpha = 0.78f), maxLines = 3, lineHeight = 21.sp)
							}
						}
					}
					Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
						CosmicButton("▶ " + stringResource(R.string.cosmic_play), { actions.play(item.id) }, main = true)
						CosmicButton(stringResource(R.string.cosmic_info), { actions.open(item.id) })
						if (items.size > 1) {
							CosmicButton(
								text = "🎲 " + stringResource(R.string.cosmic_another),
								onClick = { index++ },
								textColor = CosmicStyle.star,
								borderColor = CosmicStyle.star.copy(alpha = 0.5f),
							)
						}
					}
				}
			}
		}
	}
}

/* ---------- Library block ---------- */

/**
 * A library on the home: a rounded panel lit in its colour from the top left, with its icon, name and "see all",
 * then its rows, "what do we watch today in …", and the user's own rows of it.
 */
@Composable
fun CosmicLibraryBlock(
	block: CosmicHomeBlock,
	personalRows: List<CosmicShelf>,
	repository: CosmicHomeRepository,
	actions: CosmicHomeActions,
) {
	val accent = CosmicStyle.accentOf(block.collectionType, block.name)
	val shape = RoundedCornerShape(26.dp)
	val inset = 12.dp

	Column(
		modifier = Modifier
			.padding(start = CosmicStyle.gutter - 12.dp, end = CosmicStyle.gutter - 12.dp, bottom = 34.dp)
			.fillMaxWidth()
			.clip(shape)
			.drawBehind {
				drawRect(
					Brush.radialGradient(
						colors = listOf(accent.copy(alpha = 0.16f), Color.Transparent),
						center = Offset.Zero,
						radius = size.width * 0.75f,
					),
				)
				drawRect(Color.White.copy(alpha = 0.025f))
			}
			.border(1.dp, CosmicStyle.line, shape)
			.padding(top = 18.dp, bottom = 4.dp),
	) {
		Row(
			modifier = Modifier.padding(start = inset, end = inset, bottom = 14.dp),
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.spacedBy(12.dp),
		) {
			Box(
				modifier = Modifier
					.size(42.dp)
					.clip(RoundedCornerShape(14.dp))
					.background(accent.copy(alpha = 0.28f))
					.border(1.dp, accent.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
				contentAlignment = Alignment.Center,
			) { CosmicText(CosmicStyle.iconOf(block.collectionType), size = 20.sp) }
			CosmicText(block.name, size = 27.sp, weight = FontWeight.Black, maxLines = 1, letterSpacing = (-0.02).em)
			Spacer(Modifier.weight(1f))
			CosmicMoreLink(stringResource(R.string.cosmic_see_all), accent, { actions.open(block.libraryId) })
		}

		block.rows.take(1).forEach { CosmicShelfRow(it, accent, repository, actions, inset, 22.dp) }
		if (!block.spotlight.isNullOrEmpty()) {
			val title = if (block.collectionType == "books") R.string.cosmic_listen_today_in else R.string.cosmic_spotlight_in
			CosmicSpotlight(block.spotlight, stringResource(title, block.name), accent, repository, actions, inset, 22.dp)
		}
		block.rows.drop(1).forEach { CosmicShelfRow(it, accent, repository, actions, inset, 22.dp) }
		personalRows.forEach { CosmicShelfRow(it, accent, repository, actions, inset, 22.dp) }
		block.extra.orEmpty().forEach { CosmicShelfRow(it, accent, repository, actions, inset, 22.dp) }
	}
}
