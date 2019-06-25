package dev.olog.msc.utils.k.extension

import android.view.View
import android.view.Window
import dev.olog.presentation.theme.isImmersiveMode
import dev.olog.shared.extensions.colorSurface
import dev.olog.shared.extensions.isDarkMode
import dev.olog.shared.utils.isMarshmallow
import dev.olog.shared.utils.isOreo

fun Window.setLightStatusBar(){
    decorView.systemUiVisibility = 0

    var flags = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)

    if (isImmersiveMode){
        flags = flags or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
    }

    if (isMarshmallow() && !context.isDarkMode()){
        flags = flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        if (isOreo()){
            navigationBarColor = context.colorSurface()
            if (!context.isDarkMode()){
                flags = flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            }
        }
    }
    decorView.systemUiVisibility = flags
}

fun Window.removeLightStatusBar(){
    decorView.systemUiVisibility = 0

    var flags = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)

    if (isImmersiveMode){
        flags = flags or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
    }

    if (isMarshmallow() && !context.isDarkMode()){
        if (isOreo()){
            navigationBarColor = context.colorSurface()
            if (!context.isDarkMode()){
                flags = flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            }
        }
    }
    decorView.systemUiVisibility = flags
}