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
import androidx.core.widget.addTextChangedListener
import androidx.documentfile.provider.DocumentFile
import id.khenji.khmega.databinding.MaindBinding
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


class MainActivity : AppCompatActivity() {

    private lateinit var binding: MaindBinding
    private lateinit var sharedpref: SharedPreferences

    private var helper: Helper = Helper()
    private val asuw: Int.(Int) -> Int = fun Int.(thai: Int) = this + thai

    companion object {
        lateinit var pkgApp: String
        val isSAF = Build.VERSION.SDK_INT > Build.VERSION_CODES.Q
        var fabVisible = false
        var versionStr: String? = null
        var versionApp: String? = null
        var isFirstRun: Boolean = true
        const val pkgGlobal = "com.tencent.ig"
        const val pkgKorea = "com.pubg.krmobile"
        var stringPath: String = "/files/UE4Game/ShadowTrackerExtra/ShadowTrackerExtra/Saved/"
        var configPath: String = "/files/UE4Game/ShadowTrackerExtra/ShadowTrackerExtra/Saved/Config/Android"
        var savPath: String = "/files/UE4Game/ShadowTrackerExtra/ShadowTrackerExtra/Saved/SaveGames"
        var dataPath: String = "/Android/data/"
        var obbPath: String = "/Android/obb/"
        const val reqObb: Int = 9900
        const val reqData: Int = 9901
        const val reqActive: Int = 9902
        const val reqUserCustom: Int = 9903
        const val reqDetailsApp = 9904
        const val reqPicker = 9905
        var reqNow = 0
        var isReady = false
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
        binding.pubgVersion.setOnCheckedChangeListener { radioGroup, i ->
            when (i){
                R.id.korea_version -> {
                    versionStr = "Korea"
                    if(!appInstalledOrNot(pkgKorea)) {
                        clearVersion(sav = false, config = false)
                        toast(this, "Korea tidak terinstall")
                    } else {
                        setupVersion("korea")
                    }
                }
                R.id.global_version -> {
                    versionStr = "Global"
                    if(!appInstalledOrNot(pkgGlobal)) {
                        clearVersion(sav = false, config = false)
                        toast(this, "Global tidak terinstall")
                    } else {
                        setupVersion("global")
                    }
                }
            }
        }
        binding.fabShow.setOnClickListener { _ ->
            if (!fabVisible) {
                fabVisible = true
                binding.fabSave.show()
                binding.fabSav.show()
                binding.fabRun.show()
                binding.fabLoad.show()
                binding.fabRun.visibility = View.VISIBLE
                binding.fabLoad.visibility = View.VISIBLE
                binding.fabSave.visibility = View.VISIBLE
                binding.fabSav.visibility = View.VISIBLE
                binding.fabShow.setImageDrawable(resources.getDrawable(android.R.drawable.ic_menu_close_clear_cancel))
            } else {
                fabVisible =  false
                binding.fabSave.hide()
                binding.fabSav.hide()
                binding.fabRun.hide()
                binding.fabLoad.hide()
                binding.fabRun.visibility = View.GONE
                binding.fabLoad.visibility = View.GONE
                binding.fabSave.visibility = View.GONE
                binding.fabSav.visibility = View.GONE
                binding.fabShow.setImageDrawable(resources.getDrawable(android.R.drawable.ic_input_add))
            }
        }
        binding.fabSav.setOnLongClickListener { view ->
            toast(this, "Sav deleted")
            File("${filesDir}/Active.sav").delete()
        }
        binding.fabSav.setOnClickListener {
            if(versionIsSelected()) {
                val inte = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "*/*"
                    putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                    putExtra(Intent.EXTRA_LOCAL_ONLY, true)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                }
                startActivityForResult(inte, reqPicker)
            }
        }
        binding.fabLoad.setOnClickListener {
            if(versionIsSelected()) {
                versionStr?.let { it1 -> loadConfig(isSelf = true) }
            }
        }
        binding.fabLoad.setOnLongClickListener {
            if(versionIsSelected()) {
                versionStr?.let { it1 -> loadConfig(isSelf = false) }
            }
            true
        }

        binding.editconfig.addTextChangedListener {
            //debug(it.toString())
        }
        
        binding.fabRun.setOnClickListener {
            if(versionIsSelected()) {
                if(isSAF) {
                    val obbUri = Uri.parse(sharedpref.getString("obbUri", null))
                    val dataUri = Uri.parse(sharedpref.getString("dataUri", null))
                    obbUri?.renameTo(pkgApp)
                    dataUri?.renameTo(pkgApp)
                    openSettings(pkgApp)
                } else {
                    pkgApp.renameTo()
                    openSettings(pkgApp)
                }
            }
        }
        binding.fabSave.setOnClickListener {
            if(versionIsSelected()) {
                try {
                    val saveFile = File("${filesDir}/UserCustom.ini")
                    val backupFile = File("${filesDir}/backup$versionStr.ini")
                    val content = binding.editconfig.text.split("\n").filter { it.isNotBlank() }.joinToString("\n")
                    if(content.isEmpty() || content.lines().size < 10){
                        toast(this, "Format Config Salah -2")
                    } else if (content.contains("BackUp DeviceProfile") or content.contains("UserCustom DeviceProfile")) {
                        toast(this, "Format Config Salah")
                    } else {
                        var stringBuilder = StringBuilder()
                        stringBuilder.append("[UserCustom DeviceProfile]")
                        stringBuilder.append("\n")
                        stringBuilder.append(content)
                        stringBuilder.append("\n\n\n")
                        stringBuilder.append("[BackUp DeviceProfile]")
                        stringBuilder.append("\n")
                        stringBuilder.append(backupFile.readText())
                        FileOutputStream(saveFile).use { output ->
                            output.write(stringBuilder.toString().toByteArray())
                        }
                        configIsReady().also {
                            if(it) toast(this,"Config saved..") else toast(this, "Failed to save")
                        }
                    }
                } catch (ee: NullPointerException) {
                    clearVersion()
                    ee.printStackTrace()
                    debug("UserCustom not found")
                } catch (e: IOException) {
                    debug(e.toString())
                    e.printStackTrace()
                }
            }
        }

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
            checkPerm(reqObb)
        }
        // clear sav first
        clearVersion(config = false)
    }
    private fun clearVersion(sav: Boolean = true, config: Boolean = true): Boolean {
        return try {
            binding.pubgVersion.clearCheck()
            sav && File("${filesDir}/Active.sav").delete()
            config && File("${filesDir}/UserCustom.ini").delete()
            versionApp = null
            versionStr = null
            sharedpref.edit {
                putString("version", null)
                putString("pkg_version", null)
            }
            true
        } catch (e: Exception) {
            debug("${e.message}")
            toast(this, "Error 0x274")
            false
        }
    }
    private fun configIsReady(): Boolean {
        val uCustom = "UserCustom.ini"
        val activeSav = "Active.sav"
        val pathConfig = File("$filesDir/UserCustom.ini")
        val pathSav = File("$filesDir/Active.sav")
        val isSav = pathSav.exists() && pathSav.canRead()
        val isConfig = pathConfig.exists() && pathConfig.canRead()
        try {
            if (isSAF) {
                val treeUserCustom = DocumentFile.fromTreeUri(
                    this,
                    Uri.parse(sharedpref.getString("userCustom${versionStr}Uri", null))
                )
                val treeSav = DocumentFile.fromTreeUri(
                    this,
                    Uri.parse(sharedpref.getString("active${versionStr}Uri", null))
                )
                isConfig && treeUserCustom?.findFile("UserCustom.ini").also {
                    if (it == null) {
                        debug("UserCustom not found")
                        debug("create file")
                        val createFile = treeUserCustom?.createFile("text/", "UserCustom.ini")
                        createFile?.uri?.let { uriFile ->
                            helper.writeContent(
                                this,
                                uriFile,
                                pathConfig.readBytes()
                            )
                        }
                    } else {
                        if (it.exists() == true and (it.isFile) and (it.canRead())) {
                            it.uri.let { ur ->
                                debug(helper.delete(this, "UserCustom.ini", ur).toString())
                                val createFile = treeUserCustom?.createFile("text/", "UserCustom.ini")
                                createFile?.uri?.let { uriFile ->
                                    helper.writeContent(
                                        this,
                                        uriFile,
                                        pathConfig.readBytes()
                                    )
                                }
                            }
                        } else {
                            debug("Failed to modified UserCustom")
                        }
                    }
                } != null
                isSav && treeSav?.findFile("Active.sav").also {
                    if (it == null) {
                        debug("Active not found")
                        debug("create file")
                        val createFile = treeSav?.createFile("text/", "Active.sav")
                        createFile?.uri?.let { uriFile ->
                            helper.writeContent(
                                this,
                                uriFile,
                                pathSav.readBytes()
                            )
                        }
                    } else {
                        if (it.exists() == true and (it.isFile) and (it.canRead())) {
                            it.uri.let { ur ->
                                helper.delete(this, "Active.sav", ur)
                                val createFile = treeSav?.createFile("text/", "Active.sav")
                                createFile?.uri?.let { uriFile ->
                                    helper.writeContent(
                                        this,
                                        uriFile,
                                        pathSav.readBytes()
                                    )
                                }
                            }
                        } else {
                            debug("Failed to modified Active")
                        }
                    }
                } != null
                isReady = true
                return true
            } else {
                val userCustom = File("${Utils.externalStorageDir}/${configPath}/$uCustom")
                val active = File("${Utils.externalStorageDir}/${savPath}/$activeSav")
                debug("copy to android")
                val writeU = File("${filesDir}/${uCustom}")
                val writeA = File("${filesDir}/${activeSav}")
                writeU.copyTo(userCustom, overwrite = true)
                if (writeA.isFile) writeA.copyTo(active, overwrite = true)
                isReady = true
                return true
            }
        } catch (e: Exception) {
            debug("Error: ${e.message}")
            toast(this,"Error 0x1 8234")
            isReady = false
            return false
        }
    }
    private fun versionIsSelected(): Boolean {
        return if(binding.pubgVersion.checkedRadioButtonId == -1 && versionApp == null) {
            toast(this, "Select version first")
            false
        } else true
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
        sharedpref.edit {
            putString("version", versionApp)
            putString("pkg_version", pkgApp)
            apply()
        }
        if(sharedpref.getString(pkgApp,null) == null){
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
                requestPerms(savPath)
            } else {
                !isExist("${versionStr}.ini") && clearVersion()
                !isExist("${versionStr}.sav") && clearVersion()
            }
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
            if (!status){
                tree?.findFile("${pkg}_khenji")?.let {
                    if (it.isDirectory){
                        if(it.renameTo("${pkg}"))status=true
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    // rename for android 10 or below
    private fun String.renameTo() {
        try {
            val pkg = this
            val obbPath = File("${Utils.externalStorageDir}/Android/obb/$pkg")
            val dataPath =
                File("${Utils.externalStorageDir}/Android/data/$pkg")
            val dataDestPath =
                File("${Utils.externalStorageDir}/Android/data/${pkg}_khenji")
            val obbDestPath =
                File("${Utils.externalStorageDir}/Android/obb/${pkg}_khenji")

            if (dataPath.exists())dataPath.renameTo(dataDestPath)
            else if (dataDestPath.exists())dataDestPath.renameTo(dataPath)
            if (obbPath.exists()) obbPath.renameTo(obbDestPath)
            else if (obbDestPath.exists())obbDestPath.renameTo(obbPath)
        } catch (e: IOException) {
            debug(e.toString())
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
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun requestPerms(path: String): Boolean {
        return if(isSAF) {
            checkPerm(reqActive) && checkPerm(reqUserCustom)
        } else {
            true
        }
    }
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun checkPerm(reqCode: Int): Boolean {
        when (reqCode) {
            reqActive -> {
                sharedpref.getString("active${versionStr}Uri", null).let {
                    if (it == null)requestDocumentPermission(savPath, reqCode)
                    if (it != null) {
                        if(!permissionGranted(it))requestDocumentPermission(savPath, reqCode)
                        else !isExist("${versionStr}.sav") && clearVersion(); return true
                    }
                }
            }
            reqUserCustom -> {
                sharedpref.getString("userCustom${versionStr}Uri", null).let {
                    if (it == null) requestDocumentPermission("$configPath", reqCode)
                    if (it != null) {
                        if(!permissionGranted(it))requestDocumentPermission("$configPath", reqCode)
                        else !isExist("${versionStr}.ini") && clearVersion(); return true
                    }
                }
            }
            reqData -> {
                sharedpref.getString("dataUri", null).let {
                    if (it == null) requestDocumentPermission("$dataPath", reqCode)
                    if (it != null) {
                        if(!permissionGranted(it))requestDocumentPermission("$dataPath", reqCode) else return true
                    }
                }
            }
            reqObb -> {
                sharedpref.getString("obbUri", null).let {
                    if (it == null) requestDocumentPermission("$obbPath", reqCode)
                    if (it != null) {
                        if(!permissionGranted(it))requestDocumentPermission("$obbPath", reqCode) else return true
                    }
                }
            }
        }
        return false
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
    val isExist: (String) -> Boolean = fun(f: String): Boolean {
        return if (File("$filesDir/$f").exists()) {
            debug("file $f ada")
            true
        } else {
            debug("file $f tidak ada")
            loadConfigs()
        }
        //return if !File("$filesDir/$f").exists() else helper.copyToData(this, "UserCustom.ini", "${versionStr}.ini", Uri.parse(sharedpref.getString("${versionStr}", null)))
    }
    fun html(initz: String.() -> Unit): String {
        val hehe = String()
        hehe.initz()
        return hehe
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
                            //!helper.copyToData(this, "UserCustom.ini", "${versionStr}.ini", treeUri) && clearVersion()
                            !loadConfig("config") && clearVersion()
                            checkPerm(reqActive)
                        }
                        reqActive -> {
                            sharedpref.edit(commit = true, action = {
                                this.putString("active${versionStr}Uri", treeUri.toString())
                            })
                            //!helper.copyToData(this, "Active.sav", "${versionStr}.sav", treeUri) && clearVersion()
                            !loadConfig("sav") && clearVersion()
                            checkPerm(reqUserCustom)
                        }
                        else -> {}
                    }
                }
            }
        } else if (result.resultCode == RESULT_CANCELED) {
            if (reqNow == reqDetailsApp || sharedpref.getInt("reqNow", 0) == reqDetailsApp) {
                val obbUri = Uri.parse(sharedpref.getString("obbUri", null))
                val dataUri = Uri.parse(sharedpref.getString("dataUri", null))
                obbUri?.renameTo(pkgApp)
                dataUri?.renameTo(pkgApp)
            }
        }
    }
    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == reqPicker) {
            data?.data.also { uri ->
                if (uri != null) {
                    if (Utils.isDownloadsDocument(uri)) {
                        grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    helper.copyToData(ctx = this, filename = "Active.sav", uri = uri).also {
                        if(it) toast(this,"Sav loaded")
                    }
                }
            }
        } else if (resultCode == RESULT_CANCELED && requestCode == reqDetailsApp) {
            pkgApp.renameTo()
        }
    }
    private fun appInstalledOrNot(uri: String): Boolean {
        val pm = packageManager
        val flags = 0
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
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
        val intt = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + packageName))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            reqNow = reqDetailsApp
            startForResult.launch(intt)
            sharedpref.edit { putInt("reqNow", reqDetailsApp) }
        } else {
            sharedpref.edit { putInt("reqNow", reqDetailsApp) }
            startActivityForResult(intt, reqDetailsApp)
        }
    }
    // for Storage Access Framework
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun loadConfig(filename: String): Boolean {
        try {
            var content: String? = null
            var status = true
            val backupFile = File("${filesDir}/backup${versionStr}.ini")
            var matches = false
            if(filename == "config") {
                val treeUri =
                    sharedpref.getString("userCustom${versionStr}Uri", null).let {
                        if (it == null) toast(this, "Failed permission 0")
                        DocumentFile.fromTreeUri(this, Uri.parse(it))
                    }
                treeUri?.findFile("UserCustom.ini").also {
                    if (it == null) toast(this, "File UserCustom not found -6")
                    else content = helper.readContent(this, it.uri, "$versionStr.ini")
                }
            } else if(filename == "sav") {
                val treeUriSav =
                    sharedpref.getString("active${versionStr}Uri", null).let {
                        if (it == null) toast(this, "Failed permission -1")
                        DocumentFile.fromTreeUri(this, Uri.parse(it))
                    }
                treeUriSav?.findFile("Active.sav").also {
                    if (it == null) toast(this, "File Sav not found -4")
                    else helper.readContent(this, it.uri, "$versionStr.sav")
                }
            }
            if(filename == "config" && content != null) {
                val writeBackup = FileOutputStream(backupFile)
                for (str in content?.lines()!!) {
                    if (!matches) {
                        if (str.contains("BackUp DeviceProfile"))
                            matches = true
                    } else {
                        if (str.contains("UserCustom DeviceProfile")) break
                        if (str.isBlank()) continue
                        writeBackup.write(str.toByteArray())
                        writeBackup.write("\n".toByteArray())
                    }
                }
            }
            return true
        } catch (e: Exception) {
            debug("Error ${e.message}")
            e.printStackTrace()
            return false
        }
    }
    private fun loadConfigs(isBackUp: Boolean = true): Boolean {
        try {
            var content: String? = null
            var contentSav: String? = null
            var status = true
            val toFile = File("$filesDir/$versionStr.ini")
            val toSav = File("$filesDir/$versionStr.sav")
            val backupFile = File("${filesDir}/backup${Companion.versionStr}.ini")
            var matches = false
            if (isSAF) {
                debug("from android 10")
                val treeUri =
                    sharedpref.getString("userCustom${Companion.versionStr}Uri", null).let {
                        if (it == null) toast(this, "Failed permission 0")
                        DocumentFile.fromTreeUri(this, Uri.parse(it))
                    }
                treeUri?.findFile("UserCustom.ini").also {
                    if (it == null) toast(this, "File UserCustom not found -6")
                    else content = helper.readContent(this, it.uri, "$versionStr.ini")
                }
                val treeUriSav =
                    sharedpref.getString("active${Companion.versionStr}Uri", null).let {
                        if (it == null) toast(this, "Failed permission -1")
                        DocumentFile.fromTreeUri(this, Uri.parse(it))
                    }
                treeUriSav?.findFile("Active.sav").also {
                    if (it == null) toast(this, "File Sav not found -4")
                    else contentSav = helper.readContent(this, it.uri, "$versionStr.sav")
                }
            } else {
                val fromConfigFile = File("${Utils.externalStorageDir}/$configPath/UserCustom.ini")
                val fromSavFile = File("${Utils.externalStorageDir}/$savPath/Active.sav")
                content = fromConfigFile.readText()
                contentSav = fromSavFile.readText()
                debug("from android below 10")
                toFile.writeText(fromConfigFile.readText())
                toSav.writeText(fromSavFile.readText())
            }
            if(isBackUp && content != null) {
                val writeBackup = FileOutputStream(backupFile)
                for (str in content?.lines()!!) {
                    if (!matches) {
                        if (str.contains("BackUp DeviceProfile"))
                            matches = true
                    } else {
                        if (str.contains("UserCustom DeviceProfile")) break
                        if (str.isBlank()) continue
                        writeBackup.write(str.toByteArray())
                        writeBackup.write("\n".toByteArray())
                    }
                }
            }
            return true
        } catch (e: Exception) {
            debug("Error ${e.message}")
            return false
        }
    }
    private fun loadConfig(isSelf: Boolean = false) {
        try {
            var content: String? = null
            var status: Boolean = true
            val strBuilder = StringBuilder()
            if(isSelf) {
                val check = File("$filesDir/UserCustom.ini")
                if (check.exists() and check.isFile and check.canRead()){
                    content = check.readText()
                } else toast(this, "File UserCustom not found")
            } else {
                if (isSAF) {
                    debug("from android 111")
                    val treeUri =
                        sharedpref.getString("userCustom${versionStr}Uri", null).let {
                            if (it == null) toast(this, "Failed permission")
                            DocumentFile.fromTreeUri(this, Uri.parse(it))
                        }
                    val check = treeUri?.findFile("UserCustom.ini").also { it ->
                        if (it == null) toast(this, "File UserCustom not found")
                        else {
                            content = helper.readContent(this, it.uri, "UserCustom.ini")
                        }
                    }
                } else {
                    debug("from android below 11")
                    val from = File("${Utils.externalStorageDir}/$configPath/UserCustom.ini")
                    content = from.readText()
                }
            }
            var matches = false
            for (str in content?.lines()!!) {
                if (!matches) {
                    if (str.contains("UserCustom DeviceProfile")) matches = true
                } else {
                    if (str.contains("BackUp DeviceProfile")) break
                    strBuilder.append(str)
                    strBuilder.append("\n")
                }
            }
            binding.editconfig.setText(strBuilder.toString().split("\n").filter { it.isNotBlank() }.joinToString("\n"))
        } catch (e: Exception) {
            debug("Error ${e.message}")
            e.printStackTrace()
        }
    }

    private fun readConfig(versionStr: String) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
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
    }
    private fun listFiles(folder: DocumentFile): List<Uri> {
        return if (folder.isDirectory) {
            folder.listFiles().mapNotNull { file ->
                if (file.name != null) file.uri else null
            }
        } else emptyList()
    }
}

