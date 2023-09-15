package com.navinfo.omqs.ui.activity.login

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blankj.utilcode.util.ResourceUtils
import com.navinfo.collect.library.data.entity.LinkInfoBean
import com.navinfo.collect.library.data.entity.ReferenceEntity
import com.navinfo.collect.library.data.entity.RenderEntity
import com.navinfo.collect.library.data.entity.TaskBean
import com.navinfo.omqs.Constant
import com.navinfo.omqs.bean.LoginUserBean
import com.navinfo.omqs.bean.SysUserBean
import com.navinfo.omqs.db.RoomAppDatabase
import com.navinfo.omqs.http.DefaultResponse
import com.navinfo.omqs.http.NetResult
import com.navinfo.omqs.http.NetworkService
import com.navinfo.omqs.tools.FileManager
import com.navinfo.omqs.util.DateTimeUtil
import com.navinfo.omqs.util.NetUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import io.realm.Realm
import io.realm.RealmConfiguration
import kotlinx.coroutines.*
import java.io.File
import java.io.IOException
import javax.inject.Inject

enum class LoginStatus {
    /**
     * 访问服务器登陆中
     */
    LOGIN_STATUS_NET_LOADING,

    /**
     * 访问离线地图列表
     */
    LOGIN_STATUS_NET_OFFLINE_MAP,

    /**
     * 访问任务列表
     */
    LOGIN_STATUS_NET_GET_TASK_LIST,

    /**
     * 初始化文件夹
     */
    LOGIN_STATUS_FOLDER_INIT,

    /**
     * 创建文件夹失败
     */
    LOGIN_STATUS_FOLDER_FAILURE,

    /**
     * 网络访问失败
     */
    LOGIN_STATUS_NET_FAILURE,

    /**
     * 成功
     */
    LOGIN_STATUS_SUCCESS,

    /**
     * 取消
     */
    LOGIN_STATUS_CANCEL,
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val networkService: NetworkService,
    private val roomAppDatabase: RoomAppDatabase
) : ViewModel() {
    //用户信息
    val loginUser: MutableLiveData<LoginUserBean> = MutableLiveData()

    //是不是登录成功
    val loginStatus: MutableLiveData<LoginStatus> = MutableLiveData()

    var jobLogin: Job? = null

    var sharedPreferences: SharedPreferences? = null

    var dataIndex = 0

    init {
        loginUser.value = LoginUserBean(userCode = "haofuyue00213", passWord = "123456")
    }


    /**
     * 处理注册按钮
     */
    fun onClick(view: View) {
        loginUser.value!!.userCode = "admin2"
        loginUser.value = loginUser.value
    }

    /**
     * 点击
     */
    fun onClickLoginBtn(context: Context, userName: String, password: String) {
        if (userName.isEmpty()) {
            Toast.makeText(context, "请输入用户名", Toast.LENGTH_SHORT).show()
        }
        if (password.isEmpty()) {
            Toast.makeText(context, "请输入密码", Toast.LENGTH_SHORT).show()
        }
        sharedPreferences =
            context.getSharedPreferences("USER_SHAREDPREFERENCES", Context.MODE_PRIVATE)
        val userNameCache = sharedPreferences?.getString("userName", null)
        val passwordCache = sharedPreferences?.getString("passWord", null)
        val userCodeCache = sharedPreferences?.getString("userCode", null)
        val userRealName = sharedPreferences?.getString("userRealName", null)
        //增加缓存记录，不用每次连接网络登录
        if (userNameCache != null && passwordCache != null && userCodeCache != null && userRealName != null) {
            if (userNameCache == userName && passwordCache == password) {
                viewModelScope.launch(Dispatchers.IO) {
                    createUserFolder(context, userCodeCache, userRealName)
                    getOfflineCityList(context)
//                    loginStatus.postValue(LoginStatus.LOGIN_STATUS_SUCCESS)
                }
                return
            }
        }
        //不指定IO，会在主线程里运行
        jobLogin = viewModelScope.launch(Dispatchers.IO) {
            loginCheck(context, userName, password)
        }

    }

    /**
     * 如果不用挂起函数的方式，直接把下面这段代码替换到上面，在delay之后，线程和delay之前不是同一个，有啥影响未知。。。
     */

    private suspend fun loginCheck(context: Context, userName: String, password: String) {
        //上面调用了线程切换，这里不用调用，即使调用了还是在同一个线程中，除非自定义协程域？（待验证）
//        withContext(Dispatchers.IO) {
        //网络访问
        loginStatus.postValue(LoginStatus.LOGIN_STATUS_NET_LOADING)
        var userCode = "99999";
        var userRealName = "";
        //登录访问
        when (val result = networkService.loginUser(LoginUserBean(userName, password))) {
            is NetResult.Success<*> -> {
                if (result.data != null) {
                    try {
                        val defaultUserResponse = result.data as DefaultResponse<SysUserBean>
                        if (defaultUserResponse.success) {
                            if (defaultUserResponse.obj == null || defaultUserResponse.obj!!.userCode == null) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        context,
                                        "服务返回用户Code信息错误",
                                        Toast.LENGTH_SHORT
                                    )
                                        .show()
                                }
                                loginStatus.postValue(LoginStatus.LOGIN_STATUS_CANCEL)
                                return
                            } else {
                                userCode = defaultUserResponse.obj?.userCode.toString()
                                userRealName = defaultUserResponse.obj?.userName.toString()
                                folderInit(
                                    context = context,
                                    userName = userName,
                                    password = password,
                                    userCode = userCode,
                                    userRealName = userRealName
                                )
                                getOfflineCityList(context)
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    context,
                                    "${defaultUserResponse.msg}",
                                    Toast.LENGTH_SHORT
                                )
                                    .show()
                            }
                            loginStatus.postValue(LoginStatus.LOGIN_STATUS_CANCEL)
                            return
                        }

                    } catch (e: IOException) {
                        loginStatus.postValue(LoginStatus.LOGIN_STATUS_FOLDER_FAILURE)
                    }
                }
            }

            is NetResult.Error<*> -> {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "${result.exception.message}", Toast.LENGTH_SHORT)
                        .show()
                }
                loginStatus.postValue(LoginStatus.LOGIN_STATUS_CANCEL)
                return
            }

            is NetResult.Failure<*> -> {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "${result.code}:${result.msg}", Toast.LENGTH_SHORT)
                        .show()
                }
                loginStatus.postValue(LoginStatus.LOGIN_STATUS_CANCEL)
                return
            }

            else -> {}
        }

    }

    /**
     * 获取离线地图
     */
    private suspend fun getOfflineCityList(context: Context) {

        loginStatus.postValue(LoginStatus.LOGIN_STATUS_NET_OFFLINE_MAP)
        when (val result = networkService.getOfflineMapCityList()) {
            is NetResult.Success -> {

                if (result.data != null) {
                    for (cityBean in result.data) {
                        FileManager.checkOfflineMapFileInfo(cityBean)
                    }
                    roomAppDatabase.getOfflineMapDao().insertOrUpdate(result.data)
                }
                getTaskList(context)
            }

            is NetResult.Error<*> -> {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "${result.exception.message}", Toast.LENGTH_SHORT)
                        .show()
                }
                getTaskList(context)
            }

            is NetResult.Failure<*> -> {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "${result.code}:${result.msg}", Toast.LENGTH_SHORT)
                        .show()
                }
                getTaskList(context)
            }

            is NetResult.Loading -> {}
        }

    }

    /**
     * 获取任务列表
     */
    private suspend fun getTaskList(context: Context) {
        loginStatus.postValue(LoginStatus.LOGIN_STATUS_NET_GET_TASK_LIST)
        when (val result = networkService.getTaskList(Constant.USER_ID)) {
            is NetResult.Success -> {
                if (result.data != null) {
                    val realm = Realm.getDefaultInstance()
                    realm.executeTransaction {
                        result.data.obj?.let { list ->
                            for (index in list.indices) {
                                val task = list[index]
                                val item = realm.where(TaskBean::class.java).equalTo(
                                    "id", task.id
                                ).findFirst()
                                if (item != null) {
                                    task.fileSize = item.fileSize
                                    task.status = item.status
                                    task.currentSize = item.currentSize
                                    task.hadLinkDvoList = item.hadLinkDvoList
                                    //已上传后不在更新操作时间
                                    if (task.syncStatus != FileManager.Companion.FileUploadStatus.DONE) {
                                        //赋值时间，用于查询过滤
                                        task.operationTime = DateTimeUtil.getNowDate().time
                                    }
                                } else {
                                    for (hadLink in task.hadLinkDvoList) {
                                        hadLink.taskId = task.id
                                    }
                                    //赋值时间，用于查询过滤
                                    task.operationTime = DateTimeUtil.getNowDate().time
                                }
                                realm.copyToRealmOrUpdate(task)
                            }
                        }

                    }
                    realm.close()
                }
                //测试代码
/*                viewModelScope.launch(Dispatchers.IO) {

                    Log.e("qj", "test===开始安装")

                    for (i in 0 until 30) {
                        val userTaskFolder = File(Constant.USER_DATA_PATH+"/$i")
                        if (!userTaskFolder.exists()) userTaskFolder.mkdirs()
                        val password = "encryp".encodeToByteArray().copyInto(ByteArray(64))
                        val config = RealmConfiguration.Builder()
                            .directory(userTaskFolder)
                            .name("OMQS.realm")
                            .encryptionKey(password)
                            .allowQueriesOnUiThread(true)
                            .schemaVersion(2)
                            .build()

                        var realm = Realm.getInstance(config)
                        Realm.compactRealm(config)
*//*                        realm.beginTransaction()
                        for (j in 0 until 30000) {
                            //if(i!=1){
                            val renderEntity = RenderEntity()
                            renderEntity.geometry = "LINESTRING(116.23932262211743 40.12579920189081 35.74, 116.23931822895703 40.12581470592407 35.76, 116.23930624771914 40.12585781700239 35.79, 116.23928258433983 40.125939338575144 35.89, 116.23926850609101 40.12598745138618 35.96, 116.23924414376185 40.126070773581354 36.06, 116.23922856776639 40.12612328771694 36.12, 116.2392046045976 40.126203109397004 36.24, 116.23918922835792 40.12625522340176 36.32, 116.23916396702913 40.12633774615302 36.45, 116.23914859055587 40.126387759995225 36.55, 116.2391203337466 40.12647908540596 36.67, 116.23910755319447 40.12651959683855 36.75, 116.2390815927516 40.12660212010318 36.9, 116.23906871229808 40.12664233159039 36.99, 116.23903915709849 40.126733957916805 37.18, 116.23902338079155 40.12678097181321 37.28, 116.23899562279767 40.12686599649683 37.47, 116.23897994641094 40.12691301036026 37.57, 116.23895278734514 40.12699443439999 37.74, 116.23893721087765 40.12704144822964 37.87, 116.23890605755072 40.127132075611264 38.07, 116.23889088041396 40.1271770890434 38.18, 116.23887460471097 40.12722350328997 38.28, 116.2388636211133 40.12725481290913 38.35, 116.23884574788786 40.12730632862395 38.48, 116.23881908762554 40.12738135192382 38.65, 116.23881679105517 40.12738785393584 38.67, 116.23879302637583 40.12745367463593 38.83, 116.23877944663403 40.127491786520146 38.92, 116.23877095932345 40.127515793968584 38.97, 116.23875098890943 40.12757001129976 39.11, 116.2387227308574 40.12764713588771 39.28, 116.2387081525541 40.127687248614656 39.37, 116.23867370351799 40.12777927847206 39.58, 116.2386545318675 40.12783019508165 39.72, 116.23862417686668 40.12791142146556 39.92, 116.23860640319212 40.12795863689979 40.04, 116.23859292304284 40.127993448523135 40.12, 116.23857504944692 40.12804016399674 40.24, 116.23855657681928 40.128088980051075 40.34, 116.23853031552511 40.12815650272686 40.51, 116.23850904685051 40.12821082107825 40.64, 116.2384813875936 40.12828124495082 40.79, 116.23845742281154 40.12834136557736 40.92, 116.23842427149486 40.12842389408986 41.14, 116.23840380160156 40.1284750117316 41.26, 116.23837174854405 40.128553139214226 41.45, 116.2383542741467 40.12859565420684 41.55, 116.23831942516678 40.12867978408208 41.76, 116.23830025323893 40.128725900522745 41.88, 116.23826470526342 40.12881083099175 42.05, 116.23824683139586 40.12885314629496 42.16, 116.23821018495624 40.128939377661524 42.37, 116.23818931532472 40.128987195429495 42.48, 116.23815506532867 40.12906662471337 42.66, 116.23813779047674 40.12910593943215 42.75, 116.23813469496669 40.12911284205781 42.77, 116.23809884729717 40.12919517271111 42.95, 116.23808067375946 40.12923618820391 43.04, 116.2380433282688 40.129321120140304 43.22, 116.23801996225431 40.12937304003738 43.34, 116.23798521289724 40.129450669705015 43.5, 116.23796793804728 40.12948888443628 43.6, 116.23794277466426 40.12954420588348 43.69, 116.23792699756609 40.129578319290346 43.76, 116.23790423067194 40.129627838680705 43.88, 116.23786848266316 40.12970526913737 44, 116.2378471137247 40.129751187332936 44.1, 116.23780657269158 40.129838121884916 44.27, 116.23778390560862 40.12988604116754 44.34, 116.23774725889854 40.12996367239866 44.44, 116.23772349342555 40.130013492631235 44.55, 116.23768435029899 40.1300951259634 44.66, 116.23766248205574 40.13014034457485 44.71, 116.23762343873513 40.13022047779343 44.81, 116.23760077162373 40.13026659706827 44.86, 116.23755973121209 40.13034993200096 44.94, 116.2375357659646 40.130397952369805 45, 116.23749612344814 40.13047688606479 45.07, 116.2374712595042 40.130526107202286 45.09, 116.23743321468324 40.13060103955511 45.14, 116.23740815097895 40.13064966082994 45.19, 116.23736870811013 40.13072579432282 45.22, 116.23734823785306 40.13076581177513 45.23, 116.23730330299801 40.13085194998242 45.26, 116.23727863880121 40.13089927099006 45.27, 116.23723709907071 40.13097820635225 45.29, 116.23721153611818 40.13102612808514 45.31, 116.23717129448862 40.13110146233737 45.34, 116.23714573160329 40.13114928412407 45.33, 116.23710359278992 40.1312276200455 45.32, 116.23707812976424 40.13127464175462 45.29, 116.23703688966032 40.13135037692713 45.3, 116.23701032824796 40.13139889959046 45.29, 116.23696699119911 40.13147743656524 45.27, 116.23694162802717 40.13152295819616 45.24, 116.23689978876293 40.13159739386983 45.2, 116.23687302763048 40.13164481670404 45.16, 116.23683009001913 40.13172055336384 45.12, 116.23680592516881 40.13176317402547 45.08, 116.23676029149024 40.131842613004956 45.02, 116.23673343055732 40.1318892359725 44.98, 116.2366884959176 40.131966674401994 44.9, 116.23666123557125 40.13201329771908 44.84, 116.2366155021779 40.13209153690089 44.77, 116.23658744310768 40.13213966099004 44.69, 116.23653821483212 40.132222703193655 44.58, 116.23651345088638 40.132263924406 44.53, 116.23646572044586 40.132343265344616 44.4, 116.23644225475998 40.13238288555961 44.34, 116.23639053016349 40.132467629947804 44.19, 116.23636526707928 40.132509351694566 44.12, 116.23631583935669 40.13259069428363 43.96, 116.23630775110645 40.13260330119531 43.94, 116.2362904763613 40.13263141607684 43.87, 116.23624194730323 40.13270975788138 43.73, 116.23621518647366 40.13275298097316 43.63, 116.23616885425847 40.1328269209311 43.46, 116.23613919766159 40.13287394652052 43.34, 116.23609236625721 40.132947986974415 43.16, 116.2360636083881 40.132993111823716 43.06, 116.23601388134549 40.133071254874444 42.86, 116.23598522335485 40.133115579659105 42.73, 116.23593629520064 40.13319152206869 42.51, 116.2359073376966 40.13323584715097 42.4, 116.23585751089514 40.13331199036941 42.2, 116.2358283537537 40.13335631567969 42.08, 116.23577752849239 40.13343305982414 41.89, 116.23574907033701 40.13347558454054 41.78, 116.23569135533083 40.133561934767584 41.55, 116.2356704860765 40.13359295293967 41.47, 116.2356620984319 40.13360536024213 41.44, 116.23562714986441 40.1336563906336 41.31, 116.23561097370154 40.13368010472439 41.26, 116.23557981965153 40.13372583189419 41.16, 116.23552599918413 40.13380517894775 41.01, 116.2354947453622 40.13385070626818 40.91, 116.23544961210217 40.133916245764205 40.78, 116.23541346555174 40.133968177396326 40.67, 116.23536124285798 40.134043023162974 40.55, 116.23533018882797 40.13408705037923 40.47, 116.23527736714453 40.134162096767966 40.34, 116.2352552998847 40.13419331616499 40.26, 116.23522664242158 40.13423374136985 40.19, 116.23518879879714 40.13428827480866 40.08, 116.23516094022496 40.134327499370315 40.02, 116.23515315178082 40.134338206217436 40.01, 116.23510702038857 40.13440244689625 39.91, 116.23507656573804 40.13444487379488 39.85, 116.23501915123302 40.134523924507356 39.74, 116.23498659972418 40.134568353274716 39.71, 116.23493118238048 40.134643902328826 39.59, 116.23490012872165 40.13468592983577 39.56, 116.23484541049838 40.1347600783976 39.48, 116.23481196049558 40.13480490808954 39.42, 116.2347562438746 40.134879457623846 39.35, 116.23472399213878 40.134922086297735 39.32, 116.23466937400401 40.13499443495665 39.25, 116.23463612389497 40.135038364620286 39.22, 116.23458060719781 40.13511081415106 39.17, 116.23454815594212 40.13515304314287 39.15, 116.23449453661144 40.13522299114498 39.12, 116.23445998857872 40.13526742206711 39.12, 116.23440287457339 40.13534057325135 39.06, 116.23437012390069 40.13538210261424 39.04, 116.23431271047467 40.135454754166325 39.02, 116.23427696447838 40.13550018634124 39.02, 116.2342206496235 40.13557133708173 38.99, 116.23418640148425 40.135614668000656 38.96, 116.23412729100342 40.135688521373666 38.93, 116.2340941412789 40.135729951361185 38.92, 116.23403642881772 40.13580120357739 38.89, 116.2340024803726 40.13584303433998 38.87, 116.23394386947226 40.135914987521076 38.83, 116.23390942186808 40.135956918799764 38.83, 116.23385021201584 40.136028672630154 38.77, 116.23381576447238 40.1360699039556 38.76, 116.23375655483791 40.13614135795286 38.69, 116.23368656175582 40.13622492185699 38.67, 116.23366150009389 40.13625484478065 38.66, 116.23362705279939 40.13629567629675 38.65, 116.2335664455895 40.13636693179199 38.62, 116.23353259752703 40.1364070628737 38.61, 116.2334654006275 40.13648542457216 38.57, 116.23343265096818 40.13652375471296 38.55, 116.2333714450715 40.136594511059485 38.52, 116.23333530070481 40.136636144381406 38.51, 116.2332754928505 40.136704699587305 38.48, 116.23323785095296 40.13674803441519 38.45, 116.2331750479065 40.1368192925331 38.44, 116.23314100054348 40.13685792409827 38.39, 116.23307889661602 40.136927781720296 38.37, 116.23304285245472 40.136968315224024 38.37, 116.23297905136549 40.13703947458278 38.34, 116.23294170930376 40.13708080936036 38.32, 116.23288080391845 40.13714806617189 38.28, 116.23284146509452 40.13719130291544 38.25, 116.23278075962877 40.137257959715654 38.22, 116.2327431182934 40.13729899497125 38.18, 116.23267782018671 40.1373699562259 38.16, 116.23263388881038 40.13741739749128 38.11, 116.23257288424678 40.13748325490289 38.07, 116.23253923692094 40.137519286595484 38.04, 116.23246475369479 40.137598956882506 37.96, 116.23243569924517 40.13762948429887 37.95, 116.23237169983183 40.137697644882095 37.89, 116.2323312634272 40.13774028319174 37.86, 116.232270059853 40.13780504130548 37.82, 116.23222872502217 40.137848380588366 37.79, 116.23217211442622 40.13790793450379 37.75, 116.23216542498388 40.13791484086935 37.75, 116.23212568783079 40.13795657878363 37.7, 116.23205759560713 40.13802744381526 37.65, 116.23202235145583 40.13806397752392 37.57, 116.23195855263664 40.138129638606195 37.48, 116.23191731810505 40.13817187814024 37.44, 116.2318524213339 40.138238640506906 37.36, 116.23181188585072 40.1382798794906 37.33, 116.23174639031821 40.138347042649954 37.28, 116.23170615452611 40.138387781472844 37.27, 116.23163926151527 40.13845614620188 37.25, 116.23160042366824 40.13849538380946 37.23, 116.23153792390859 40.13855914449832 37.2, 116.23149659026978 40.13860108468573 37.2, 116.23142849971647 40.13866985098414 37.15, 116.23138946254697 40.138709189057 37.12, 116.23132356872917 40.1387753534182 37.09, 116.23128243514144 40.138816893693594 37.07, 116.23127844157703 40.13882079759429 37.07, 116.23121843853458 40.13888085640481 37.07, 116.23117670608593 40.13892279739697 37.05, 116.23110871624885 40.138990664262806 37.03, 116.23106908052489 40.139029903284225 37.01, 116.23100288805404 40.139095668593235 37, 116.2309621543321 40.139136208864294 36.97, 116.2309160294518 40.1391819545178 36.95, 116.2308955628174 40.139202274800624 36.94, 116.23085572777286 40.139241514288315 36.92, 116.23078893688945 40.13930768065456 36.91, 116.2307468058099 40.13934932258645 36.89, 116.23069329345823 40.139402175929014 36.86, 116.23068171244036 40.13941358748345 36.86, 116.2306408794297 40.13945392826941 36.84, 116.23057299095743 40.13952089619483 36.83, 116.23053135948638 40.13956223795326 36.83, 116.23046327166271 40.139629306321716 36.81, 116.23042223937979 40.13966984761106 36.8, 116.23035914365832 40.13973221121553 36.77, 116.23031681370452 40.13977395395284 36.74, 116.23024992452875 40.13983972159017 36.7, 116.23020909234098 40.139880363001986 36.68, 116.23017205387563 40.13991690058266 36.67, 116.23014250303684 40.139946230616935 36.65, 116.23010017345273 40.13998767363418 36.62, 116.23001860950203 40.14006865680467 36.6, 116.22958923042418 40.140493798217776 36.4, 116.229563673556 40.14051882465874 36.39, 116.22949818434331 40.14058399261601 36.38, 116.22945455819139 40.1406269379199 36.36, 116.22941023336017 40.14067098405548 36.33, 116.22936900331707 40.14071162699646 36.32, 116.22930481239321 40.14077499397327 36.29, 116.22926198531417 40.140817638772056 36.28, 116.22919679640933 40.14088210703598 36.26, 116.22915606593772 40.140922549775894 36.23, 116.22909257440547 40.140985116463035 36.22, 116.22906162738138 40.141015949049766 36.23)"
                            renderEntity.code =
                                "{\"kind\":7,\"linkOrPa\":1,\"linkPid\":\"148077295492871682\",\"mesh\":\"20596659\"}"
                            Log.e("qj", "test==${++dataIndex}")
                            realm.insert(renderEntity)
                        }
                        realm.commitTransaction()*//*
                        realm.close()
                    }
                    Log.e("qj", "test===结束")
                }*/

                loginStatus.postValue(LoginStatus.LOGIN_STATUS_SUCCESS)
            }

            is NetResult.Error<*> -> {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "${result.exception.message}", Toast.LENGTH_SHORT)
                        .show()
                }
                loginStatus.postValue(LoginStatus.LOGIN_STATUS_SUCCESS)
            }

            is NetResult.Failure<*> -> {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "${result.code}:${result.msg}", Toast.LENGTH_SHORT)
                        .show()
                }
                loginStatus.postValue(LoginStatus.LOGIN_STATUS_SUCCESS)
            }

            is NetResult.Loading -> {}
        }
    }

    /**
     * 初始化文件夹
     */
    private fun folderInit(
        context: Context,
        userName: String,
        password: String,
        userCode: String,
        userRealName: String
    ) {
        //文件夹初始化
        try {
            loginStatus.postValue(LoginStatus.LOGIN_STATUS_FOLDER_INIT)
            sharedPreferences?.edit()?.putString("userName", userName)?.commit()
            sharedPreferences?.edit()?.putString("passWord", password)?.commit()
            sharedPreferences?.edit()?.putString("userCode", userCode)?.commit()
            sharedPreferences?.edit()?.putString("userRealName", userRealName)?.commit()

            createUserFolder(context, userCode, userRealName)
        } catch (e: IOException) {
            loginStatus.postValue(LoginStatus.LOGIN_STATUS_FOLDER_FAILURE)
        }
    }

    /**
     * 创建用户目录
     */
    private fun createUserFolder(context: Context, userId: String, userRealName: String) {
        Constant.IS_VIDEO_SPEED = false
        Constant.USER_ID = userId
        Constant.USER_REAL_NAME = userRealName
        Constant.VERSION_ID = userId
        Constant.USER_DATA_PATH = Constant.DATA_PATH + Constant.USER_ID + "/" + Constant.VERSION_ID
        Constant.USER_DATA_ATTACHEMNT_PATH = Constant.USER_DATA_PATH + "/attachment/"
        // 在SD卡创建用户目录，解压资源等
        val userFolder = File(Constant.USER_DATA_PATH)
        if (!userFolder.exists()) userFolder.mkdirs()
        //创建附件目录
        val userAttachmentFolder = File(Constant.USER_DATA_ATTACHEMNT_PATH)
        if (!userAttachmentFolder.exists()) userAttachmentFolder.mkdirs()
        // 初始化Realm
        Realm.init(context.applicationContext)
        // 656e6372797000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000
        val config = RealmConfiguration.Builder()
            .directory(userFolder)
            .name("OMQS.realm")
            .encryptionKey(Constant.PASSWORD)
            .allowQueriesOnUiThread(true)
            .schemaVersion(2)
            .build()
        Realm.setDefaultConfiguration(config)
        // 拷贝配置文件到用户目录下
        val omdbConfigFile = File(userFolder.absolutePath, Constant.OMDB_CONFIG);
        ResourceUtils.copyFileFromAssets(Constant.OMDB_CONFIG, omdbConfigFile.absolutePath)
    }

    /**
     * 取消登录
     */
    fun cancelLogin() {
        jobLogin?.let {
            it.cancel()
            loginStatus.value = LoginStatus.LOGIN_STATUS_CANCEL
        }
    }

    override fun onCleared() {
        super.onCleared()
        cancelLogin()
    }

    private fun byteArrayToHexString(byteArray: ByteArray): String {
        return byteArray.joinToString("") { "%02x".format(it) }
    }
}