package com.navinfo.omqs.http

import com.navinfo.omqs.bean.EvaluationInfo
import com.navinfo.omqs.bean.OfflineMapCityBean
import com.navinfo.collect.library.data.entity.TaskBean
import com.navinfo.omqs.bean.IndoorConnectionInfoBean
import com.navinfo.omqs.bean.LoginUserBean
import com.navinfo.omqs.bean.QRCodeBean
import com.navinfo.omqs.bean.SysUserBean
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

/**
 * retrofit2 网络请求接口
 */
interface RetrofitNetworkServiceAPI {

    /**
     * 在 Retrofit2 中，可以使用不同类型的返回值来获取 API 调用的结果。以下是 Retrofit2 支持的一些常见返回类型：

    1. `Call<T>`：表示一个异步请求，其中 `T` 是 API 响应的类型，通常是一个自定义的数据类。使用 `enqueue` 方法来执行异步请求，并在回调中处理响应。

    2. `Response<T>`：表示一个同步请求的响应，其中 `T` 是 API 响应的类型。使用 `execute` 方法来执行同步请求，并返回一个 `Response` 对象，其中包含了响应的状态码、响应头和响应体等信息。

    3. `Observable<T>`：表示一个 RxJava 的 Observable 对象，其中 `T` 是 API 响应的类型。使用 `subscribe` 方法来执行请求，并在 onNext 回调中处理响应。

    4. `Single<T>`：表示一个 RxJava 的 Single 对象，其中 `T` 是 API 响应的类型。使用 `subscribe` 方法来执行请求，并在 onSuccess 回调中处理响应。

    5. `Completable`：表示一个 RxJava 的 Completable 对象，用于执行没有响应体的 API 调用。使用 `subscribe` 方法来执行请求，并在 onComplete 回调中处理响应。

    6. `Flowable<T>`：表示一个 RxJava 的 Flowable 对象，其中 `T` 是 API 响应的类型。类似于 Observable，但支持背压处理。

    7. `Deferred<T>`：表示一个 Kotlin 的 Deferred 对象，其中 `T` 是 API 响应的类型。使用 `await` 方法来执行请求，并返回响应的结果。

    8. `LiveData<Response<T>>`：表示一个 LiveData 对象，其中 `T` 是 API 响应的类型。使用 `observe` 方法来执行请求，并在回调中处理响应。

    总之，Retrofit2 支持多种返回类型，开发者可以根据项目需求选择合适的方式来处理 API 响应结果。
     */

    /**
     * 获取离线地图城市列表
     */
    @GET("/drdc/MapDownload/maplist")
    suspend fun retrofitGetOfflineMapCityList(): Response<List<OfflineMapCityBean>>

    /**
     * 登录接口
     */
    @Headers("Content-Type: application/json")
    @POST("/devcp/loginUser")
    suspend fun retrofitLoginUser(@Body loginUserBean: LoginUserBean): Response<DefaultResponse<SysUserBean>>

    /**
     * 下载文件
     */
    @Streaming
    @GET
    suspend fun retrofitDownLoadFile(
        @Header("RANGE") start: String? = "0",
        @Url url: String
    ): Response<ResponseBody>

    @GET("/devcp/getEvaluationTask?evaluatType=2")
    suspend fun retrofitGetTaskList(
        @Query("evaluatorNo") evaluatorNo: String,
    ): Response<DefaultResponse<List<TaskBean>>>



    /**
     * 连接室内整理工具
     */
    @Streaming
    @GET
    suspend fun retrofitConnectIndoorTools(@Url url: String): Response<QRCodeBean>

    /**
     * 登录接口
     */
    @Headers("Content-Type: application/json")
    @POST
    suspend fun retrofitUpdateServerInfo(@Url url: String,@Body indoorConnectionInfoBean: IndoorConnectionInfoBean): Response<QRCodeBean>

    @Headers("Content-Type: application/json")
    @POST("/devcp/uploadSceneProblem")
    suspend fun postRequest(@Body listEvaluationInfo: List<EvaluationInfo>?): Response<DefaultResponse<*>>

    /**
     * @FormUrlEncoded 请求格式注解，请求实体是一个From表单，每个键值对需要使用@Field注解
    @Field 请求参数注解，提交请求的表单字段，必须要添加，而且需要配合@FormUrlEncoded使用
    “token” 参数字段，与后台给的字段需要一致
    String 声明的参数类型
    token 实际参数，表示后面token的取值作为"token"的值
    Post请求如果有参数需要在头部添加@FormUrlEncoded注解，表示请求实体是一个From表单，每个键值对需要使用@Field注解，使用@Field添加参数，这是发送Post请求时，提交请求的表单字段，必须要添加的，而且需要配合@FormUrlEncoded使用，若为在头部添加@FormUrlEncoded注解，会抛出如下异常：
    当有多个不确定参数时，我们可以使用@FieldMap注解，@FieldMap与@Field的作用一致，可以用于添加多个不确定的参数，类似@QueryMap，Map的key作为表单的键，Map的value作为表单的值。
     */
//    @FormUrlEncoded
//    @POST("api/dog")
//    fun postCall(@Field("token") token: String?): Call<Any?>?

    /**
     * 当有多个不确定参数时，我们可以使用@FieldMap注解，@FieldMap与@Field的作用一致，可以用于添加多个不确定的参数，类似@QueryMap，Map的key作为表单的键，Map的value作为表单的值。
     */
//    @FormUrlEncoded
//    @POST("api/dog")
//    open fun postCall(@FieldMap map: Map<String?, Any?>?): Call<Any?>?
    /**
     * @GET("v7/weather/now")
    Call<Wth_now_out> getCall(@QueryMap Map<String, Object> map);
     */
//    @GET("v7/weather/now")
//    open fun getCall(@QueryMap map: Map<String?, Any?>?): Call<Wth_now_out?>?
    /**
     *@Query 请求参数注解，用于Get请求中的参数
    “location”/“key” 参数字段，与后台给的字段需要一致
     */
//    @GET("v7/weather/now")
//    open fun getCall(@QueryMap map: Map<String?, Any?>?): Call<Wth_now_out?>?

    /**
     * @FieldMap 请求参数注解，与@Field作用一致，用于不确定表单参数
    Map<String, Object> map 通过Map将不确定的参数传入，相当于多个Field参数
    适用于Post请求的还有一个注解@Body，@Body可以传递自定义类型数据给服务器，多用于post请求发送非表单数据，比如用传递Json格式数据，它可以注解很多东西，比如HashMap、实体类等，我们来看看它用法：
    特别注意：@Body注解不能用于表单或者支持文件上传的表单的编码，即不能与@FormUrlEncoded和@Multipart注解同时使用
     */
//    @POST("")
//    open fun getPsotDataBody(@Body body: RequestBody?): Call<Any?>?

    /**
     * @Path
    使用第一个get请求网址
     */
//    @GET("v7/weather/{time}")
//    open fun getCall(@Path("time") time: String?,@QueryMap map: Map<String?, Any?>?): Call<Wth_now_out?>?

    /**
     *  @QueryMap get请求方法参数的注解，上面已经解释了，这里就不重复讲
    @Path 请求参数注解，用于Url中的占位符{}，在网址中的参数
    @Path注解用于Url中的占位符{}，在网址中的参数，如上面 @GET(“v7/weather/{time}”)的time，通过{}占位符来标记time，使用@Path注解传入time的值，注意有的Url既有占位符又有"?"后面的键值对，其实@Query和@Path两者是可以共用的。在发起请求时，{time}会被替换为方法中第二个参数的值time。
     */
//    Map<String ,Object> map = new HashMap<>()Map<String ,Object> map = new HashMap<>();
//    map.put("location","101010100");
//    map.put("key","a5cf6ab782a14013b08fb92a57dd2f72");
//    Call<Wth_now_out> call = apiService.getCall("now",map);
    /**
     * 最终{time}会被替换为now,此时形成的牵绊部分Url为：https://devapi.qweather.com/v7/weather/now，剩余**？号**后面的Url由@Query传入。
     */

    /**
     * @HTTP
    @HTTP注解的作用是替换@GET、@POST、@PUT、@DELETE、@HEAD以及更多拓展功能
    如可通过此注解代替@POST注解实现post请求，如下：
    method 表示请求的方法，区分大小写，这里的值retrofit不会再做任何处理，必须要保证正确
    path 网络请求地址路径
    hasBody 是否有请求体，boolean类型
    除请求接口部分的方法有变，别的部分与正常使用@POST请求一致，另外需要注意在使用@HTTP注解实现post请求时，若含有请求体，那么对应的标识注解@FormUrlEncoded一定要加上，若为别的请求体，那么就添加与之对应的标识注解。
    @HTTP注解可以通过method字段灵活设置具体请求方法，通过path设置网络请求地址，用的比较少。使用方法与设置的请求方法种类一致。
     */

//    @FormUrlEncoded
//    @POST("api/dog")
//    Call<Object> postCall(@FieldMap Map<String,Object> map);
//
//    @FormUrlEncoded
//    @HTTP(method = "POST",path = "api/dog", hasBody = true)
//    Call<Object> postCall1(@FieldMap Map<String,Object> map);
    /**
     * @Url
    如果需要重新地址接口地址，可以使用@Url，将地址以参数的形式传入即可。如果有@Url注解时，GET传入的Url必须省略。不然会抛出如下异常。
     */
    //错误的
//    @FormUrlEncoded
//    @POST("api/dog")
//    open fun postCall(@Url url: String?, @FieldMap map: Map<String?, Any?>?): Call<Any?>?
    //正确的
//    @FormUrlEncoded
//    @POST
//    open fun postCall(@Url url: String?, @FieldMap map: Map<String?, Any?>?): Call<Any?>?

    /**
     * @Streaming
     * @Streaming 表示响应体的数据用流的方式返回，使用于返回数据比较大，该注解在下载大文件时特别有用
     */
//    @Streaming
//    @POST("gists/public")
//    open fun getStreamingBig(): Call<ResponseBody?>?

    /**
     * @Multipart、@part、@PartMap
     * @Multipart 表示请求实体是一个支持文件上传的表单，需要配合@Part和@PartMap使用，适用于文件上传
    @Part 用于表单字段，适用于文件上传的情况，@Part支持三种类型：RequestBody、MultipartBody.Part、 任意类型
    @PartMap 用于多文件上传， 与@FieldMap和@QueryMap的使用类似
     */
//    @Multipart
//    @POST("user/followers")
//    open fun getPartData(@Part("name") name: RequestBody?, @Part file: Part?): Call<ResponseBody?>?

    /**
     *   代码使用逻辑：
     *   首先声明类型，通过MediaType实现类型的声明，此处使用的时文本类型，然后会根据该类型转化为RequestBody对象，此处使用的参数委RequestBody对象，相当于讲周润发以文本格式转换委RequestBody对象，最终上传时与name形成键值对。
     */
//    //声明类型,这里是文字类型
//    MediaType textType = MediaType.parse("text/plain");
//    //根据声明的类型创建RequestBody,就是转化为RequestBody对象
//    RequestBody name = RequestBody.create(textType, "周润发");

////创建文件，这里演示图片上传
//    File file = new File("文件路径");
//    if (!file.exists()) {
//        file.mkdir();
//    }
////将文件转化为RequestBody对象
////需要在表单中进行文件上传时，就需要使用该格式：multipart/form-data
//    RequestBody imgBody = RequestBody.create(MediaType.parse("image/png"), file);
////将文件转化为MultipartBody.Part
////第一个参数：上传文件的key；第二个参数：文件名；第三个参数：RequestBody对象
//    MultipartBody.Part filePart = MultipartBody.Part.createFormData("key", file.getName(), imgBody);
    /**
     * 对于文件类型的上传，我们一般使用MultipartBody.Part类型上传，在上面讲两个参数设置好后，然后调用接口使用请求方法即可：
     * 此种格式属于文本与文件混合发送的格式。
     */
//    var partDataCall: Call<ResponseBody?>? = retrofit.create(Api::class.java).getPartData(name, filePart)

    /**
     * @PartMap的使用与@FieldMap和@QueryMap的使用类似，用于多文件上传，我们直接看代码：
     */
//    @Multipart
//    @POST("user/followers")
//    open fun getPartMapData(@PartMap map: Map<String?, Part?>?): Call<ResponseBody?>?

//    File file1 = new File("文件路径");
//    File file2 = new File("文件路径");
//    if (!file1.exists()) {
//        file1.mkdir();
//    }
//    if (!file2.exists()) {
//        file2.mkdir();
//    }
//    RequestBody requestBody1 = RequestBody.create(MediaType.parse("image/png"), file1);
//    RequestBody requestBody2 = RequestBody.create(MediaType.parse("image/png"), file2);
//    MultipartBody.Part filePart1 = MultipartBody.Part.createFormData("file1", file1.getName(), requestBody1);
//    MultipartBody.Part filePart2 = MultipartBody.Part.createFormData("file2", file2.getName(), requestBody2);
//
//    Map<String,MultipartBody.Part> mapPart = new HashMap<>();
//    mapPart.put("file1",filePart1);
//    mapPart.put("file2",filePart2);

    /**
     * 有关MediaType方法
     * 概述：MediaType，即是Internet Media Type，互联网媒体类型，也叫做MIME类型，在Http协议消息头中，使用Content-Type来表示具体请求中的媒体类型信息。（也就是说MediaType在网络协议的消息头里面叫做Content-Type）它使用两部分的标识符来确定一个类型，是为了表明我们传的东西是什么类型。
    常见的媒体格式类型如下：
    text/html ： HTML格式
    text/plain ：纯文本格式
    text/xml ： XML格式
    image/gif ：gif图片格式
    image/jpeg ：jpg图片格式
    image/png：png图片格式
    以application开头的媒体格式类型：
    application/xhtml+xml ：XHTML格式
    application/xml ：XML数据格式
    application/atom+xml ：Atom XML聚合格式
    application/json ：JSON数据格式
    application/pdf ：pdf格式
    application/msword ： Word文档格式
    application/octet-stream ： 二进制流数据（如常见的文件下载、上传）
    application/x-www-form-urlencoded ： 中默认的encType，form表单数据被编码为key/value格式发送到服务器（表单默认的提交数据的格式
    另外一种常见的媒体格式是上传文件之时使用的：
    multipart/form-data ： 需要在表单中进行文件上传时，就需要使用该格式
     */


}