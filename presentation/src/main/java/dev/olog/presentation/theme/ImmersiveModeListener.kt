package dev.olog.presentation.theme

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import dev.olog.core.dagger.ApplicationContext
import dev.olog.presentation.R
import javax.inject.Inject

class ImmersiveModeListener @Inject constructor(
    @ApplicationContext context: Context,
    prefs: SharedPreferences
) : BaseThemeUpdater(context, prefs, context.getString(R.string.prefs_immersive_key)) {

    private var currentActivity: Activity? = null

    override fun onPrefsChanged(forced: Boolean) {
        isImmersiveMode = prefs.getBoolean(key, false)
        if (!forced) {
            currentActivity?.recreate()
        }
    }

    fun setCurrentActivity(activity: Activity) {
        currentActivity = activity
    }
}

var isImmersiveMode = false