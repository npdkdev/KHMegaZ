package id.khenji.khmega

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.webkit.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit

import id.khenji.khmega.databinding.SplashBinding


class Splash : AppCompatActivity() {

    private lateinit var binding: SplashBinding
    private lateinit var sharedpref: SharedPreferences

    companion object {
        var isFirstRun: Boolean = true
    }

    override fun onCreate(_savedInstanceState: Bundle?) {
        super.onCreate(_savedInstanceState)
        binding = SplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
        window.statusBarColor = Color.TRANSPARENT
        sharedpref = getSharedPreferences("khenji", MODE_PRIVATE)
        isFirstRun = sharedpref.getBoolean("firstrun", true)
        checkPermission()
    }
    private fun String.checkPerms(): Boolean {
        val permission = this
        return (ContextCompat.checkSelfPermission(this@Splash, permission) == PackageManager.PERMISSION_DENIED)
    }
    private fun checkPermission() {
        if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            if (Manifest.permission.WRITE_EXTERNAL_STORAGE.checkPerms()
                or Manifest.permission.READ_EXTERNAL_STORAGE.checkPerms()) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ),
                    1000
                )
            } else {
                Handler(Looper.getMainLooper()).postDelayed({
                    startActivity(Intent(this@Splash, MainActivity::class.java))
                    finishAffinity()
                },1000)
            }
        }
    }
    override fun onResume() {
        super.onResume()
        if (Splash.isFirstRun){
            sharedpref.edit(commit = true, action = {
                this.putBoolean("firstrun", false)
            })
        }
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1000) {
            if (Splash.isFirstRun){
                sharedpref.edit(commit = true, action = {
                    this.putBoolean("firstrun", false)
                })
            }
            startActivity(Intent(this@Splash, MainActivity::class.java))
        }
    }
}