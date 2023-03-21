package com.navinfo.collect.library.map.flutter.plugin

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.navinfo.collect.FlutterBaseActivity
import com.navinfo.collect.library.system.Constant
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.BinaryMessenger

object FlutterMapViewFlutterPlugin : ActivityAware, FlutterPlugin {
    private const val NATIVE_VIEW_TYPE_ID = "com.navinfo.collect/mapView"
    private var mLifecycle: Lifecycle? = null
    private lateinit var mActivity:FlutterBaseActivity
//    //旧方法
//    fun registerWith(registry: PluginRegistry) {
//        val key = FlutterMapViewFlutterPlugin::class.java.canonicalName
//        if (registry.hasPlugin(key)) {
//            return
//        }
//        val registrar = registry.registrarFor(key)
//        registrar.platformViewRegistry()
//            .registerViewFactory(NATIVE_VIEW_TYPE_ID, FlutterMapViewFactory(registrar.messenger()))
//    }

    //新方法
    fun registerWith(
        flutterEngine: FlutterEngine,
        activity: FlutterBaseActivity,
        rootPath: String = ""
    ): FlutterMapViewFactory {
        mActivity = activity
        var lifecycleProxy: LifecycleProxy = if (activity is LifecycleOwner) {
            object : LifecycleProxy {
                override fun getLifecycle(): Lifecycle {
                    return (activity as LifecycleOwner).lifecycle
                }
            }
        } else {
            ActivityLifecycleProxy(activity)
        }
        if (rootPath != "") {
            Constant.ROOT_PATH = rootPath;
        }

        val factory =
            FlutterMapViewFactory(flutterEngine.dartExecutor.binaryMessenger, lifecycleProxy,activity)
        flutterEngine.platformViewsController.registry.registerViewFactory(
            NATIVE_VIEW_TYPE_ID,
            factory
        )
        return factory
    }

    private class ActivityLifecycleProxy(activity: Activity) :
        LifecycleProxy, Application.ActivityLifecycleCallbacks, LifecycleOwner {
        private val mLifecycle = LifecycleRegistry(this)
        private var mRegistrarActivityHashCode: Int = activity.hashCode()
        override fun getLifecycle(): Lifecycle {
            return mLifecycle
        }

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            if (activity.hashCode() != mRegistrarActivityHashCode) {
                return
            }
            mLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        }


        override fun onActivityStarted(activity: Activity) {
            if (activity.hashCode() != mRegistrarActivityHashCode) {
                return
            }
            mLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        }

        override fun onActivityResumed(activity: Activity) {
            if (activity.hashCode() != mRegistrarActivityHashCode) {
                return
            }
            mLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }

        override fun onActivityPaused(activity: Activity) {
            if (activity.hashCode() != mRegistrarActivityHashCode) {
                return
            }
            mLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        }

        override fun onActivityStopped(activity: Activity) {
            if (activity.hashCode() != mRegistrarActivityHashCode) {
                return
            }
            mLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        }

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
        override fun onActivityDestroyed(activity: Activity) {
            if (activity.hashCode() != mRegistrarActivityHashCode) {
                return
            }
            mLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        }

        init {
            activity.application.registerActivityLifecycleCallbacks(this)
        }
    }

    //首次绑定到Activity
    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        mLifecycle = FlutterLifecycleAdapter.getActivityLifecycle(binding)
    }

    //由于某些原因导致暂时解绑
    override fun onDetachedFromActivityForConfigChanges() {
        onDetachedFromActivity()
    }

    //恢复绑定
    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        onAttachedToActivity(binding)
    }

    //解绑
    override fun onDetachedFromActivity() {
        mLifecycle = null
    }

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
//        val mMessenger: BinaryMessenger = binding.binaryMessenger

        binding.platformViewRegistry.registerViewFactory(
            NATIVE_VIEW_TYPE_ID,
            FlutterMapViewFactory(binding.binaryMessenger, object : LifecycleProxy {
                override fun getLifecycle(): Lifecycle? {
                    return mLifecycle
                }
            }, mActivity)
        )


//        // 单独配置获取版本的消息通道,和地图实例的消息通道无关
//        val methodChannel = MethodChannel(
//            mMessenger,
//            Constants.NATIVE_SDK_VERSION_CHANNEL
//        )
//        methodChannel.setMethodCallHandler(this)

    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
//        val binaryMessenger = binding.binaryMessenger

//        mOfflineHandler.unInit(binaryMessenger)
    }
}