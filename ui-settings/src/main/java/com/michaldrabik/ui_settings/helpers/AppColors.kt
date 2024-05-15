package com.michaldrabik.ui_settings.helpers

import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
import com.michaldrabik.ui_settings.R

enum class AppColors(
  val code: Int,
  @StringRes val displayName: Int
) {
  STATIC(0, R.string.textColorsStatic),
  DYNAMIC(1, R.string.textColorsDynamic);

  companion object {
    fun fromCode(code: Int) = values().first { it.code == code }
  }
}
