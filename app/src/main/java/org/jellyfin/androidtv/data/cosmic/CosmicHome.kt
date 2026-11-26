package org.jellyfin.androidtv.data.cosmic

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jellyfin.sdk.model.serializer.UUIDSerializer
import java.util.UUID

/*
 * Home page prepared by a Jellyfin Cosmic server (HomeFeed endpoints), the same data its web home draws.
 */

@Serializable
data class CosmicHomeCard(
	@SerialName("Id")
	@Serializable(with = UUIDSerializer::class)
	val id: UUID,
	@SerialName("Name")
	val name: String = "",
	@SerialName("Type")
	val type: String = "",
	@SerialName("Year")
	val year: Int? = null,
	@SerialName("Rating")
	val rating: Float? = null,
	@SerialName("PrimaryTag")
	val primaryTag: String? = null,
	@SerialName("BackdropTag")
	val backdropTag: String? = null,
	@SerialName("LogoTag")
	val logoTag: String? = null,
	@SerialName("Overview")
	val overview: String? = null,
	@SerialName("Genres")
	val genres: List<String>? = null,
	@SerialName("Note")
	val note: String? = null,
	@SerialName("NoteCount")
	val noteCount: Int? = null,
	@SerialName("Subtitle")
	val subtitle: String? = null,
	@SerialName("Progress")
	val progress: Double? = null,
	@SerialName("Done")
	val done: Boolean = false,
)

@Serializable
data class CosmicProgressCard(
	@SerialName("Id")
	@Serializable(with = UUIDSerializer::class)
	val id: UUID,
	@SerialName("Name")
	val name: String = "",
	@SerialName("Subtitle")
	val subtitle: String? = null,
	@SerialName("Type")
	val type: String = "",
	/** 0 to 100 */
	@SerialName("Progress")
	val progress: Double = 0.0,
	@SerialName("RemainingTicks")
	val remainingTicks: Long? = null,
	@SerialName("PositionTicks")
	val positionTicks: Long = 0,
	@SerialName("ImageItemId")
	@Serializable(with = UUIDSerializer::class)
	val imageItemId: UUID,
	@SerialName("ImageType")
	val imageType: String = "Primary",
	@SerialName("ImageTag")
	val imageTag: String? = null,
)

@Serializable
data class CosmicShelf(
	@SerialName("Key")
	val key: String = "",
	/** poster, rank, square, round or wide */
	@SerialName("Shape")
	val shape: String = "poster",
	@SerialName("Items")
	val items: List<CosmicHomeCard> = emptyList(),
)

@Serializable
data class CosmicUpcoming(
	@SerialName("Kind")
	val kind: String = "",
	@SerialName("Title")
	val title: String = "",
	@SerialName("ReleaseDate")
	val releaseDate: String = "",
	@SerialName("ImageUrl")
	val imageUrl: String? = null,
	/** The TMDb path ("/abc.jpg"), served by the Cosmic server from its cache */
	@SerialName("ImagePath")
	val imagePath: String? = null,
	@SerialName("SeriesId")
	@Serializable(with = UUIDSerializer::class)
	val seriesId: UUID? = null,
	@SerialName("SeriesName")
	val seriesName: String? = null,
	@SerialName("SeasonNumber")
	val seasonNumber: Int? = null,
	@SerialName("EpisodeNumber")
	val episodeNumber: Int? = null,
	@SerialName("CollectionName")
	val collectionName: String? = null,
	@SerialName("RelatedItemId")
	@Serializable(with = UUIDSerializer::class)
	val relatedItemId: UUID? = null,
)

@Serializable
data class CosmicHomeBlock(
	@SerialName("LibraryId")
	@Serializable(with = UUIDSerializer::class)
	val libraryId: UUID,
	@SerialName("Name")
	val name: String = "",
	@SerialName("CollectionType")
	val collectionType: String? = null,
	@SerialName("Rows")
	val rows: List<CosmicShelf> = emptyList(),
	@SerialName("Spotlight")
	val spotlight: List<CosmicHomeCard>? = null,
	@SerialName("Extra")
	val extra: List<CosmicShelf>? = null,
)

@Serializable
data class CosmicGenre(
	@SerialName("Id")
	@Serializable(with = UUIDSerializer::class)
	val id: UUID,
	@SerialName("Name")
	val name: String = "",
	@SerialName("Count")
	val count: Int = 0,
)

@Serializable
data class CosmicLibrary(
	@SerialName("Id")
	@Serializable(with = UUIDSerializer::class)
	val id: UUID,
	@SerialName("Name")
	val name: String = "",
	@SerialName("CollectionType")
	val collectionType: String? = null,
)

@Serializable
data class CosmicHomePage(
	@SerialName("Ready")
	val ready: Boolean = false,
	@SerialName("Libraries")
	val libraries: List<CosmicLibrary> = emptyList(),
	@SerialName("Hero")
	val hero: List<CosmicHomeCard> = emptyList(),
	@SerialName("Blocks")
	val blocks: List<CosmicHomeBlock> = emptyList(),
	@SerialName("Spotlight")
	val spotlight: List<CosmicHomeCard>? = null,
	@SerialName("Upcoming")
	val upcoming: List<CosmicUpcoming> = emptyList(),
	@SerialName("Genres")
	val genres: List<CosmicGenre> = emptyList(),
)

@Serializable
data class CosmicHomePersonal(
	@SerialName("Continue")
	val continueWatching: List<CosmicProgressCard> = emptyList(),
	@SerialName("NextUp")
	val nextUp: List<CosmicProgressCard> = emptyList(),
	@SerialName("Listening")
	val listening: List<CosmicProgressCard> = emptyList(),
	@SerialName("Favorites")
	val favorites: List<CosmicHomeCard> = emptyList(),
	@SerialName("LibraryRows")
	val libraryRows: List<CosmicHomeBlock> = emptyList(),
)

/** The order and the hidden sections the user chose on the web (Settings → Home), shared through the server. */
@Serializable
data class CosmicHomeSections(
	@SerialName("order")
	val order: List<String> = emptyList(),
	@SerialName("hidden")
	val hidden: List<String> = emptyList(),
)
