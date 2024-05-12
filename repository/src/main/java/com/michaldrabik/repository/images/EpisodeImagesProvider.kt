package com.michaldrabik.repository.images

import com.michaldrabik.common.dispatchers.CoroutineDispatchers
import com.michaldrabik.data_local.LocalDataSource
import com.michaldrabik.data_remote.RemoteDataSource
import com.michaldrabik.data_remote.tmdb.model.TmdbImage
import com.michaldrabik.repository.TranslationsRepository
import com.michaldrabik.repository.mappers.Mappers
import com.michaldrabik.ui_model.Episode
import com.michaldrabik.ui_model.IdTmdb
import com.michaldrabik.ui_model.Image
import com.michaldrabik.ui_model.ImageFamily.EPISODE
import com.michaldrabik.ui_model.ImageSource
import com.michaldrabik.ui_model.ImageStatus.AVAILABLE
import com.michaldrabik.ui_model.ImageStatus.UNAVAILABLE
import com.michaldrabik.ui_model.ImageType
import com.michaldrabik.ui_model.ImageType.FANART
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EpisodeImagesProvider @Inject constructor(
  private val dispatchers: CoroutineDispatchers,
  private val remoteSource: RemoteDataSource,
  private val localSource: LocalDataSource,
  private val mappers: Mappers,
  private var translationsRepository: TranslationsRepository
) {

  suspend fun loadRemoteImage(showId: IdTmdb, episode: Episode): Image = withContext(dispatchers.IO) {
    val tmdbId = episode.ids.tmdb.id

    val type = FANART
    val cachedImage = findCachedImage(episode, type)
    if (cachedImage.status == AVAILABLE) {
      return@withContext cachedImage
    }

    val typeImages = (remoteSource.tmdb.fetchEpisodeImage(showId.id, episode.season, episode.number).stills
      // Try absolute episode number if present (may happen with certain Anime series)
      ?: (episode.numberAbs?.let { remoteSource.tmdb.fetchEpisodeImage(showId.id, episode.season, it).stills }
      ?: emptyList()))

    val remoteImage = findBestImage(typeImages, type)

    val image = when (remoteImage) {
      null -> Image.createUnavailable(FANART)
      else -> Image.createAvailable(episode.ids, type, EPISODE, remoteImage.file_path, ImageSource.TMDB)
    }

    when (image.status) {
      UNAVAILABLE -> localSource.showImages.deleteByEpisodeId(tmdbId, image.type.key)
      else -> localSource.showImages.insertEpisodeImage(mappers.image.toDatabaseShow(image))
    }

    return@withContext image
  }

  private suspend fun findCachedImage(episode: Episode, type: ImageType): Image {
    val cachedImage = localSource.showImages.getByEpisodeId(episode.ids.tmdb.id, type.key)
    return when (cachedImage) {
      null -> Image.createUnknown(type, EPISODE)
      else -> mappers.image.fromDatabase(cachedImage).copy(type = type)
    }
  }

  private fun findBestImage(images: List<TmdbImage>, type: ImageType): TmdbImage? {
    val language = translationsRepository.getLanguageCode()
    val comparator = when (type) {
      ImageType.POSTER -> compareBy<TmdbImage> { it.isLanguage(language) }
        .thenBy { it.isEnglish() }
        .thenBy { it.isPlain() }
      else -> compareBy<TmdbImage> { it.isPlain() }
        .thenBy { it.isLanguage(language) }
        .thenBy { it.isEnglish() }
    }
    return images.maxWithOrNull(comparator.thenBy { it.getVoteScore() })
  }
}
