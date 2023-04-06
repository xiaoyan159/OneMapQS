package com.navinfo.omqs.ui.activity

import android.os.Build
import android.os.Bundle
import android.widget.Toast
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions

/**
 * 权限申请Activity
 */
open class PermissionsActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val permissionList = mutableListOf<String>()
        if (applicationInfo.targetSdkVersion >= Build.VERSION_CODES.TIRAMISU) {
            //文件读写
            permissionList.add(Permission.READ_MEDIA_IMAGES)
            permissionList.add(Permission.READ_MEDIA_AUDIO)
            permissionList.add(Permission.READ_MEDIA_VIDEO)
        } else {
            //文件读写
            permissionList.add(Permission.WRITE_EXTERNAL_STORAGE)
            permissionList.add(Permission.READ_EXTERNAL_STORAGE)
            permissionList.add(Permission.READ_MEDIA_VIDEO)
        }
        //定位权限
        permissionList.add(Permission.ACCESS_FINE_LOCATION)
        permissionList.add(Permission.ACCESS_COARSE_LOCATION)
        //android10
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            permissionList.add(Permission.ACCESS_BACKGROUND_LOCATION)
        }
        XXPermissions.with(this)
            // 申请单个权限
            .permission(permissionList)
            // 设置权限请求拦截器（局部设置）
            //.interceptor(new PermissionInterceptor())
            // 设置不触发错误检测机制（局部设置）
            //.unchecked()
            .request(object : OnPermissionCallback {

                override fun onGranted(permissions: MutableList<String>, all: Boolean) {
                    if (!all) {
                        Toast.makeText(
                            this@PermissionsActivity,
                            "获取部分权限成功，但部分权限未正常授予",
                            Toast.LENGTH_SHORT
                        )
                            .show()
                        onPermissionsGranted()
                        return
                    } else {
                        onPermissionsDenied()
                    }
                    // 在SD卡创建项目目录
                }

                override fun onDenied(permissions: MutableList<String>, never: Boolean) {
                    if (never) {
                        Toast.makeText(
                            this@PermissionsActivity,
                            "永久拒绝授权,请手动授权文件读写权限",
                            Toast.LENGTH_SHORT
                        )
                            .show()
                        // 如果是被永久拒绝就跳转到应用权限系统设置页面
                        XXPermissions.startPermissionActivity(this@PermissionsActivity, permissions)
                        onPermissionsDenied()
                    } else {
                        onPermissionsDenied()
                    }
                }
            })
    }

    /**
     * 权限全部同意
     */
    open fun onPermissionsGranted() {

    }

    /**
     * 权限
     */
    open fun onPermissionsDenied() {

    }
}