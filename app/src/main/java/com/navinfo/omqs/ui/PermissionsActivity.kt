package com.navinfo.omqs.ui

import android.os.Bundle
import android.os.PersistableBundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions

/**
 * 权限申请Activity
 */
abstract class PermissionsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        XXPermissions.with(this)
            // 申请单个权限
//            .permission(Permission.WRITE_EXTERNAL_STORAGE)
//            .permission(Permission.READ_EXTERNAL_STORAGE)
//            .permission(Permission.READ_MEDIA_IMAGES)
//            .permission(Permission.READ_MEDIA_AUDIO)
//            .permission(Permission.READ_MEDIA_VIDEO)
            .permission(Permission.MANAGE_EXTERNAL_STORAGE)
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
                    }
                }
            })
    }

    abstract fun onPermissionsGranted()
    abstract fun onPermissionsDenied()
}