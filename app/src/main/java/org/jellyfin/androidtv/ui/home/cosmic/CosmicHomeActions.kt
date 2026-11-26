package org.jellyfin.androidtv.ui.home.cosmic

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.data.cosmic.CosmicHomeRepository
import org.jellyfin.androidtv.ui.itemhandling.BaseItemDtoBaseRowItem
import org.jellyfin.androidtv.ui.itemhandling.ItemLauncher
import org.jellyfin.androidtv.ui.navigation.Destinations
import org.jellyfin.androidtv.ui.navigation.NavigationRepository
import org.jellyfin.androidtv.ui.presentation.MutableObjectAdapter
import org.jellyfin.androidtv.util.PlaybackHelper
import java.util.UUID

/** What the cards and buttons of the Cosmic home do: open with the usual screens of the app, or play. */
class CosmicHomeActions(
	private val context: Context,
	private val scope: CoroutineScope,
	private val repository: CosmicHomeRepository,
	private val itemLauncher: ItemLauncher,
	private val playbackHelper: PlaybackHelper,
	private val navigationRepository: NavigationRepository,
) {
	/** Opens an item, a library or a folder the way the rest of the app does. */
	fun open(id: UUID) {
		scope.launch {
			val item = repository.getItem(id) ?: return@launch
			val rowItem = BaseItemDtoBaseRowItem(item)
			val adapter = MutableObjectAdapter<Any>().apply { add(rowItem) }
			itemLauncher.launch(rowItem, adapter, context)
		}
	}

	/** Plays an item, from where it was left. */
	fun play(id: UUID) = playbackHelper.retrieveAndPlay(id, false, context)

	/** A genre of the home: what the server has of it. */
	fun openGenre(name: String) = navigationRepository.navigate(Destinations.search(name))
}
