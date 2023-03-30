package com.navinfo.omqs.http

/**
 * Created by Mayokun Adeniyi on 23/05/2020.
 */

/**
 * 网络返回封装
 */

/**
 * 在类名之前使用sealed关键字将类声明为密封类。
 * 密封类仅在编译时限制类型集来确保类型安全的重要性。
 * 密封类隐式是一个无法实例化的抽象类。
 */
/**
 * 二:密封类所具有的特性和与别的类具有不同之处

①Sealed class（密封类） 是一个有特定数量子类的类，看上去和枚举有点类似，所不同的是，在枚举中，我们每个类型只有一个对象（实例）；而在密封类中，同一个类可以拥有几个对象。

②Sealed class（密封类）的所有子类都必须与密封类在同一文件中

③Sealed class（密封类）的子类的子类可以定义在任何地方，并不需要和密封类定义在同一个文件中

④Sealed class（密封类）没有构造函数，不可以直接实例化，只能实例化内部的子类
 */
/**
 * 如何获取密封类里面的函数方法
 * 只能创建密封类子类对象 通过密封类的子类对象调用密封类里的函数方法
 */
sealed class NetResult<out R> {

    data class Success<out T>(val data: T?) : NetResult<T>()
    data class Failure(val code: Int, val msg: String) : NetResult<Nothing>()
    data class Error(val exception: Exception) : NetResult<Nothing>()
    object Loading : NetResult<Nothing>()

    /**
     * 密封类通常与表达when时一起使用。 由于密封类的子类将自身类型作为一种情况。
    因此，密封类中的when表达式涵盖所有情况，从而避免使用else子句。
     */
    override fun toString(): String {
        return when (this) {
            is Success<*> -> "网络访问成功，返回正确结果Success[data=$data]"
            is Failure -> "网络访问成功，返回错误结果Failure[$msg]"
            is Error -> "网络访问出错 Error[exception=$exception]"
            is Loading -> "网络访问中 Loading"
        }
    }
}
/**
 * 密封类里面可以有若干个子类，这些子类如果要继承密封类,则必须和密封类在同一个文件里
 */