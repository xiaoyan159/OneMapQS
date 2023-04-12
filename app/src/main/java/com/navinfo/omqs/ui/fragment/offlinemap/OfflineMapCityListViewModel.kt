package com.navinfo.omqs.ui.fragment.offlinemap

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.navinfo.omqs.db.RoomAppDatabase
import com.navinfo.omqs.bean.OfflineMapCityBean
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 离线地图城市列表viewModel
 */
@HiltViewModel
class OfflineMapCityListViewModel @Inject constructor(
    @ApplicationContext val context: Context,
    private val roomDatabase: RoomAppDatabase
) : ViewModel() {

    val cityListLiveData = MutableLiveData<List<OfflineMapCityBean>>()

    /**
     * 去获取离线地图列表
     */
    fun getCityList() {
        viewModelScope.launch(Dispatchers.IO) {
//            val realm = Realm.getDefaultInstance()
//            val objects = realm.where<OfflineMapCityBean>().findAll().sort("id", Sort.ASCENDING)
//            val list = realm.copyFromRealm(objects)
//            realm.close()
//            for (item in list) {
//                FileManager.checkOfflineMapFileInfo(item)
//            }
            val list = roomDatabase.getOfflineMapDao().getOfflineMapList()
            cityListLiveData.postValue(list)
        }
    }
}