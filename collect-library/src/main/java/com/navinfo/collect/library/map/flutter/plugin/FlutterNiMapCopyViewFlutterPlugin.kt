//package com.navinfo.collect.library.map.flutter.plugin
//
//import android.app.Activity
//import android.app.Application
//import android.os.Bundle
//import androidx.lifecycle.Lifecycle
//import androidx.lifecycle.LifecycleOwner
//import androidx.lifecycle.LifecycleRegistry
//import com.navinfo.collect.library.system.Constant
//import io.flutter.embedding.engine.FlutterEngine
//import io.flutter.embedding.engine.plugins.FlutterPlugin
//import io.flutter.embedding.engine.plugins.activity.ActivityAware
//import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
//import io.flutter.plugin.common.BinaryMessenger
//
//object FlutterNiMapCopyViewFlutterPlugin : ActivityAware, FlutterPlugin {
//    private const val NATIVE_VIEW_TYPE_ID = "com.navinfo.collect/copyView"
//    private var mLifecycle: Lifecycle? = null
//
//    //新方法
//    fun registerWith(flutterEngine: FlutterEngine, activity: Activity, rootPath: String = "") {
//        var lifecycleProxy: LifecycleProxy = if (activity is LifecycleOwner) {
//            object : LifecycleProxy {
//                override fun getLifecycle(): Lifecycle {
//                    return (activity as LifecycleOwner).lifecycle
//                }
//            }
//        } else {
//            ActivityLifecycleProxy(activity)
//        }
//        if (rootPath != "") {
//            Constant.ROOT_PATH = rootPath;
//        }
//        flutterEngine.platformViewsController.registry.registerViewFactory(
//            NATIVE_VIEW_TYPE_ID,
//            FlutterMapViewFactory(flutterEngine.dartExecutor.binaryMessenger, lifecycleProxy)
//        )
//    }
//
//    private class ActivityLifecycleProxy(activity: Activity) :
//        LifecycleProxy, Application.ActivityLifecycleCallbacks, LifecycleOwner {
//        private val mLifecycle = LifecycleRegistry(this)
//        private var mRegistrarActivityHashCode: Int = activity.hashCode()
//        override fun getLifecycle(): Lifecycle {
//            return mLifecycle
//        }
//
//        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
//            if (activity.hashCode() != mRegistrarActivityHashCode) {
//                return
//            }
//            mLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
//        }
//
//
//        override fun onActivityStarted(activity: Activity) {
//            if (activity.hashCode() != mRegistrarActivityHashCode) {
//                return
//            }
//            mLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
//        }
//
//        override fun onActivityResumed(activity: Activity) {
//            if (activity.hashCode() != mRegistrarActivityHashCode) {
//                return
//            }
//            mLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
//        }
//
//        override fun onActivityPaused(activity: Activity) {
//            if (activity.hashCode() != mRegistrarActivityHashCode) {
//                return
//            }
//            mLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
//        }
//
//        override fun onActivityStopped(activity: Activity) {
//            if (activity.hashCode() != mRegistrarActivityHashCode) {
//                return
//            }
//            mLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
//        }
//
//        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
//        override fun onActivityDestroyed(activity: Activity) {
//            if (activity.hashCode() != mRegistrarActivityHashCode) {
//                return
//            }
//            mLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
//        }
//
//        init {
//            activity.application.registerActivityLifecycleCallbacks(this)
//        }
//    }
//
//    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
//        mLifecycle = FlutterLifecycleAdapter.getActivityLifecycle(binding)
//    }
//
//    override fun onDetachedFromActivityForConfigChanges() {
//        onDetachedFromActivity()
//    }
//
//    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
//        onAttachedToActivity(binding)
//    }
//
//    override fun onDetachedFromActivity() {
//        mLifecycle = null
//    }
//
//    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
//        binding.platformViewRegistry.registerViewFactory(
//            NATIVE_VIEW_TYPE_ID,
//            FlutterMapViewFactory(binding.binaryMessenger, object : LifecycleProxy {
//                override fun getLifecycle(): Lifecycle? {
//                    return mLifecycle
//                }
//            })
//        )
//
//    }
//
//    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
//    }
//}