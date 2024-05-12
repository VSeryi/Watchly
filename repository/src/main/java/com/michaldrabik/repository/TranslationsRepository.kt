package com.michaldrabik.repository

import android.content.SharedPreferences
import com.michaldrabik.common.Config.DEFAULT_LANGUAGE_CODE
import com.michaldrabik.common.Config.DEFAULT_COUNTRY
import com.michaldrabik.common.Config.DEFAULT_LANGUAGE
import com.michaldrabik.common.ConfigVariant
import com.michaldrabik.common.extensions.nowUtcMillis
import com.michaldrabik.data_local.LocalDataSource
import com.michaldrabik.data_local.database.model.EpisodeTranslation
import com.michaldrabik.data_local.database.model.MovieTranslation
import com.michaldrabik.data_local.database.model.ShowTranslation
import com.michaldrabik.data_local.database.model.TranslationsMoviesSyncLog
import com.michaldrabik.data_local.database.model.TranslationsSyncLog
import com.michaldrabik.data_remote.RemoteDataSource
import com.michaldrabik.data_remote.trakt.model.Translation as TraktTranslation
import com.michaldrabik.repository.mappers.Mappers
import com.michaldrabik.repository.settings.SettingsRepository.Key.COUNTRY
import com.michaldrabik.repository.settings.SettingsRepository.Key.LANGUAGE_CODE
import com.michaldrabik.ui_model.Episode
import com.michaldrabik.ui_model.IdTrakt
import com.michaldrabik.ui_model.Movie
import com.michaldrabik.ui_model.Season
import com.michaldrabik.ui_model.SeasonTranslation
import com.michaldrabik.ui_model.Show
import com.michaldrabik.ui_model.Translation
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class TranslationsRepository @Inject constructor(
  @Named("miscPreferences") private var miscPreferences: SharedPreferences,
  private val remoteSource: RemoteDataSource,
  private val localSource: LocalDataSource,
  private val mappers: Mappers,
) {

  fun getLanguage() = Pair(getLanguageCode(), getCountry())

  fun getCountry() = miscPreferences.getString(COUNTRY, DEFAULT_COUNTRY) ?: DEFAULT_COUNTRY
  fun getLanguageCode() = miscPreferences.getString(LANGUAGE_CODE, DEFAULT_LANGUAGE_CODE) ?: DEFAULT_LANGUAGE_CODE

  suspend fun loadAllShowsLocal(language: Pair<String, String> = DEFAULT_LANGUAGE): Map<Long, Translation> {
    val local = localSource.showTranslations.getAll(language.first, language.second)
    return local.associate {
      Pair(it.idTrakt, mappers.translation.fromDatabase(it))
    }
  }

  suspend fun loadAllMoviesLocal(language: Pair<String, String> = DEFAULT_LANGUAGE): Map<Long, Translation> {
    val local = localSource.movieTranslations.getAll(language.first, language.second)
    return local.associate {
      Pair(it.idTrakt, mappers.translation.fromDatabase(it))
    }
  }

  suspend fun loadTranslation(
    show: Show,
    language: Pair<String, String> = DEFAULT_LANGUAGE,
    onlyLocal: Boolean = false,
  ): Translation? {
    val code = language.first
    val country = language.second

    val local = localSource.showTranslations.getById(show.traktId, code, country)
    local?.let {
      return mappers.translation.fromDatabase(it)
    }
    if (onlyLocal) return null

    val timestamp = localSource.translationsShowsSyncLog.getById(show.traktId)?.syncedAt ?: 0
    if (nowUtcMillis() - timestamp < ConfigVariant.TRANSLATION_SYNC_SHOW_MOVIE_COOLDOWN) {
      return Translation.EMPTY
    }

    val remoteTranslation = try {
      findTranslation(remoteSource.trakt.fetchShowTranslations(show.traktId, code), code, country)
    } catch (_: Throwable) {
      null
    }

    val translation = mappers.translation.fromNetwork(remoteTranslation)
    val translationDb = ShowTranslation.fromTraktId(
      show.traktId,
      translation.title,
      code,
      country,
      translation.overview,
      nowUtcMillis()
    )

    if (translationDb.overview.isNotBlank() || translationDb.title.isNotBlank()) {
      localSource.showTranslations.insertSingle(translationDb)
    }
    localSource.translationsShowsSyncLog.upsert(TranslationsSyncLog(show.traktId, nowUtcMillis()))

    return translation
  }

  suspend fun loadTranslation(
    movie: Movie,
    language: Pair<String, String> = DEFAULT_LANGUAGE,
    onlyLocal: Boolean = false,
  ): Translation? {
    val code = language.first
    val country = language.second

    val local = localSource.movieTranslations.getById(movie.traktId, code, country)
    local?.let {
      return mappers.translation.fromDatabase(it)
    }
    if (onlyLocal) return null

    val timestamp = localSource.translationsMoviesSyncLog.getById(movie.traktId)?.syncedAt ?: 0
    if (nowUtcMillis() - timestamp < ConfigVariant.TRANSLATION_SYNC_SHOW_MOVIE_COOLDOWN) {
      return Translation.EMPTY
    }

    val remoteTranslation = try {
      findTranslation(remoteSource.trakt.fetchMovieTranslations(movie.traktId, code), code, country)
    } catch (error: Throwable) {
      null
    }

    val translation = mappers.translation.fromNetwork(remoteTranslation)
    val translationDb = MovieTranslation.fromTraktId(
      movie.traktId,
      translation.title,
      code,
      country,
      translation.overview,
      nowUtcMillis()
    )

    if (translationDb.overview.isNotBlank() || translationDb.title.isNotBlank()) {
      localSource.movieTranslations.insertSingle(translationDb)
    }
    localSource.translationsMoviesSyncLog.upsert(TranslationsMoviesSyncLog(movie.traktId, nowUtcMillis()))

    return translation
  }

  suspend fun loadTranslation(
    episode: Episode,
    showId: IdTrakt,
    language: Pair<String, String> = DEFAULT_LANGUAGE,
    onlyLocal: Boolean = false,
  ): Translation? {
    val code = language.first
    val country = language.second

    val nowMillis = nowUtcMillis()
    val local = localSource.episodesTranslations.getById(episode.ids.trakt.id, showId.id, code, country)
    local?.let {
      val isCacheValid = nowMillis - it.updatedAt < ConfigVariant.TRANSLATION_SYNC_EPISODE_COOLDOWN
      if (it.title.isNotBlank() && it.overview.isNotBlank()) {
        return mappers.translation.fromDatabase(it)
      }
      if ((it.title.isNotBlank() || it.overview.isNotBlank()) && (isCacheValid || onlyLocal)) {
        return mappers.translation.fromDatabase(it)
      }
    }

    if (onlyLocal) return null

    val remoteTranslations = remoteSource.trakt.fetchSeasonTranslations(showId.id, episode.season, code)
      .map { mappers.translation.fromNetwork(it) }

    remoteTranslations
      .forEach { item ->
        val dbItem = EpisodeTranslation.fromTraktId(
          traktEpisodeId = item.ids.trakt.id,
          traktShowId = showId.id,
          title = item.title,
          overview = item.overview,
          language = code,
          country = country,
          createdAt = nowMillis
        )
        localSource.episodesTranslations.insertSingle(dbItem)
      }

    findTranslation(episode.ids.trakt.id, remoteTranslations, code, country)
      ?.let {
        return Translation(it.title, it.overview, it.language, it.country)
      }

    return null
  }

  suspend fun loadTranslations(
    season: Season,
    showId: IdTrakt,
    language: Pair<String, String> = DEFAULT_LANGUAGE,
  ): List<SeasonTranslation> {
    val code = language.first
    val country = language.second

    val episodes = season.episodes.toList()
    val episodesIds = season.episodes.map { it.ids.trakt.id }

    val local = localSource.episodesTranslations.getByIds(episodesIds, showId.id, code, country)
    val hasAllTranslated = local.isNotEmpty() && local.all { it.title.isNotBlank() && it.overview.isNotBlank() }
    val isCacheValid = local.isNotEmpty() && nowUtcMillis() - local.first().updatedAt < ConfigVariant.TRANSLATION_SYNC_EPISODE_COOLDOWN

    if (hasAllTranslated || isCacheValid) {
      return episodes.map { episode ->
        val translation = local.find { it.idTrakt == episode.ids.trakt.id }
        SeasonTranslation(
          ids = episode.ids.copy(),
          title = translation?.title ?: "",
          overview = translation?.overview ?: "",
          seasonNumber = season.number,
          episodeNumber = episode.number,
          language = code,
          country = country,
          isLocal = true
        )
      }
    }

    val remoteTranslation = remoteSource.trakt.fetchSeasonTranslations(showId.id, season.number, code)
      .map { mappers.translation.fromNetwork(it) }

    remoteTranslation
      .forEach { item ->
        val dbItem = EpisodeTranslation.fromTraktId(
          item.ids.trakt.id,
          showId.id,
          item.title,
          code,
          country,
          item.overview,
          nowUtcMillis()
        )
        localSource.episodesTranslations.insertSingle(dbItem)
      }

    return episodes.map { episode ->
      val translation = findTranslation(episode.ids.trakt.id, remoteTranslation, code, country)
      SeasonTranslation(
        ids = episode.ids.copy(),
        title = translation?.title ?: "",
        overview = translation?.overview ?: "",
        seasonNumber = season.number,
        episodeNumber = episode.number,
        language = code,
        country = country,
        isLocal = true
      )
    }
  }

  private fun findTranslation(translations: List<TraktTranslation>, code: String, country: String): TraktTranslation? {
    var fallback: TraktTranslation? = null
    for (t in translations) {
      if (t.country.equals(country, true)) return t
      if (fallback == null && t.language.equals(code, true)) fallback = t
    }
    return fallback
  }

  private fun findTranslation(episodeId: Long, translations: List<SeasonTranslation>, code: String, country: String): SeasonTranslation? {
    var fallback: SeasonTranslation? = null
    for (t in translations) {
      if (t.ids.trakt.id != episodeId) continue
      if (t.country.equals(country, true)) return t
      if (fallback == null && t.language.equals(code, true)) fallback = t
    }
    return fallback
  }
}
