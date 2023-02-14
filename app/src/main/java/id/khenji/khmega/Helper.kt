package id.khenji.khmega

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import id.khenji.khmega.MainActivity.Companion.debug
import id.khenji.khmega.MainActivity.Companion.toast
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
            toast(ctx, "Error: ${e.message}")
            e.printStackTrace()
            return ""
        }
    }
    fun copyToData(ctx: Context, filename: String, newFile: String, uri: Uri): Boolean {
        var fis: InputStream? = null
        var fos: OutputStream? = null
        val output: OutputStream?
        val to: File
        try {
            val tree = DocumentFile.fromTreeUri(ctx, uri)
            val checkFile = tree?.findFile(filename)
            if(checkFile?.exists() == true && checkFile?.isFile == true) {
                to = File("${ctx.filesDir}/$newFile")
                output = FileOutputStream(to)
                val content = ctx.contentResolver
                fis = content.openInputStream(checkFile.uri)
                fos = output
                val buff = ByteArray(1024)
                var length = 0
                while (fis!!.read(buff).also { length = it } > 0) {
                    fos.write(buff, 0, length)
                }
            } else {
                toast(ctx, "File $filename not found 0x1")
                return false
                //throw IOException("File $filename not found")
            }
        } catch (e: IOException) {
            debug(e.toString())
            toast(ctx, "Error: ${e.message}")
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
    fun copyToData(ctx: Context, filename: String, uri: Uri): Boolean {
        var fis: InputStream? = null
        var fos: OutputStream? = null
        var output: OutputStream?
        var to: File
        try {
            to = File("${ctx.filesDir}/$filename")
            output = FileOutputStream(to)
            val content = ctx.contentResolver
            fis = content.openInputStream(uri)
            fos = output
            val buff = ByteArray(1024)
            var length = 0
            while (fis!!.read(buff).also { length = it } > 0) {
                fos.write(buff, 0, length)
            }
        } catch (e: IOException) {
            debug(e.toString())
            toast(ctx, "Error: ${e.message}")
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
    fun delete(ctx: Context, filename: String, uri: Uri): Boolean {
        return try {
            val tree = DocumentFile.fromTreeUri(ctx, uri)
            return tree?.findFile(filename).let { its ->
                if(its != null) {
                    its.exists() && its.isFile && its.delete()
                } else false
            }
        } catch (e: Exception) {
            e.message?.let { debug(it) }
            false
        }
    }
    fun writeContent(ctx: Context, uri: Uri, content: ByteArray): Boolean {
        return try {
            ctx.contentResolver.openFileDescriptor(uri, "w")?.use { it ->
                FileOutputStream(it.fileDescriptor).use { output ->
                    output.write(content)
                }
            }
            true
        } catch (e: FileNotFoundException) {
            toast(ctx, "Error: ${e.message}")
            e.printStackTrace()
            false
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }
    fun copyFile(ctx: Context, filename: String, uri: Uri): Boolean {
        return try {
            val to = DocumentFile.fromTreeUri(ctx, uri)
            to!!.findFile(filename)?.let {
                if (it.exists() and it.isFile) it.delete() else false
            }
            val uir: DocumentFile? = to.createFile("text/", filename)
            ctx.contentResolver.openOutputStream(uir!!.uri)!!.use { output ->
                ctx.openFileInput(filename)?.use { input ->
                    val buff = ByteArray(1024)
                    var length = 0
                    while (input.read(buff).also { length = it } > 0) {
                        output.write(buff, 0, length)
                    }
                }

            }
            true
        } catch (e: IOException) {
            false
        }
    }
    fun readContent(ctx: Context, uri: Uri, to: String): String {
        return try {
            val dest = File("${ctx.filesDir}/$to")
            ctx.contentResolver.openFileDescriptor(uri, "r")?.use { fd ->
                FileInputStream(fd.fileDescriptor).use { input ->
                    FileOutputStream(dest).use { output ->
                        val buffer = ByteArray(1024)
                        var length = 0
                        while (input.read(buffer).also { length = it } > 0) {
                            output.write(buffer, 0, length)
                        }
                    }
                }
            }
            dest.readText()
        } catch (e: FileNotFoundException) {
            toast(ctx, "Error: ${e.message}")
            e.printStackTrace()
            "Error: ${e.message}"
        } catch (e: IOException) {
            e.printStackTrace()
            "Error: ${e.message}"
        }
    }
}
