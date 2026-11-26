package org.jellyfin.androidtv.data.cosmic

import org.jellyfin.androidtv.data.repository.ItemRepository
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.get
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.model.api.BaseItemDto
import timber.log.Timber
import java.util.UUID

/**
 * Reads the home page that a Jellyfin Cosmic server prepares in the background (HomeFeed).
 * On a server without HomeFeed (the official Jellyfin) every call returns null, and the app keeps its usual home.
 */
class CosmicHomeRepository(
	private val api: ApiClient,
) {
	suspend fun getPage(): CosmicHomePage? = runCatching {
		api.get<CosmicHomePage>("/HomeFeed/Page").content
	}.onFailure { Timber.i(it, "HomeFeed page not available") }.getOrNull()

	suspend fun getPersonal(): CosmicHomePersonal? = runCatching {
		api.get<CosmicHomePersonal>("/HomeFeed/Personal").content
	}.onFailure { Timber.i(it, "HomeFeed personal rows not available") }.getOrNull()

	/**
	 * The full items for [ids], in the same order, so the rows can use the usual cards and open the usual screens.
	 * Items that no longer exist or the user cannot see are left out.
	 */
	suspend fun getItems(ids: Collection<UUID>): Map<UUID, BaseItemDto> {
		val unique = ids.distinct()
		if (unique.isEmpty()) return emptyMap()

		return unique.chunked(ITEMS_PER_REQUEST).flatMap { chunk ->
			runCatching {
				api.itemsApi.getItems(
					ids = chunk,
					fields = ItemRepository.browseFields,
					imageTypeLimit = 1,
					enableTotalRecordCount = false,
				).content.items
			}.onFailure { Timber.w(it, "Could not load HomeFeed items") }.getOrDefault(emptyList())
		}.associateBy { it.id }
	}

	companion object {
		// Keeps the address of each request short
		private const val ITEMS_PER_REQUEST = 100
	}
}
