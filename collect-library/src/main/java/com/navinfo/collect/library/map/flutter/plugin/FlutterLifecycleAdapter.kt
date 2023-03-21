package com.navinfo.collect.library.map.flutter.plugin

import androidx.annotation.NonNull
import androidx.lifecycle.Lifecycle
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.embedding.engine.plugins.lifecycle.HiddenLifecycleReference


class FlutterLifecycleAdapter {
    private val TAG = "FlutterLifecycleAdapter"

    /**
     * Returns the lifecycle object for the activity a plugin is bound to.
     *
     *
     * Returns null if the Flutter engine version does not include the lifecycle extraction code.
     * (this probably means the Flutter engine version is too old).
     */
    companion object {
        @NonNull
        fun getActivityLifecycle(
            @NonNull activityPluginBinding: ActivityPluginBinding
        ): Lifecycle {
            val reference = activityPluginBinding.lifecycle as HiddenLifecycleReference
            return reference.lifecycle
        }
    }

}