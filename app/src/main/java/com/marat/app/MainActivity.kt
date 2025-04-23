package com.marat.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import com.marat.app.data.PrefManager

class MainActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navHost = supportFragmentManager
            .findFragmentById(R.id.nav_host) as NavHostFragment
        val navController = navHost.navController

        if (PrefManager(this).isLoggedIn()) {
            navController.navigate(R.id.homeFragment)
        }
    }
}