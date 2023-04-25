package com.navinfo.omqs.ui.fragment.qsrecordlist

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.findNavController
import com.blankj.utilcode.util.ToastUtils
import com.navinfo.collect.library.data.entity.QsRecordBean
import com.navinfo.omqs.R
import com.navinfo.omqs.ui.activity.map.MainActivity
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

    fun onItemClickListener(activity: MainActivity, position :Int){
        val naviController = activity.findNavController(R.id.main_activity_right_fragment)
        val bundle = Bundle()
        bundle.putString("QsId", liveDataQSList.value?.get(position)?.id)
        naviController.navigate(R.id.EvaluationResultFragment, bundle)
        ToastUtils.showLong(liveDataQSList.value?.get(position)?.classType)
    }
}
