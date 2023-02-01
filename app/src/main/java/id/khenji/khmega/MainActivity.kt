package id.khenji.khmega

import android.Manifest.permission.*
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.storage.StorageManager
import android.provider.DocumentsContract
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.documentfile.provider.DocumentFile
import id.khenji.khmega.databinding.MaindBinding
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


class MainActivity : AppCompatActivity() {

    //private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: MaindBinding
    private lateinit var sharedpref: SharedPreferences
    private lateinit var userCustomUri: Uri

    private var helper: Helper = Helper()
    private val asuw: Int.(Int) -> Int = fun Int.(thai: Int) = this + thai
//    private val asuws: Int.(Int) -> Int = (2,9)

    companion object {
        val REQUEST_CODE = 899
        lateinit var pkgApp: String
        var fabVisible = false
        var versionStr: String? = null
        var versionApp: String? = null
        var isFirstRun: Boolean = true
        val pkgGlobal = "com.tencent.ig"
        val pkgKorea = "com.pubg.krmobile"
        var stringPath: String = "/files/UE4Game/ShadowTrackerExtra/ShadowTrackerExtra/Saved/"
        var configPath: String = "/files/UE4Game/ShadowTrackerExtra/ShadowTrackerExtra/Saved/Config/Android"
        var savPath: String = "/files/UE4Game/ShadowTrackerExtra/ShadowTrackerExtra/Saved/SaveGames"
        var dataPath: String = "/Android/data/"
        var obbPath: String = "/Android/obb/"
        val reqObb: Int = 9900
        val reqData: Int = 9901
        val reqActive: Int = 9902
        val reqUserCustom: Int = 9903
        val reqDetailsApp = 9904
        var askPerm = 1
        var reqNow = 0
        fun debug(msg: String, tag: String = "KHMEGA"){
            Log.d(tag, msg)
        }
        fun toast(ctx: Context, msg: String, duration: Int = Toast.LENGTH_SHORT){
            Toast.makeText(ctx, msg, duration).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = MaindBinding.inflate(layoutInflater)
        setContentView(binding.root)
        sharedpref =getSharedPreferences("khenji", MODE_PRIVATE)
        isFirstRun = sharedpref.getBoolean("firstrun", true)
        debug(asuw(3,2).toString())
        binding.pubgVersion.setOnCheckedChangeListener { radioGroup, i ->
            var pathConfig = "${stringPath}Config/Android"
            when (i){
                R.id.korea_version -> {
                    versionStr = "Korea"
                    if(!appInstalledOrNot(pkgKorea)) {
                        radioGroup.clearCheck()
                        toast(this, "$versionStr tidak terinstall")
                    } else {
                        setupVersion("korea")
                    }
                }
                R.id.global_version -> {
                    versionStr = "Global"
                    if(!appInstalledOrNot(pkgGlobal)) {
                        radioGroup.clearCheck()
                        toast(this, "$versionStr tidak terinstall")
                    } else {
                        //openSettings(pkgGlobal)
                        setupVersion("global")
                    }
                }
            }
        }
        binding.fabShow.setOnClickListener { view ->
            versionStr?.let { readConfig(it) }
            if (!fabVisible) {
                fabVisible = true
                binding.fabSave.show()
                binding.fabRun.show()
                binding.fabLoad.show()
                binding.fabRun.visibility = View.VISIBLE
                binding.fabLoad.visibility = View.VISIBLE
                binding.fabSave.visibility = View.VISIBLE
                binding.fabShow.setImageDrawable(resources.getDrawable(android.R.drawable.ic_menu_close_clear_cancel))
            } else {
                fabVisible =  false
                binding.fabSave.hide()
                binding.fabRun.hide()
                binding.fabLoad.hide()
                binding.fabRun.visibility = View.GONE
                binding.fabLoad.visibility = View.GONE
                binding.fabSave.visibility = View.GONE
                binding.fabShow.setImageDrawable(resources.getDrawable(android.R.drawable.ic_input_add))
            }
            //Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
            //    .setAction("Action", null).show()
            //val uri3 = Uri.parse(sharedpref.getString("obbUri", null))
            //uri3?.renameTo(pkgApp)
        }
        binding.fabLoad.setOnClickListener {
            try {
                val backup = File("${filesDir}/$versionStr.ini")
                val backupFile = File("${filesDir}/backup$versionStr.ini")
                val writeBackup = FileOutputStream(backupFile)
                    debug(fileList().toString())
                //backupFile.createNewFile()
                if (backup.exists() and backup.isFile and backup.canRead()) {
                    val reader = backup.bufferedReader()
                    val stringBuilder = StringBuilder()
                    var line: String?
                    var matches = false
                    while (reader.readLine().also {
                            line = it
                        } != null) {
                        stringBuilder.append(line)
                        stringBuilder.append("\n")
                        if(!matches) {
                            if(line!!.contains("BackUp DeviceProfile")){
                                matches = true
                            }
                        } else {
                            if(line!!.contains("UserCustom DeviceProfile")) break
                            writeBackup.write(line?.toByteArray())
                            writeBackup.write("\n".toByteArray())
                        }
                    }
                    binding.editconfig.setText(backup.readText())
                } else {
                    debug("File not exist")
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            checkPerm(reqObb)
        }
    }
    private fun setupVersion(pkg: String){
        pkgApp = when(pkg){
            "global" -> pkgGlobal
            "korea" -> pkgKorea
            else -> pkgGlobal
        }
        versionApp = pkg
        configPath = "Android/data/$pkgApp${stringPath}Config/Android"
        savPath = "Android/data/$pkgApp${stringPath}SaveGames"
        dataPath = "$dataPath"
        obbPath = "$obbPath"
        sharedpref.edit {
            putString("version", versionApp)
            putString("pkg_version", pkgApp)
            apply()
        }
        if(sharedpref.getString(pkgApp,null) == null){
//            requestPerms(config.path)
            debug(configPath)
            requestPerms(savPath)
        }
    }
    private fun Uri.renameTo(pkg: String) {
        val uri = this
        try {
            val tree = DocumentFile.fromTreeUri(this@MainActivity, uri)
            var status = false
            tree?.findFile(pkg)?.let {
                if (it.isDirectory){
                    if(it.renameTo("${it.name}_khenji"))status=true
                }
            }
            if (status == false){
                tree?.findFile("${pkg}_khenji")?.let {
                    if (it.isDirectory){
                        if(it.renameTo("${pkg}"))status=true
                    }
                }
            }
            debug(status.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }



    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            1000 -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    debug("permission granted")
                } else {
                    debug("permission denied")
                }
            }
        }
    }
    private fun requestPerms(path: String){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if(isFirstRun)requestAllFilesPermission()
            //requestDocumentPermission("obb/$pkgApp")
            //requestDocumentPermission(path)
            checkPerm(reqActive)
        }
    }
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun checkPerm(reqCode: Int) {
        when (reqCode) {
            reqActive -> {
                sharedpref.getString("active${versionStr}Uri", null).let {
                    if (it == null) requestDocumentPermission("$savPath", reqCode)
                    if (it != null) {
                        if(!permissionGranted(it))requestDocumentPermission("$savPath", reqCode)
                    }
                }
            }
            reqUserCustom -> {
                sharedpref.getString("userCustom${versionStr}Uri", null).let {
                    if (it == null) requestDocumentPermission("$configPath", reqCode)
                    if (it != null) {
                        if(!permissionGranted(it))requestDocumentPermission("$configPath", reqCode)
                    }
                }
            }
            reqData -> {
                sharedpref.getString("dataUri", null).let {
                    if (it == null) requestDocumentPermission("$dataPath", reqCode)
                    if (it != null) {
                        if(!permissionGranted(it))requestDocumentPermission("$dataPath", reqCode)
                    }
                }
            }
            reqObb -> {
                sharedpref.getString("obbUri", null).let {
                    if (it == null) requestDocumentPermission("$obbPath", reqCode)
                    if (it != null) {
                        if(!permissionGranted(it))requestDocumentPermission("$obbPath", reqCode)
                    }
                }
            }
        }
    }
    private fun askPermission(pck: String?) {
        if (pck != null) {
            debug(pck)
        }
        val nuri = Uri.parse(pck)
        val i = Intent()
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        i.action = Intent.ACTION_OPEN_DOCUMENT_TREE
        i.putExtra(DocumentsContract.EXTRA_INITIAL_URI, nuri)
        @Suppress("DEPRECATION")
        startActivityForResult(i, 999)
    }
   // @RequiresApi(Build.VERSION_CODES.R)
    private fun requestAllFilesPermission() {
        val myintent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
        //val myintent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
        myintent.addCategory("android.intent.category.DEFAULT")
        myintent.data = Uri.fromParts("package",packageName,null)
        startActivity(myintent)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun requestDocumentPermission(folder: String, requestCode: Int){
        reqNow = requestCode
        sharedpref.edit { putInt("reqNow", requestCode).commit() }
        val storageManager = application.getSystemService(Context.STORAGE_SERVICE) as StorageManager
        val myintent = storageManager.primaryStorageVolume.createOpenDocumentTreeIntent()
        val targetDir = "${Uri.encode(folder)}"
        var uri = myintent.getParcelableExtra<Uri>("android.provider.extra.INITIAL_URI") as Uri
        var scheme = uri.toString()
        scheme = scheme.replace("/root/", "/tree/")
        scheme += "%3A$targetDir/document/primary%3A$targetDir"
        uri = Uri.parse(scheme)
        debug("set storage ${uri}")
        myintent.putExtra("requestCode", requestCode)
        myintent.putExtra("android.provider.extra.INITIAL_URI", uri)
        myintent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        //startActivityForResult(intent, REQUEST_CODE)
        startForResult.launch(myintent)
    }
    private fun permissionGranted(uri: String): Boolean {
        val listPerm = contentResolver.persistedUriPermissions
        for (i in listPerm.indices) {
            val persistedUriString = listPerm[i].uri.toString()
            if(persistedUriString == uri && listPerm[i].isWritePermission && listPerm[i].isReadPermission) {
                return true
            }
        }
        return false
    }
    @RequiresApi(Build.VERSION_CODES.Q)
    val startForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        result ->
        if (result.resultCode == RESULT_OK) {
            if (result.data != null) {
                val takeFlags = (Intent.FLAG_GRANT_READ_URI_PERMISSION
                        or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                result.data?.data?.let { treeUri ->
                    contentResolver.takePersistableUriPermission(
                        treeUri,
                        takeFlags
                    )
                    when(reqNow) {
                        reqObb -> {
                            sharedpref.edit(commit = true, action = {
                                this.putString("obbUri", treeUri.toString())
                            })
                            checkPerm(reqData)
                        }
                        reqData -> {
                            sharedpref.edit(commit = true, action = {
                                this.putString("dataUri", treeUri.toString())
                            })
                            checkPerm(reqObb)
                        }
                        reqUserCustom -> {
                            sharedpref.edit(commit = true, action = {
                                this.putString("userCustom${versionStr}Uri", treeUri.toString())
                            })
                            helper.copyToData(this, "UserCustom.ini", "${versionStr}.ini", treeUri)
                            checkPerm(reqActive)
                        }
                        reqActive -> {
                            sharedpref.edit(commit = true, action = {
                                this.putString("active${versionStr}Uri", treeUri.toString())
                            })
                            helper.copyToData(this, "Active.sav", "${versionStr}.sav", treeUri)
                            checkPerm(reqUserCustom)
                        }
                    }

                    debug(treeUri.toString())
                    //readSDK30(treeUri)
                }
            }
        } else if (result.resultCode == RESULT_CANCELED) {
            debug("cancel $reqNow")
            if (reqNow == reqDetailsApp || sharedpref.getInt("reqNow", 0) == reqDetailsApp) {
                toast(this, "its from settings android 10 or higher")
            }
        }
    }
    private fun appInstalledOrNot(uri: String): Boolean {
        val pm = packageManager
        val flags = 0
        try {
            val checkPkg = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(uri, PackageManager.PackageInfoFlags.of(flags.toLong()))
            } else {
                pm.getPackageInfo(uri, 0)
            }
            return true
        } catch (e: NameNotFoundException) {
            debug(e.toString())
        }
        return false
    }
    private fun openSettings(packageName: String) {
        val pm = this.packageManager
        val intt = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + packageName))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            reqNow = reqDetailsApp
            startForResult.launch(intt)
            debug("load 10 higer")
            sharedpref.edit { putInt("reqNow", reqDetailsApp) }
        } else {
            sharedpref.edit { putInt("reqNow", reqDetailsApp) }
            debug("load under 10")
            startActivityForResult(intt, reqDetailsApp)
        }
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == reqDetailsApp) {
            if (data != null) {
                debug("its from settings")
            }
        }
    }
    private fun readConfig(versionStr: String) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            sharedpref.getString("userCustom${versionStr}Uri", null).let {
                if(it != null) {
                    if (!permissionGranted(it)) {
                        requestDocumentPermission("$configPath", reqUserCustom)
                    } else {
                        debug(it)
                        binding.editconfig.setText(helper.readFromUri(this, "UserCustom.ini", Uri.parse(it)), TextView.BufferType.EDITABLE)
                    }
                }
            }
        }
    }
    private fun readSDK30(treeUri: Uri) {
        val tree = DocumentFile.fromTreeUri(this, treeUri)!!
        val uriList = arrayListOf<Uri>()
        listFiles(tree).forEach { uri ->
            uriList.add(uri)
            debug(" Uri log: $uri")
        }
        debug(" Uri log: $uriList")
        try {
            //val userCustom = tree.findFile("UserCustom.ini")
            var userCustomPath = File(Environment.getExternalStorageDirectory().path+"/Android/data/${pkgApp}${stringPath}Config/Android").path
            var activeSavPath = File(Environment.getExternalStorageDirectory().path+"/Android/data/${pkgApp}${stringPath}SaveGames").path
            var asfadfadf = File(Environment.getExternalStorageDirectory().path+"/Android/data/com.tencent.ig/files/iMSDK").path
            val testt = DocumentFile.fromTreeUri(this,helper.toTreeUri(asfadfadf))
            testt?.renameTo("anjay")
            debug(" active ${helper.toTreeUri(activeSavPath)}")
            debug(" ucustom ${helper.toTreeUri(userCustomPath)}")
            debug(" toTreeUri  ${helper.toTreeUri(userCustomPath)}")
            val treeActive = DocumentFile.fromTreeUri(this,helper.toTreeUri(activeSavPath))
            val treeUserCustom = DocumentFile.fromTreeUri(this,helper.toTreeUri(userCustomPath))
            debug(" treeUserCustom  ${treeUserCustom?.toString()}")
            val checkActive = treeActive?.findFile("Active.sav")?.let {
                if(it.exists() and (it.isFile) and it.canRead()){
                    debug("Active exists")
                    it.uri
                } else {
                    debug("Active not exist")
                    it.uri
                }
            }
            val checkUserCustom = treeUserCustom?.findFile("UserCustom.ini")?.let {
                if(it.exists() and (it.isFile) and it.canRead()){
                    debug("UserCustom exists")
                    it.uri
                } else {
                    debug("UserCustom not exist")
                    it.uri
                }
            }
            debug(" check active $checkActive")
            debug(" check Usercustom $checkUserCustom")
            debug(" treeActive $treeActive")
//            if(userCustom!!.isFile and userCustom!!.canRead()){
//                //val testcreate = tree.createFile("text/","test.txt")
//                //val asuu = DocumentsContract.copyDocument()
//                //debug("${testcreate?.name} ${testcreate?.uri}")
//
//                val in_s = resources.openRawResource(R.raw.active)
//                val b = ByteArray(in_s.available())
//                in_s.read(b)
//                helper.writeContent(this, userCustom.uri, b)
//                helper.writeContent(this, activeSav.toUri(), b)
//                debug("start to copy")
//                val status = helper.copyToData(this,"UserCustom.ini", userCustom.uri)
//                debug(status.toString())
//            } else {
//                toast(this,"UserCustom tidak ditemukan")
//                debug("UserCustom not exist")
//            }
        } catch (e: Exception) {
            debug("error ${e.toString()}")
        }
    }
    private fun listFiles(folder: DocumentFile): List<Uri> {
        return if (folder.isDirectory) {
            folder.listFiles().mapNotNull { file ->
                if (file.name != null) file.uri else null
            }
        } else emptyList()
    }

}

