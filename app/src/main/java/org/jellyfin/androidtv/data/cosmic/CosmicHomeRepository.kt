package org.jellyfin.androidtv.data.cosmic

import kotlinx.serialization.json.Json
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.displayPreferencesApi
import org.jellyfin.sdk.api.client.extensions.get
import org.jellyfin.sdk.api.client.extensions.imageApi
import org.jellyfin.sdk.api.client.extensions.userLibraryApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.ImageType
import timber.log.Timber
import java.util.UUID

/**
 * Reads the home page that a Jellyfin Cosmic server prepares in the background (HomeFeed).
 * On a server without HomeFeed (the official Jellyfin) every call returns null, and the app keeps its usual home.
 */
class CosmicHomeRepository(
	private val api: ApiClient,
) {
	private val json = Json { ignoreUnknownKeys = true }

	/** The last page read from this server, to draw the home at once when coming back to it. */
	var lastPage: CosmicHomePage? = null
		private set
	private var lastPageServer: String? = null

	fun cachedPage(): CosmicHomePage? = lastPage?.takeIf { lastPageServer == api.baseUrl }

	suspend fun getPage(): CosmicHomePage? = runCatching {
		api.get<CosmicHomePage>("/HomeFeed/Page").content
	}.onFailure { Timber.i(it, "HomeFeed page not available") }.getOrNull().also {
		lastPage = it
		lastPageServer = api.baseUrl
	}

	suspend fun getPersonal(): CosmicHomePersonal? = runCatching {
		api.get<CosmicHomePersonal>("/HomeFeed/Personal").content
	}.onFailure { Timber.i(it, "HomeFeed personal rows not available") }.getOrNull()

	/** The sections of the home as the user ordered them on the web; the default order when never changed. */
	suspend fun getHomeSections(): CosmicHomeSections = runCatching {
		val prefs = api.displayPreferencesApi.getDisplayPreferences(
			displayPreferencesId = "usersettings",
			client = "emby",
		).content
		prefs.customPrefs[HOME_SECTIONS_SETTING]?.let { json.decodeFromString<CosmicHomeSections>(it) }
	}.onFailure { Timber.i(it, "Cosmic home sections not available") }.getOrNull() ?: CosmicHomeSections()

	/** The full item, to open it with the usual screens of the app. */
	suspend fun getItem(id: UUID): BaseItemDto? = runCatching {
		api.userLibraryApi.getItem(itemId = id).content
	}.onFailure { Timber.w(it, "Could not load item $id") }.getOrNull()

	fun imageUrl(id: UUID, type: ImageType, tag: String?, maxWidth: Int): String? {
		if (tag == null) return null
		return api.imageApi.getItemImageUrl(itemId = id, imageType = type, tag = tag, maxWidth = maxWidth, quality = 85)
	}

	/** An image without a known tag (pictures of upcoming releases fall back to the library's own art). */
	fun anyImageUrl(id: UUID, type: ImageType, maxWidth: Int): String =
		api.imageApi.getItemImageUrl(itemId = id, imageType = type, maxWidth = maxWidth, quality = 85)

	/** A TMDb picture of an upcoming release through the server cache (Discovery/Images). */
	fun tmdbImageUrl(release: CosmicUpcoming, size: String): String? {
		val file = release.imagePath?.removePrefix("/")
			?: release.imageUrl?.let { TMDB_URL.matchEntire(it)?.groupValues?.get(1) }
			?: return null
		return api.createUrl("/Discovery/Images/{size}/{file}", mapOf("size" to size, "file" to file))
	}

	companion object {
		private val TMDB_URL = Regex("""^https?://image\.tmdb\.org/t/p/[^/]+/([A-Za-z0-9_-]+\.(?:jpe?g|png))$""")

		const val HOME_SECTIONS_SETTING = "cosmicHomeSections"
	}
}
