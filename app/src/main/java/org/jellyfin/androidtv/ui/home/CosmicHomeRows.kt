package org.jellyfin.androidtv.ui.home

import android.content.Context
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.Row
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.constant.QueryType
import org.jellyfin.androidtv.data.cosmic.CosmicHomeBlock
import org.jellyfin.androidtv.data.cosmic.CosmicHomePage
import org.jellyfin.androidtv.data.cosmic.CosmicHomePersonal
import org.jellyfin.androidtv.data.cosmic.CosmicHomeRepository
import org.jellyfin.androidtv.data.cosmic.CosmicShelf
import org.jellyfin.androidtv.ui.itemhandling.BaseItemDtoBaseRowItem
import org.jellyfin.androidtv.ui.itemhandling.ItemRowAdapter
import org.jellyfin.androidtv.ui.presentation.CardPresenter
import org.jellyfin.androidtv.ui.presentation.MutableObjectAdapter
import org.jellyfin.sdk.model.api.BaseItemDto
import java.util.UUID

/** One row of the Cosmic home: a title and the items, already loaded. */
class HomeFragmentCosmicRow(
	val key: String,
	private val title: String,
	private var items: List<BaseItemDto>,
	private val preferParentThumb: Boolean = false,
) : HomeFragmentRow {
	private var adapter: ItemRowAdapter? = null
	private var listRow: ListRow? = null
	private var parent: MutableObjectAdapter<Row>? = null

	override fun addToRowsAdapter(context: Context, cardPresenter: CardPresenter, rowsAdapter: MutableObjectAdapter<Row>) {
		if (items.isEmpty()) return

		val rowAdapter = ItemRowAdapter(context, items, cardPresenter, rowsAdapter, QueryType.StaticItems)
		val row = ListRow(HeaderItem(title), rowAdapter)
		rowAdapter.setRow(row)
		rowAdapter.replaceAll(items.map(::toRowItem))
		rowsAdapter.add(row)
		adapter = rowAdapter
		listRow = row
		parent = rowsAdapter
	}

	/** Shows new items in the row already on screen, keeping the focus where it is when the items are the same. */
	fun update(newItems: List<BaseItemDto>) {
		items = newItems
		if (newItems.isEmpty()) {
			listRow?.let { parent?.remove(it) }
			adapter = null
			listRow = null
			return
		}

		adapter?.replaceAll(
			newItems.map(::toRowItem),
			areItemsTheSame = { old, new -> (old as? BaseItemDtoBaseRowItem)?.baseItem?.id == (new as? BaseItemDtoBaseRowItem)?.baseItem?.id },
			areContentsTheSame = { old, new -> (old as? BaseItemDtoBaseRowItem)?.baseItem == (new as? BaseItemDtoBaseRowItem)?.baseItem },
		)
	}

	private fun toRowItem(item: BaseItemDto): Any = BaseItemDtoBaseRowItem(item, preferParentThumb)
}

/**
 * Builds the home rows from the page a Jellyfin Cosmic server prepares, in the same order as its web home:
 * continue watching, next up, continue listening, then each library with its rows, then favorites.
 */
class CosmicHomeRows(
	private val context: Context,
	private val repository: CosmicHomeRepository,
) {
	private data class RowSpec(val key: String, val title: String, val ids: List<UUID>, val preferParentThumb: Boolean = false)

	/** The rows, or null when the server has no HomeFeed (an official Jellyfin server). */
	suspend fun load(): List<HomeFragmentCosmicRow>? {
		val specs = loadSpecs() ?: return null
		val items = repository.getItems(specs.flatMap { it.ids })

		return specs.map { spec ->
			HomeFragmentCosmicRow(spec.key, spec.title, spec.ids.mapNotNull(items::get), spec.preferParentThumb)
		}
	}

	/** Loads the page again and updates the rows already shown (rows that were not there appear next time). */
	suspend fun refresh(rows: Collection<HomeFragmentCosmicRow>): Map<HomeFragmentCosmicRow, List<BaseItemDto>> {
		val specs = loadSpecs() ?: return emptyMap()
		val items = repository.getItems(specs.flatMap { it.ids })
		val byKey = specs.associateBy { it.key }

		// A row that is no longer on the page (e.g. nothing left to continue) gets no items and goes away
		return rows.associateWith { row -> byKey[row.key]?.ids?.mapNotNull(items::get).orEmpty() }
	}

	private suspend fun loadSpecs(): List<RowSpec>? {
		val page = repository.getPage() ?: return null
		val personal = repository.getPersonal() ?: CosmicHomePersonal()
		return buildSpecs(page, personal)
	}

	private fun buildSpecs(page: CosmicHomePage, personal: CosmicHomePersonal): List<RowSpec> = buildList {
		add(RowSpec("continue", context.getString(R.string.cosmic_row_continue), personal.continueWatching.map { it.id }, preferParentThumb = true))
		add(RowSpec("nextup", context.getString(R.string.cosmic_row_nextup), personal.nextUp.map { it.id }, preferParentThumb = true))
		add(RowSpec("listening", context.getString(R.string.cosmic_row_listening), personal.listening.map { it.id }))

		val personalRows = personal.libraryRows.associate { it.libraryId to it.rows }
		for (block in page.blocks) addBlock(block, personalRows[block.libraryId].orEmpty())

		add(RowSpec("favorites", context.getString(R.string.cosmic_row_favorites), personal.favorites.map { it.id }))
	}.filter { it.ids.isNotEmpty() }

	private fun MutableList<RowSpec>.addBlock(block: CosmicHomeBlock, personalRows: List<CosmicShelf>) {
		val libraryKey = "lib:${block.libraryId}"

		fun shelf(shelf: CosmicShelf) = RowSpec(
			key = "$libraryKey:${shelf.key}",
			title = context.getString(R.string.cosmic_row_in_library, block.name, shelfTitle(shelf.key)),
			ids = shelf.items.map { it.id },
		)

		// Like the web: the first row, "what do we watch today", the rest of the rows, then the user's own
		block.rows.take(1).forEach { add(shelf(it)) }
		if (block.spotlight.isNotEmpty()) {
			val title = if (block.collectionType == "books") R.string.cosmic_row_spotlight_books else R.string.cosmic_row_spotlight
			add(RowSpec("$libraryKey:spotlight", context.getString(title, block.name), block.spotlight.map { it.id }))
		}
		block.rows.drop(1).forEach { add(shelf(it)) }
		personalRows.forEach { add(shelf(it)) }
		block.extra.forEach { add(shelf(it)) }
	}

	private fun shelfTitle(key: String): String {
		if (key.startsWith(GENRE_PREFIX)) return key.removePrefix(GENRE_PREFIX)

		val title = when (key) {
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
		return context.getString(title)
	}

	private companion object {
		const val GENRE_PREFIX = "genre:"
	}
}
