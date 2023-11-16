package com.map.main.auth

import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.map.R
import com.map.databinding.ActivityGetStartedBinding
import com.map.databinding.ActivityMainBinding
import com.map.main.MainActivity
import com.map.utils.PermissionUtils

class GetStartedActivity : AppCompatActivity() {

    private val LOCATION_PERMISSION_REQUEST_CODE = 1001

    private lateinit var binding: ActivityGetStartedBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGetStartedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btn.setOnClickListener {
            // Check and request location permissions if needed
            if (PermissionUtils.checkLocationPermission(this)) {
                startActivity(Intent(this, MainActivity::class.java))
            } else {
                requestLocationPermission()
            }

        }
    }

    private fun requestLocationPermission() {
        PermissionUtils.requestLocationPermission(this, LOCATION_PERMISSION_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startActivity(Intent(this, MainActivity::class.java))
                } else {
                    // Handle the case where the user denies the permission
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}