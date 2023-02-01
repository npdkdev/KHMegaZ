package id.khenji.khmega

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import id.khenji.khmega.MainActivity.Companion.debug
import java.io.*


class Helper {
    fun toTreeUri(from: String, filename: String? = null): Uri {
        var prefixUri = "content://com.android.externalstorage.documents/tree/primary%3A"
        var allUri: String = from
        var treeUri: String = "content://com.android.externalstorage.documents/tree/primary%3A"
        var docUri: String ="/document/primary%3A"
        var fileInfo = File(from)
        var stringPath: String = fileInfo.path
        if(stringPath.contains("/sdcard/")) {
            stringPath = stringPath.replace("/sdcard/","")
        } else if(stringPath.contains("/storage/") && stringPath.contains("/emulated/")){
            stringPath = stringPath.replace("/storage/emulated/0/", "")
        }
        if(fileInfo.isFile or (filename != null)){
            val name = filename ?: fileInfo.name
            MainActivity.debug("is file $stringPath")
            stringPath = stringPath.replace(name,"")
            stringPath = if(stringPath.endsWith("/")){
                stringPath.substring(0,stringPath.length-1).replace("/","%2F")
            } else {
                stringPath.replace("/","%2F")
            }
            allUri = "$treeUri$stringPath$docUri$stringPath%2F${name}"
        } else {
            MainActivity.debug("is not file $stringPath")
            stringPath = if(stringPath.endsWith("/")){
                stringPath.substring(0,stringPath.length-1).replace("/","%2F")
            } else {
                stringPath.replace("/","%2F")
            }
            allUri = "$treeUri$stringPath$docUri$stringPath"
        }
        return allUri.toUri()
    }
    fun toTreeUri2(from: String, filename: String? = null): Uri {
        var allUri: String = from
        var treeUri = "content://com.android.externalstorage.documents/tree/primary%3A"
        var fileInfo = File(from)
        var stringPath: String = fileInfo.path
        if(stringPath.contains("/sdcard/")) {
            stringPath = stringPath.replace("/sdcard/","")
        } else if(stringPath.contains("/storage/") && stringPath.contains("/emulated/")){
            stringPath = stringPath.replace("/storage/emulated/0/", "")
        }
        if(fileInfo.isFile or (filename != null)){
            val name = filename ?: fileInfo.name
            stringPath = stringPath.replace(name,"")
            stringPath = if(stringPath.endsWith("/")){
                stringPath.substring(0,stringPath.length-1).replace("/","%2F")
            } else {
                stringPath.replace("/","%2F")
            }
            allUri = "$treeUri$stringPath}"
        } else {
            stringPath = if(stringPath.endsWith("/")){
                stringPath.substring(0,stringPath.length-1).replace("/","%2F")
            } else {
                stringPath.replace("/","%2F")
            }
            allUri = "$treeUri$stringPath"
        }
        return allUri.toUri()
    }
    fun toUri(path: String): String {
        var path = path
        var uriFor1 = ""
        var uriFor2 = ""
        uriFor1 = "content://com.android.externalstorage.documents/tree/primary%3A"
        var uriEnd: String = "/document/primary%3A"
        if (path.endsWith("/")) {
            path = path.substring(0, path.length - 1)
        }
        if (path.contains("/sdcard/")) {
            uriFor2 = path.replace("/sdcard/", "").replace("/", "%2F")
            if (uriFor2.substring(uriFor2.length - 1, uriFor2.length) == "/") {
                uriFor2 = uriFor1.substring(0, uriFor1.length - 1)
            }
        } else {
            if (path.contains("/storage/") && path.contains("/emulated/")) {
                uriFor2 = path.replace("/storage/emulated/0/", "").replace("/", "%2F")
                if (uriFor2.substring(uriFor2.length - 1, uriFor2.length) == "/") {
                    uriFor2 = uriFor1.substring(0, uriFor1.length - 1)
                }
            }
        }
        return uriFor1 + uriFor2.also { uriFor1 = it+uriEnd }
    }
    fun readFromUri(ctx: Context, filename: String, uri: Uri): String {
        try {
            val contentResolver = ctx.contentResolver
            val tree = DocumentFile.fromTreeUri(ctx, uri)
            val checkFile = tree!!.findFile(filename)
            return if(checkFile!!.exists() and checkFile!!.isFile) {
                val inputStream = contentResolver.openInputStream(checkFile!!.uri)
                val reader = BufferedReader(InputStreamReader(inputStream))
                val stringBuilder = StringBuilder()
                var line: String?
                while (reader.readLine().also {
                        line = it
                    } != null) {
                    stringBuilder.append(line)
                }
                stringBuilder.toString()
            } else {
                debug("File not Found")
                "File not Found"
            }
        } catch (e: IOException) {
            e.printStackTrace()
            return ""
        }
    }
    fun copyToData(ctx: Context, filename: String, newFile: String, uri: Uri): Boolean {
        var fis: InputStream? = null
        var fos: OutputStream? = null
        var output: OutputStream?
        var to: File
        try {
            val tree = DocumentFile.fromTreeUri(ctx, uri)
            val checkFile = tree!!.findFile(filename)
            if(checkFile!!.exists() and checkFile!!.isFile) {
                to = File("${ctx.filesDir}/$newFile")
                output = FileOutputStream(to)
                val content = ctx.contentResolver
                fis = content.openInputStream(checkFile!!.uri)
                fos = output
                val buff = ByteArray(1024)
                var length = 0
                while (fis!!.read(buff).also { length = it } > 0) {
                    fos.write(buff, 0, length)
                }
            }
        } catch (e: IOException) {
            MainActivity.debug(e.toString())
            e.printStackTrace()
            return false
        } finally {
            if (fis != null) {
                try {
                    fis.close()
                } catch (e: IOException) {
                    MainActivity.debug(e.toString())
                    e.printStackTrace()
                    return false
                }
            }
            if (fos != null) {
                try {
                    fos.close()
                } catch (e: IOException) {
                    MainActivity.debug(e.toString())
                    e.printStackTrace()
                    return false
                }
            }
        }
        return true
    }
    fun writeContent(ctx: Context, uri: Uri, content: ByteArray) {
        try {
            MainActivity.debug("test write ${uri.path}")
            ctx.contentResolver.openFileDescriptor(uri, "w")?.use { it ->
                FileOutputStream(it.fileDescriptor).use { output ->
                    //output.write(("Test write content ${System.currentTimeMillis()}\n").toByteArray())
                    output.write(content)
                }
            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}