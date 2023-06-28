//package com.navinfo.omqs.ui.other
//
//import androidx.lifecycle.*
//
//val vMStoreMap = HashMap<String, VMStoreClass>()
//
//inline fun <reified VM : ViewModel> LifecycleOwner.shareViewModels(
//    scopeName: String,
//    factory: ViewModelProvider.Factory? = null
//): Lazy<VM> {
//    val store: VMStoreClass
//    if (vMStoreMap.keys.contains(scopeName)) {
//        store = vMStoreMap[scopeName]!!
//    } else {
//        store = VMStoreClass()
//        vMStoreMap[scopeName] = store
//    }
//    store.register(this)
//    return ViewModelLazy(VM::class,
//        { store.viewModelStore },
//        { factory ?: ViewModelProvider.NewInstanceFactory() })
//}
//
//class VMStoreClass : ViewModelStoreOwner {
//
//    private val bindTargets = ArrayList<LifecycleOwner>()
//    private var vmStore: ViewModelStore? = null
//
//    fun register(host: LifecycleOwner) {
//        if (!bindTargets.contains(host)) {
//            bindTargets.add(host)
//            host.lifecycle.addObserver(object : LifecycleEventObserver {
//                override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
//                    if (event == Lifecycle.Event.ON_DESTROY) {
//                        host.lifecycle.removeObserver(this)
//                        bindTargets.remove(host)
//                        if (bindTargets.isEmpty()) {//如果当前商店没有关联对象，则释放资源
//                            vMStoreMap.entries.find { it.value == this@VMStoreClass }?.also {
//                                vmStore?.clear()
//                                vMStoreMap.remove(it.key)
//                            }
//                        }
//                    }
//                }
//            })
//        }
//    }
//
//    override fun getViewModelStore(): ViewModelStore {
//        if (vmStore == null)
//            vmStore = ViewModelStore()
//        return vmStore!!
//    }
//}