package com.example.finsnap.view

import android.Manifest
import android.content.ContentValues.TAG
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import androidx.room.Room
import com.example.finsnap.R
import com.example.finsnap.databinding.ActivityMainBinding
import com.example.finsnap.viewmodel.SessionManager
import com.example.finsnap.viewmodel.UserDatabase
import com.example.finsnap.viewmodel.DatabaseManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private lateinit var database: UserDatabase
    private lateinit var binding: ActivityMainBinding
    private val SMS_PERMISSION_CODE = 100
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navController: NavController
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SessionManager.init(this)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        drawerLayout = binding.drawerLaeyout

        val toolbar = findViewById<Toolbar>(R.id.mytoolbar)
        setSupportActionBar(toolbar)

        navController = findNavController(R.id.nav_host_fragment)

        val appBarConfiguration = AppBarConfiguration(
            setOf(R.id.homeFragment, R.id.nav_acount, R.id.nav_info),
            drawerLayout
        )

        // Setup Navigation View
        val navigationView = findViewById<NavigationView>(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener(this)

        val toggle = ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.open_nav, R.string.close_nav)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)
        bottomNav.setupWithNavController(navController)
        
        // Handle bottom navigation clicks with proper back stack management
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.homeFragment -> {
                    navController.navigate(R.id.action_global_homeFragment)
                    true
                }
                R.id.cashTransactionFragment -> {
                    navController.navigate(R.id.action_global_cashTransactionFragment)
                    true
                }
                R.id.articleFragment -> {
                    navController.navigate(R.id.action_global_articleFragment)
                    true
                }
                else -> false
            }
        }

        database = DatabaseManager.getDatabase(applicationContext)

        checkSmsPermission()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean{
        when(item.itemId){
            R.id.nav_acount -> {
                // Use Navigation Component for drawer items too
                navController.navigate(R.id.nav_acount)
            }
            R.id.nav_info -> {
                navController.navigate(R.id.nav_info)
            }
            R.id.nav_logout -> {
                // Handle logout separately
                Log.d(TAG, "Logout selected")
                SessionManager.logout(this)
                return true
            }
        }

        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        return NavigationUI.navigateUp(navController, drawerLayout)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            // Check current destination
            val currentDestId = navController.currentDestination?.id
            
            // If we're on a detail fragment, navigate back to the appropriate parent
            when (currentDestId) {
                R.id.transactionDetailFragment -> {
                    navController.navigate(R.id.homeFragment)
                }
                R.id.addCashFragment -> {
                    navController.navigate(R.id.cashTransactionFragment)
                }
                R.id.categoryFragment -> {
                    navController.navigateUp()
                }
                // For top level destinations, use the default back behavior
                R.id.homeFragment, R.id.cashTransactionFragment, R.id.articleFragment -> {
                    super.onBackPressed()
                }
                else -> {
                    if (!navController.navigateUp()) {
                        super.onBackPressed()
                    }
                }
            }
        }
    }

    private fun checkSmsPermission() {
        if (ContextCompat.checkSelfPermission(
               this,
                Manifest.permission.READ_SMS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Permission not granted, request it
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_SMS),
                SMS_PERMISSION_CODE
            )
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == SMS_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
                return;
            } else {
                // Permission denied
                Toast.makeText(
                    this,
                    "SMS permission is required to show messages",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}