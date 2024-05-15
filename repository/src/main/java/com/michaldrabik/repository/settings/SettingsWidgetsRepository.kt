package com.michaldrabik.repository.settings

import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import com.michaldrabik.common.Mode
import com.michaldrabik.ui_model.CalendarMode
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class SettingsWidgetsRepository @Inject constructor(
  @Named("miscPreferences") private var preferences: SharedPreferences
) {

  companion object Key {
    private const val THEME_WIDGET = "KEY_THEME_WIDGET"
    private const val THEME_WIDGET_TRANSPARENT = "KEY_THEME_WIDGET_TRANSPARENT"

    private const val WIDGET_CALENDAR_MODE = "WIDGET_CALENDAR_MODE"
    private const val WIDGET_CALENDAR_MOVIES_MODE = "WIDGET_CALENDAR_MOVIES_MODE"
  }

  var widgetsTheme: Int
    get() {
      return AppCompatDelegate.MODE_NIGHT_YES
    }
    set(value) = preferences.edit(true) { putInt(THEME_WIDGET, value) }

  var widgetsTransparency: Int
    get() {
      return 100
    }
    set(value) = preferences.edit(true) { putInt(THEME_WIDGET_TRANSPARENT, value) }

  fun getWidgetCalendarMode(mode: Mode, widgetId: Int): CalendarMode {
    val default = CalendarMode.PRESENT_FUTURE.name
    val key = when (mode) {
      Mode.SHOWS -> WIDGET_CALENDAR_MODE
      Mode.MOVIES -> WIDGET_CALENDAR_MOVIES_MODE
    }
    val value = preferences.getString("$key$widgetId", default) ?: default
    return CalendarMode.valueOf(value)
  }

  fun setWidgetCalendarMode(mode: Mode, widgetId: Int, calendarMode: CalendarMode) {
    val key = when (mode) {
      Mode.SHOWS -> WIDGET_CALENDAR_MODE
      Mode.MOVIES -> WIDGET_CALENDAR_MOVIES_MODE
    }
    preferences.edit(true) { putString("$key$widgetId", calendarMode.name) }
  }

  fun revokePremium() {
    widgetsTheme = AppCompatDelegate.MODE_NIGHT_YES
    widgetsTransparency = 100
  }
}
