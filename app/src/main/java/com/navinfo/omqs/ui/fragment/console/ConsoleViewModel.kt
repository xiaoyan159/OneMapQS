package com.navinfo.omqs.ui.fragment.console

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.navinfo.collect.library.data.entity.QsRecordBean
import com.navinfo.collect.library.data.entity.TaskBean
import com.navinfo.omqs.tools.FileManager
import com.navinfo.omqs.util.DateTimeUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import io.realm.Realm
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConsoleViewModel @Inject constructor() : ViewModel() {
    /**
     * 当前任务量统计
     */
    val liveDataTaskCount = MutableLiveData(0)

    /**
     * 作业数据统计
     */
    val liveDataEvaluationResultCount = MutableLiveData(0)

    init {
        viewModelScope.launch {
            val realm = Realm.getDefaultInstance()
            val nowTime: Long = DateTimeUtil.getNowDate().time
            val beginNowTime: Long = nowTime - 90 * 3600 * 24 * 1000L
            val syncUpload: Int = FileManager.Companion.FileUploadStatus.DONE
            val count =
                realm.where(TaskBean::class.java).notEqualTo("syncStatus", syncUpload).or()
                    .between("operationTime", beginNowTime, nowTime)
                    .equalTo("syncStatus", syncUpload).count()
            liveDataTaskCount.postValue(count.toInt())
            val count2 = realm.where(QsRecordBean::class.java).count()
            liveDataEvaluationResultCount.postValue(count2.toInt())
            realm.close()
        }
    }
}