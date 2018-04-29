package dev.olog.msc.constants

import android.content.Context
import android.preference.PreferenceManager
import dev.olog.msc.R
import dev.olog.msc.presentation.widget.QuickActionView
import dev.olog.msc.utils.k.extension.updateNightMode

enum class Theme {
    DEFAULT, FLAT, SPOTIFY, FULLSCREEN, BIG_IMAGE;

    fun isDefault(): Boolean = this == DEFAULT

    fun isFlat(): Boolean = this == FLAT

    fun isSpotify(): Boolean = this == SPOTIFY

    fun isFullscreen(): Boolean = this == FULLSCREEN

    fun isBigImage(): Boolean = this == BIG_IMAGE

}

object AppConstants {

    private const val TAG = "AppConstants"
    const val ACTION_CONTENT_VIEW = "$TAG.action.content.view"

    var useFakeData = false

    const val SHORTCUT_SEARCH = "$TAG.shortcut.search"
    const val SHORTCUT_DETAIL = "$TAG.shortcut.detail"
    const val SHORTCUT_DETAIL_MEDIA_ID = "$TAG.shortcut.detail.media.id"
    const val SHORTCUT_PLAYLIST_CHOOSER = "$TAG.shortcut.playlist.chooser"

    var QUICK_ACTION = QuickActionView.Type.NONE
    var ICON_SHAPE = "round"

    var THEME = Theme.DEFAULT

    const val PROGRESS_BAR_INTERVAL = 250

    const val UNKNOWN = "<unknown>"
    lateinit var UNKNOWN_ALBUM: String
    lateinit var UNKNOWN_ARTIST: String

    fun initialize(context: Context){
        UNKNOWN_ALBUM = context.getString(R.string.common_unknown_album)
        UNKNOWN_ARTIST = context.getString(R.string.common_unknown_artist)

        updateQuickAction(context)
        updateIconShape(context)
        updateTheme(context)
    }

    fun updateQuickAction(context: Context){
        QUICK_ACTION = getQuickAction(context)
    }

    fun updateIconShape(context: Context){
        ICON_SHAPE = getIconShape(context)
    }

    fun updateNightMode(context: Context) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val isNightMode = prefs.getBoolean(context.getString(R.string.prefs_dark_theme_key), false)
        context.updateNightMode(isNightMode)
    }

    fun updateTheme(context: Context){
        THEME = getTheme(context)
    }

    private fun getTheme(context: Context): Theme {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val theme = prefs.getString(context.getString(R.string.prefs_appearance_key), context.getString(R.string.prefs_appearance_entry_value_default))
        return when (theme) {
            context.getString(R.string.prefs_appearance_entry_value_default) -> Theme.DEFAULT
            context.getString(R.string.prefs_appearance_entry_value_flat) -> Theme.FLAT
            context.getString(R.string.prefs_appearance_entry_value_spotify) -> Theme.SPOTIFY
            context.getString(R.string.prefs_appearance_entry_value_fullscreen) -> Theme.FULLSCREEN
            context.getString(R.string.prefs_appearance_entry_value_big_image) -> Theme.BIG_IMAGE
            else -> throw IllegalStateException("invalid theme=$theme")
        }
    }

    private fun getQuickAction(context: Context): QuickActionView.Type {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val quickAction = preferences.getString(context.getString(R.string.prefs_quick_action_key), context.getString(R.string.prefs_quick_action_entry_value_hide))
        return when (quickAction) {
            context.getString(R.string.prefs_quick_action_entry_value_hide) -> QuickActionView.Type.NONE
            context.getString(R.string.prefs_quick_action_entry_value_play) -> QuickActionView.Type.PLAY
            else ->  QuickActionView.Type.SHUFFLE
        }
    }

    private fun getIconShape(context: Context): String {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getString(context.getString(R.string.prefs_icon_shape_key), context.getString(R.string.prefs_icon_shape_rounded))
    }

}