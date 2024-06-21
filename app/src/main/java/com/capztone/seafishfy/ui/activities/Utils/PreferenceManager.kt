package com.capztone.seafishfy.ui.activities.Utils

import android.content.Context
import android.content.SharedPreferences

object PreferenceManager {
    private const val PREF_NAME = "MyAppPreferences"
    private const val KEY_FAVORITES = "favorites"

    private fun getSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun saveFavorites(context: Context, favorites: Set<String>) {
        val editor = getSharedPreferences(context).edit()
        editor.putStringSet(KEY_FAVORITES, favorites)
        editor.apply()
    }

    fun getFavorites(context: Context): Set<String>? {
        return getSharedPreferences(context).getStringSet(KEY_FAVORITES, emptySet())
    }
}
