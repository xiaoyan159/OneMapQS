package com.navinfo.omqs.ui.fragment.offlinemap

import android.app.Application
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.navinfo.omqs.http.NetResult
import com.navinfo.omqs.http.NetworkService
import com.navinfo.omqs.bean.OfflineMapCityBean
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 离线地图城市列表viewModel
 */
@HiltViewModel
class OfflineMapCityListViewModel @Inject constructor(
    private val networkService: NetworkService,
    @ApplicationContext val context: Context
) : ViewModel() {

    val cityListLiveData = MutableLiveData<List<OfflineMapCityBean>>()

    /**
     * 去获取离线地图列表
     */
    fun getCityList() {
        viewModelScope.launch {
            when (val result = networkService.getOfflineMapCityList()) {
                is NetResult.Success -> {
                    cityListLiveData.postValue(result.data?.sortedBy { bean -> bean.id })
                }
                is NetResult.Error -> {
                    Toast.makeText(context, "${result.exception.message}", Toast.LENGTH_SHORT)
                        .show()
                }
                is NetResult.Failure -> {
                    Toast.makeText(context, "${result.code}:${result.msg}", Toast.LENGTH_SHORT)
                        .show()
                }
                NetResult.Loading -> {}
            }
        }
    }
}