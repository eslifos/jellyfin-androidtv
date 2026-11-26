package org.jellyfin.androidtv.data.cosmic

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jellyfin.sdk.model.serializer.UUIDSerializer
import java.util.UUID

/*
 * Home page prepared by a Jellyfin Cosmic server (HomeFeed endpoints).
 * Only the fields the TV app uses; the server sends more.
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
)

@Serializable
data class CosmicProgressCard(
	@SerialName("Id")
	@Serializable(with = UUIDSerializer::class)
	val id: UUID,
	@SerialName("Name")
	val name: String = "",
)

@Serializable
data class CosmicShelf(
	@SerialName("Key")
	val key: String = "",
	@SerialName("Shape")
	val shape: String = "poster",
	@SerialName("Items")
	val items: List<CosmicHomeCard> = emptyList(),
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
	val spotlight: List<CosmicHomeCard> = emptyList(),
	@SerialName("Extra")
	val extra: List<CosmicShelf> = emptyList(),
)

@Serializable
data class CosmicHomePage(
	@SerialName("Ready")
	val ready: Boolean = false,
	@SerialName("Blocks")
	val blocks: List<CosmicHomeBlock> = emptyList(),
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
