package com.anisync.android.data

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.edit
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.anisync.android.MainActivity
import com.anisync.android.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** The launcher icons the app ships, each backed by an `<activity-alias>` in the manifest. */
enum class AppIcon(
    val aliasName: String,
    @StringRes val label: Int,
    @DrawableRes val background: Int,
    @DrawableRes val foreground: Int
) {
    INK("LauncherIconInk", R.string.app_icon_ink, R.drawable.ic_launcher_background, R.drawable.ic_launcher_foreground),
    WHITE("LauncherIconWhite", R.string.app_icon_white, R.drawable.ic_launcher_background_white, R.drawable.ic_launcher_foreground_white),
    BLUE("LauncherIconBlue", R.string.app_icon_blue, R.drawable.ic_launcher_background_blue, R.drawable.ic_launcher_foreground_blue),
    SYNC("LauncherIconSync", R.string.app_icon_sync, R.drawable.ic_launcher_background_blue, R.drawable.ic_launcher_foreground_sync)
}

/**
 * Switches the launcher icon by enabling one activity alias and disabling the rest.
 *
 * Disabling the alias the app was launched through finishes its task, so a choice is only recorded
 * here and the aliases are swapped once the app is in the background. The choice is persisted, so
 * one made just before the process dies is still applied the next time the app is left.
 */
@Singleton
class AppIconManager @Inject constructor(
    @ApplicationContext private val context: Context
) : DefaultLifecycleObserver {
    private val packageManager = context.packageManager
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _current = MutableStateFlow(
        prefs.getString(KEY_ICON, null)
            ?.let { name -> AppIcon.entries.firstOrNull { it.name == name } }
            ?: enabledIcon()
    )

    /** The icon the user chose, which the launcher shows once the app has been backgrounded. */
    val current: StateFlow<AppIcon> = _current.asStateFlow()

    fun bindTo(processLifecycle: Lifecycle) {
        processLifecycle.addObserver(this)
    }

    fun setIcon(icon: AppIcon) {
        prefs.edit { putString(KEY_ICON, icon.name) }
        _current.value = icon
    }

    override fun onStop(owner: LifecycleOwner) {
        val icon = _current.value
        if (icon == enabledIcon()) return
        // The new alias goes live before the old one is turned off, so there is never a moment with
        // no launcher entry at all.
        setEnabled(icon, true)
        AppIcon.entries.filter { it != icon }.forEach { setEnabled(it, false) }
    }

    private fun enabledIcon(): AppIcon =
        AppIcon.entries.firstOrNull { isEnabled(it) } ?: AppIcon.INK

    private fun isEnabled(icon: AppIcon): Boolean =
        when (packageManager.getComponentEnabledSetting(componentFor(icon))) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
            PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> icon == AppIcon.INK
            else -> false
        }

    private fun setEnabled(icon: AppIcon, enabled: Boolean) {
        packageManager.setComponentEnabledSetting(
            componentFor(icon),
            if (enabled) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            },
            PackageManager.DONT_KILL_APP
        )
    }

    // The alias lives in the code namespace, which the debug and preview builds do not share with
    // their application id, so the class name is built from MainActivity rather than packageName.
    private fun componentFor(icon: AppIcon) = ComponentName(
        context.packageName,
        "${MainActivity::class.java.name.substringBeforeLast('.')}.${icon.aliasName}"
    )

    private companion object {
        const val PREFS_NAME = "app_icon"
        const val KEY_ICON = "icon"
    }
}
