package com.navinfo.omqs.ui.fragment.qsrecordlist

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.navinfo.collect.library.data.entity.QsRecordBean
import dagger.hilt.android.lifecycle.HiltViewModel
import io.realm.Realm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QsRecordListViewModel @Inject constructor(
) : ViewModel() {

    val liveDataQSList = MutableLiveData<List<QsRecordBean>>()

    fun getList(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val realm = Realm.getDefaultInstance()
            Log.e("jingo","realm hashCOde ${realm.hashCode()}")
            val objects = realm.where(QsRecordBean::class.java).findAll()
            liveDataQSList.postValue(realm.copyFromRealm(objects))
        }
    }

}
