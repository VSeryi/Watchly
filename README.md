![Version](https://img.shields.io/github/v/tag/VSeryi/Watchly?label=version)
![Build]( https://img.shields.io/github/actions/workflow/status/VSeryi/Watchly/android.yml?branch=main)

# Watchly
<img src="https://i.ibb.co/k847xPY/ic-launcher.png" align="left" width="88" hspace="10" vspace="10" />

Watchly is an unofficial, experimental, unstable and personal fork of [Showly](https://play.google.com/store/apps/details?id=com.michaldrabik.showly2).

You can get the original and stable OSS version in [this repo](https://github.com/michaldrabik/showly-2.0/) completely free of all Google services. 

## Screenshots

<div>
   <img src="assets/screenshots/github1a.jpg" width="150" alt="Screenshot 1">
   <img src="assets/screenshots/github2a.jpg" width="150" alt="Screenshot 2">
   <img src="assets/screenshots/github3a.jpg" width="150" alt="Screenshot 3">
   <img src="assets/screenshots/github4a.jpg" width="150" alt="Screenshot 4">
</div>

## New features

1. Translations based in your country
2. Poster translations when available
3. Some other minor changes

## Project Setup

1. Clone repository and open project in the latest version of Android Studio.
2. Create `keystore.properties` file and put it in the `/app` folder.
3. Add following properties into `keystore.properties` file (values are not important at this moment):
```
keyAlias=github
keyPassword=github
storePassword=github
```
4. Add your [Trakt.tv](https://trakt.tv/oauth/applications), [TMDB](https://developers.themoviedb.org/3/), [OMDB](http://www.omdbapi.com) API keys as following properties into your `local.properties` file located in the root directory of the project:
```
traktClientId="your trakt client id"
traktClientSecret="your trakt client secret"
tmdbApiKey="your tmdb api key (v4)"
omdbApiKey="your omdb api key"
```
5. Rebuild and start the app.

## Issues & Contributions

Feel free to post problems with the app as Github [Issues](https://github.com/VSeryi/Watchly/issues).

Features ideas should be posted as new GIthub [Discussion](https://github.com/VSeryi/Watchly/discussions).

Pull requests are welcome. Remember about leaving a comment in the relevant issue if you are working on something.

### Language Translations

We're always looking for help with translating app into more languages.<br>
If you are interested in helping now or in the future, please visit the original CrowdIn project and join:<br>
https://crwd.in/showly-android-app

## FAQ

**1. Can I watch/stream/download shows and movies with Watchly app?**

  No, that is not possible. Watchly is a progress tracking type of app - not a streaming service.

**2. Show/Episode/Movie I'm looking for seems to be missing. What can I do?**

  Watchly uses [Trakt.tv](https://trakt.tv) as its main data source.
  If something is missing please use "Import Show" / "Import Movie" option located at the bottom of Trakt.tv website.
  It's also possible to contact Trakt.tv support about any related issue.

## Credits

  Thanks to [michaldrabik](https://github.com/michaldrabik) the original and main maintainer of [Showly 2.0](https://github.com/michaldrabik/showly-2.0)
