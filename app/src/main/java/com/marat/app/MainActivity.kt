package com.marat.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.marat.app.data.PrefManager

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navHost = supportFragmentManager
            .findFragmentById(R.id.nav_host) as NavHostFragment
        val navController = navHost.navController

        val bottom = findViewById<BottomNavigationView>(R.id.bottom_nav)
        bottom.setupWithNavController(navController)

        // показываем панель только на вкладках-табах
        val tabs = setOf(R.id.homeFragment, R.id.touchpadFragment, R.id.profileFragment)
        navController.addOnDestinationChangedListener { _, d, _ ->
            bottom.visibility = if (d.id in tabs) BottomNavigationView.VISIBLE else BottomNavigationView.GONE
        }

        // сразу на Home, если пользователь уже авторизован
        if (PrefManager(this).isLoggedIn())
            navController.navigate(R.id.homeFragment)
    }
}