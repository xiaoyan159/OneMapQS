package com.navinfo.omqs.ui

import android.content.Intent
import android.os.Bundle
import androidx.core.view.WindowCompat
import androidx.navigation.ui.AppBarConfiguration
import com.github.k1rakishou.fsaf.FileChooser
import com.github.k1rakishou.fsaf.callback.FSAFActivityCallbacks
import com.navinfo.omqs.databinding.ActivityMainBinding
import com.navinfo.omqs.ui.activity.PermissionsActivity

class MainActivity : PermissionsActivity(), FSAFActivityCallbacks {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private val fileChooser by lazy { FileChooser(this@MainActivity) }

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

//        val navController = findNavController(R.id.nav_host_fragment_content_main)
//        appBarConfiguration = AppBarConfiguration(navController.graph)
//        setupActionBarWithNavController(navController, appBarConfiguration)

        fileChooser.setCallbacks(this@MainActivity)
//        binding.fab.setOnClickListener { view ->
//            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                .setAnchorView(R.id.fab)
//                .setAction("Action", null).show()
//            // 开始数据导入功能
//            fileChooser.openChooseFileDialog(object: FileChooserCallback() {
//                override fun onCancel(reason: String) {
//                }
//
//                override fun onResult(uri: Uri) {
//                    val file = UriUtils.uri2File(uri)
//                    Snackbar.make(view, "文件大小为：${file.length()}", Snackbar.LENGTH_LONG)
//                        .show()
//                }
//            })
//        }
    }

    override fun onPermissionsGranted() {
    }

    override fun onPermissionsDenied() {
    }

//    override fun onCreateOptionsMenu(menu: Menu): Boolean {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        menuInflater.inflate(R.menu.menu_main, menu)
//        return true
//    }

//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        // Handle action bar item clicks here. The action bar will
//        // automatically handle clicks on the Home/Up button, so long
//        // as you specify a parent activity in AndroidManifest.xml.
//        return when (item.itemId) {
//            R.id.action_settings -> true
//            else -> super.onOptionsItemSelected(item)
//        }
//    }
//
//    override fun onSupportNavigateUp(): Boolean {
//        val navController = findNavController(R.id.nav_host_fragment_content_main)
//        return navController.navigateUp(appBarConfiguration)
//                || super.onSupportNavigateUp()
//    }

    override fun fsafStartActivityForResult(intent: Intent, requestCode: Int) {
        startActivityForResult(intent, requestCode)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        fileChooser.onActivityResult(requestCode, resultCode, data)
    }
}