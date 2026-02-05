package com.example.myapplication.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.example.myapplication.MainActivity

object GhostModeManager {
    private const val GHOST_ALIAS = "com.example.myapplication.MainActivityGhost"

    fun isGhostModeEnabled(context: Context): Boolean {
        val ghostName = ComponentName(context, GHOST_ALIAS)
        val state = context.packageManager.getComponentEnabledSetting(ghostName)
        return state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
    }

    fun setGhostMode(context: Context, enabled: Boolean) {
        val pm = context.packageManager
        val normalName = ComponentName(context, MainActivity::class.java)
        val ghostName = ComponentName(context, GHOST_ALIAS)

        // Note: DONT_KILL_APP might still kill the app if the main activity is changed.
        // Usually modifying the enabled state of the current activity kills the app.
        // We accept this as "Restarting in Ghost Mode".
        
        if (enabled) {
            pm.setComponentEnabledSetting(
                ghostName,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
            pm.setComponentEnabledSetting(
                normalName,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        } else {
            pm.setComponentEnabledSetting(
                normalName,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
            pm.setComponentEnabledSetting(
                ghostName,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        }
    }
}
