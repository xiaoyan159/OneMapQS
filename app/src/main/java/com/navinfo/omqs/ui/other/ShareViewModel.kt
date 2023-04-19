package com.navinfo.omqs.ui.other

import androidx.annotation.MainThread
import androidx.fragment.app.Fragment
import androidx.fragment.app.createViewModelLazy
import androidx.lifecycle.*
import androidx.lifecycle.viewmodel.CreationExtras

/**
 * 用来共享的viewModel
 */
val vMStores = HashMap<String, ViewModelStoreOwner>()


@MainThread
inline fun <reified VM : ViewModel> Fragment.shareViewModels(
    scopeName: String,
    noinline ownerProducer: () -> ViewModelStoreOwner = { this },
    noinline factoryProducer: (() -> ViewModelProvider.Factory)? = null
): Lazy<VM> {
    val owner by lazy(LazyThreadSafetyMode.NONE) { ownerProducer() }

    val store: ViewModelStoreOwner
    if (vMStores.keys.contains(scopeName)) {
        store = vMStores[scopeName]!!
    } else {
        vMStores[scopeName] = owner
        store = owner
        this.let { fragment ->
            fragment.lifecycle.addObserver(object : LifecycleEventObserver {
                override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                    if (event == Lifecycle.Event.ON_DESTROY) {
                        fragment.lifecycle.removeObserver(this)
                        store.viewModelStore.clear()
                        vMStores.remove(scopeName)
                    }
                }
            })
        }


    }
//    store.register(this)

    return createViewModelLazy(
        VM::class,
        { store.viewModelStore },
        {
            (store as? HasDefaultViewModelProviderFactory)?.defaultViewModelCreationExtras
                ?: CreationExtras.Empty
        },
        factoryProducer ?: {
            (store as? HasDefaultViewModelProviderFactory)?.defaultViewModelProviderFactory
                ?: defaultViewModelProviderFactory
        })
}

//
//@MainThread
//inline fun <reified VM : ViewModel> LifecycleOwner.shareViewModels(
//    scopeName: String,
//    factory: ViewModelProvider.Factory? = null
//): Lazy<VM> {
//    val store: VMStore
//    if (vMStores.keys.contains(scopeName)) {
//        store = vMStores[scopeName]!!
//    } else {
//        store = VMStore()
//        vMStores[scopeName] = store
//    }
//    store.register(this)
//    return ViewModelLazy(VM::class,
//        { store.viewModelStore },
//        { factory ?: MyViewModelFactory() })
//}


class MyViewModelFactory(
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return modelClass.getConstructor().newInstance()
    }
}

class VMStore(val owner: ViewModelStoreOwner) {

//    private val bindTargets = ArrayList<LifecycleOwner>()
//    fun register(host: LifecycleOwner) {
//        if (!bindTargets.contains(host)) {
//            bindTargets.add(host)
//            host.lifecycle.addObserver(object : LifecycleEventObserver {
//                override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
//                    if (event == Lifecycle.Event.ON_DESTROY) {
//                        host.lifecycle.removeObserver(this)
//                        bindTargets.remove(host)
//                        if (bindTargets.isEmpty()) {//如果当前商店没有关联对象，则释放资源
//                            vMStores.entries.find { it.value == this@VMStore }?.also {
//                                owner.viewModelStore.clear()
//                                vMStores.remove(it.key)
//                            }
//                        }
//                    }
//                }
//            })
//        }
//    }
}