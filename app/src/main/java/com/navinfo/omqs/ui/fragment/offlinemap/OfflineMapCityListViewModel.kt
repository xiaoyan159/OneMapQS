package com.navinfo.omqs.ui.fragment.offlinemap

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.navinfo.collect.library.data.entity.OfflineMapCityBean
import com.navinfo.omqs.tools.FileManager
import com.navinfo.omqs.tools.RealmCoroutineScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.realm.Realm
import io.realm.Sort
import io.realm.kotlin.where
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 离线地图城市列表viewModel
 */
@HiltViewModel
class OfflineMapCityListViewModel @Inject constructor(
    @ApplicationContext val context: Context,
) : ViewModel() {

    val cityListLiveData = MutableLiveData<List<OfflineMapCityBean>>()

    /**
     * 去获取离线地图列表
     */
    fun getCityList() {
        viewModelScope.launch(Dispatchers.IO) {
            val realm = Realm.getDefaultInstance()
            val objects = realm.where<OfflineMapCityBean>().findAll().sort("id", Sort.ASCENDING)
            val list = realm.copyFromRealm(objects)
            realm.close()
            for (item in list) {
                FileManager.checkOfflineMapFileInfo(item)
            }
            cityListLiveData.postValue(list)
        }
    }
}