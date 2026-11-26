package org.jellyfin.androidtv.ui.home.cosmic

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.data.cosmic.CosmicHomePage
import org.jellyfin.androidtv.data.cosmic.CosmicHomePersonal
import org.jellyfin.androidtv.data.cosmic.CosmicHomeRepository
import org.jellyfin.androidtv.data.cosmic.CosmicHomeSections
import org.jellyfin.androidtv.data.cosmic.CosmicProgressCard
import org.jellyfin.androidtv.data.cosmic.CosmicUpcoming
import org.jellyfin.androidtv.ui.itemhandling.ItemLauncher
import org.jellyfin.androidtv.ui.navigation.NavigationRepository
import org.jellyfin.androidtv.util.PlaybackHelper
import org.jellyfin.sdk.model.api.ImageType
import org.koin.compose.koinInject
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** The sections before and after the libraries, in the web's default order. */
private val SECTIONS_BEFORE = listOf("resume", "nextup", "listening", "spotlight")
private val SECTIONS_AFTER = listOf("upcoming", "recommendations", "genres", "favorites")

/**
 * The Cosmic home on the TV, drawn like the Cosmic web home in its TV layout: the banner, the libraries, the user's
 * rows, each library in its own panel, upcoming releases, genres and favorites, in the order the user chose on
 * the web. [toolbar] is drawn over the banner, like the web's top bar.
 */
@Composable
fun CosmicHomeScreen(
	initialPage: CosmicHomePage,
	toolbar: @Composable () -> Unit,
) {
	val repository = koinInject<CosmicHomeRepository>()
	val context = LocalContext.current
	val scope = rememberCoroutineScope()
	val itemLauncher = koinInject<ItemLauncher>()
	val playbackHelper = koinInject<PlaybackHelper>()
	val navigationRepository = koinInject<NavigationRepository>()
	val actions = remember {
		CosmicHomeActions(context, scope, repository, itemLauncher, playbackHelper, navigationRepository)
	}

	var page by remember(initialPage) { mutableStateOf(initialPage) }
	var personal by remember { mutableStateOf<CosmicHomePersonal?>(null) }
	var sections by remember { mutableStateOf(CosmicHomeSections()) }
	val lastInteraction = remember { mutableLongStateOf(0L) }
	val listState = rememberLazyListState()
	val playFocusRequester = remember { FocusRequester() }

	// Coming back to the home (after watching something) refreshes the user's rows and the order of the sections
	val lifecycle = LocalLifecycleOwner.current.lifecycle
	LaunchedEffect(lifecycle) {
		lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
			personal = repository.getPersonal() ?: personal
			sections = repository.getHomeSections()
		}
	}

	// The very first time the server is still preparing the page: ask again every 20 s
	LaunchedEffect(page.ready) {
		while (!page.ready) {
			delay(20_000)
			repository.getPage()?.let { page = it }
		}
	}

	// Arriving at the home, the remote is on "Play" of the banner
	LaunchedEffect(Unit) {
		if (page.hero.isNotEmpty()) runCatching { playFocusRequester.requestFocus() }
	}

	val order = remember(page, sections) { orderSections(page, sections) }
	val personalRows = remember(personal) { personal?.libraryRows.orEmpty().associate { it.libraryId to it.rows } }
	val blocks = remember(page) { page.blocks.associateBy { "lib:${it.libraryId}" } }

	Box(
		modifier = Modifier
			.fillMaxSize()
			.background(CosmicStyle.background)
			.drawBehind {
				// The two soft lights of the Cosmic theme
				drawRect(Brush.radialGradient(listOf(Color(0x9900375A), Color.Transparent), Offset(size.width * 0.15f, size.height * 0.4f), size.maxDimension * 0.5f))
				drawRect(Brush.radialGradient(listOf(Color(0x8050006E), Color.Transparent), Offset(size.width * 0.85f, size.height * 0.2f), size.maxDimension * 0.5f))
			}
			.onPreviewKeyEvent {
				lastInteraction.longValue = System.currentTimeMillis()
				false
			},
	) {
		LazyColumn(
			state = listState,
			modifier = Modifier.fillMaxSize(),
			contentPadding = PaddingValues(bottom = 96.dp),
		) {
			item(key = "hero") {
				CosmicHero(
					items = page.hero,
					repository = repository,
					actions = actions,
					lastInteraction = lastInteraction,
					playFocusRequester = playFocusRequester,
					onFocused = { scope.launch { listState.animateScrollToItem(0) } },
				)
				if (page.hero.isEmpty()) Gap(84.dp)
			}

			item(key = "nav") { LibraryChips(page, actions) }

			if (!page.ready) {
				item(key = "preparing") {
					CosmicText(
						text = stringResource(R.string.cosmic_preparing),
						modifier = Modifier.padding(horizontal = CosmicStyle.gutter, vertical = 8.dp),
						weight = FontWeight.Bold,
						color = CosmicStyle.soft,
					)
				}
			}

			for (key in order) {
				item(key = key) {
					when {
						key.startsWith("lib:") -> blocks[key]?.let { block ->
							CosmicLibraryBlock(block, personalRows[block.libraryId].orEmpty(), repository, actions)
						}

						key == "resume" -> ProgressRow(stringResource(R.string.cosmic_continue), Color(0xFF00A4DC), personal?.continueWatching, repository, actions)
						key == "nextup" -> ProgressRow(stringResource(R.string.cosmic_nextup), Color(0xFFAA5CC3), personal?.nextUp, repository, actions)
						key == "listening" -> ProgressRow(stringResource(R.string.cosmic_listening), Color(0xFFFF7043), personal?.listening, repository, actions)
						key == "spotlight" -> CosmicSpotlight(page.spotlight.orEmpty(), stringResource(R.string.cosmic_spotlight), CosmicStyle.star, repository, actions)
						key == "upcoming" -> UpcomingRow(page.upcoming, repository, actions)
						key == "genres" -> GenresRow(page, actions)
						key == "favorites" -> CosmicRow(
							title = stringResource(R.string.cosmic_favorites),
							accent = Color(0xFFEF5350),
							items = personal?.favorites.orEmpty(),
							key = { it.id },
						) { _, card -> CosmicHomeCardView(card, CosmicShape.Poster, Color(0xFFEF5350), repository, actions) }
					}
				}
			}
		}

		toolbar()
	}
}

/** The order of the sections: the user's from the web, with any new library or section where it belongs. */
private fun orderSections(page: CosmicHomePage, settings: CosmicHomeSections): List<String> {
	val libs = page.libraries.filter { it.collectionType != "boxsets" }.map { "lib:${it.id}" }
		.ifEmpty { page.blocks.map { "lib:${it.libraryId}" } }
	val defaults = SECTIONS_BEFORE + libs + SECTIONS_AFTER
	val order = settings.order.filter { it in defaults }.toMutableList()
	for (key in defaults.filter { it !in order }) {
		val at = when {
			key.startsWith("lib:") -> {
				val lastLib = order.indexOfLast { it.startsWith("lib:") }
				if (lastLib >= 0) lastLib + 1 else order.indexOfFirst { it in SECTIONS_AFTER }
			}

			key in SECTIONS_BEFORE -> order.indexOfFirst { it.startsWith("lib:") || it in SECTIONS_AFTER }
			else -> -1
		}
		order.add(if (at >= 0) at else order.size, key)
	}
	// Recommendations are not on the TV yet
	return order.filter { it !in settings.hidden && it != "recommendations" }
}

/** The libraries under the banner, each in its colour. */
@Composable
private fun LibraryChips(page: CosmicHomePage, actions: CosmicHomeActions) {
	val libraries = page.libraries
	if (libraries.isEmpty()) return
	LazyRow(
		modifier = Modifier.focusRestorer(),
		contentPadding = PaddingValues(start = CosmicStyle.gutter, end = CosmicStyle.gutter, top = 14.dp, bottom = 24.dp),
		horizontalArrangement = Arrangement.spacedBy(8.dp),
	) {
		items(libraries, key = { it.id }) { library ->
			CosmicChip(
				text = CosmicStyle.iconOf(library.collectionType) + "  " + library.name,
				color = CosmicStyle.accentOf(library.collectionType, library.name),
				onClick = { actions.open(library.id) },
			)
		}
	}
}

/** Continue watching, next up and continue listening: landscape cards that play from where it was left. */
@Composable
private fun ProgressRow(
	title: String,
	accent: Color,
	cards: List<CosmicProgressCard>?,
	repository: CosmicHomeRepository,
	actions: CosmicHomeActions,
) {
	val left = remainingFormatter()
	CosmicRow(title = title, accent = accent, items = cards.orEmpty(), key = { it.id }) { _, card ->
		val type = runCatching { ImageType.fromName(card.imageType) }.getOrDefault(ImageType.PRIMARY)
		// Audiobooks, books and songs only have their square cover: shown whole
		val cover = type == ImageType.PRIMARY && card.type !in setOf("Episode", "Movie", "Video")
		CosmicWideCard(
			imageUrl = repository.imageUrl(card.imageItemId, type, card.imageTag, 720),
			title = card.name,
			subtitle = listOfNotNull(card.subtitle, left(card.remainingTicks)).joinToString(" · "),
			accent = accent,
			onClick = { actions.play(card.id) },
			progress = (card.progress / 100).toFloat(),
			cover = cover,
		)
	}
}

@Composable
private fun remainingFormatter(): (Long?) -> String? {
	val hours = stringResource(R.string.cosmic_left_hours)
	val minutes = stringResource(R.string.cosmic_left_minutes)
	return { ticks ->
		if (ticks == null || ticks <= 0) null
		else {
			val total = maxOf(1, Math.round(ticks / 600_000_000.0).toInt())
			if (total >= 60) hours.format(total / 60, total % 60) else minutes.format(total)
		}
	}
}

/** New episodes of the user's series and films of their sagas coming in the next weeks, with the date. */
@Composable
private fun UpcomingRow(releases: List<CosmicUpcoming>, repository: CosmicHomeRepository, actions: CosmicHomeActions) {
	val dateFormat = remember { DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault()) }
	CosmicRow(
		title = stringResource(R.string.cosmic_upcoming),
		accent = Color(0xFF26C6DA),
		items = releases,
		key = { "${it.kind}-${it.seriesId ?: it.relatedItemId}-${it.releaseDate}-${it.title}" },
		hint = stringResource(R.string.cosmic_upcoming_hint),
	) { _, release ->
		val target = if (release.kind == "Episode") release.seriesId else release.relatedItemId
		val sources = listOfNotNull(
			repository.tmdbImageUrl(release, "w300"),
			release.imageUrl,
		) + listOfNotNull(target).flatMap { id ->
			listOf(ImageType.THUMB, ImageType.BACKDROP, ImageType.PRIMARY).map { repository.anyImageUrl(id, it, 480) }
		}
		val date = runCatching { LocalDate.parse(release.releaseDate.take(10)).format(dateFormat) }.getOrNull()
		val episode = if (release.kind == "Episode" && release.seasonNumber != null) {
			"T${release.seasonNumber}:E${release.episodeNumber ?: "?"}"
		} else release.collectionName

		CosmicWideCard(
			imageUrl = sources.firstOrNull(),
			fallbacks = sources.drop(1),
			title = release.seriesName ?: release.title,
			subtitle = listOfNotNull(episode, release.title.takeIf { release.kind == "Episode" }).joinToString(" · "),
			accent = Color(0xFF26C6DA),
			onClick = { target?.let(actions::open) },
			showPlay = false,
			dateLabel = date?.replaceFirstChar { it.titlecase(Locale.getDefault()) },
		)
	}
}

/** "Browse by genre": coloured tiles in two lines that scroll sideways. */
@Composable
private fun GenresRow(page: CosmicHomePage, actions: CosmicHomeActions) {
	if (page.genres.isEmpty()) return
	val numbers = remember { NumberFormat.getIntegerInstance() }
	Column(modifier = Modifier.padding(bottom = 30.dp)) {
		CosmicRowHeader(stringResource(R.string.cosmic_genres), Color(0xFF00A4DC), CosmicStyle.gutter)
		LazyHorizontalGrid(
			rows = GridCells.Fixed(2),
			modifier = Modifier
				.fillMaxWidth()
				.height(160.dp)
				.focusRestorer(),
			contentPadding = PaddingValues(horizontal = CosmicStyle.gutter, vertical = 18.dp),
			horizontalArrangement = Arrangement.spacedBy(10.dp),
			verticalArrangement = Arrangement.spacedBy(10.dp),
		) {
			items(page.genres, key = { it.id }) { genre ->
				CosmicGenreTile(genre.name, numbers.format(genre.count), { actions.openGenre(genre.name) })
			}
		}
	}
}

/** While the page loads: a banner and two rows that shimmer. */
@Composable
fun CosmicHomeSkeleton() {
	val shimmer = rememberInfiniteTransition(label = "shimmer")
	val shift by shimmer.animateFloat(2f, -2f, infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart), label = "shift")
	val brush = { width: Float ->
		Brush.horizontalGradient(
			listOf(Color(0xFF141A30), Color(0xFF1D2440), Color(0xFF141A30)),
			startX = shift * width,
			endX = (shift + 1) * width,
		)
	}

	Column(
		modifier = Modifier
			.fillMaxSize()
			.background(CosmicStyle.background),
	) {
		Box(Modifier.fillMaxWidth().height(360.dp).drawBehind { drawRect(brush(size.width)) })
		repeat(2) {
			Box(
				Modifier
					.padding(start = CosmicStyle.gutter, end = CosmicStyle.gutter, top = 8.dp, bottom = 28.dp)
					.fillMaxWidth()
					.height(240.dp)
					.background(Color.Transparent, RoundedCornerShape(24.dp))
					.drawBehind { drawRoundRect(brush(size.width), cornerRadius = androidx.compose.ui.geometry.CornerRadius(24.dp.toPx())) },
			)
		}
	}
}
