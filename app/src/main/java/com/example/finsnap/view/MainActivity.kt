package com.example.finsnap.view

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
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
import androidx.fragment.app.FragmentTransaction
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import androidx.room.Room
import com.example.finsnap.R
import com.example.finsnap.databinding.ActivityMainBinding
import com.example.finsnap.viewmodel.UserDatabase
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private lateinit var database: UserDatabase
    private lateinit var binding: ActivityMainBinding
    private val SMS_PERMISSION_CODE = 100
    private lateinit var drawerLayout: DrawerLayout
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        drawerLayout = binding.drawerLaeyout

        val toolbar = findViewById<Toolbar>(R.id.mytoolbar)
        setSupportActionBar(toolbar)

        val navController = findNavController(R.id.nav_host_fragment)

        val navigationView = findViewById<NavigationView>(R.id.nav_view)
        navigationView.setupWithNavController(navController)  // 🔄 Use setupWithNavController

        val toggle = ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.open_nav, R.string.close_nav)
        toggle.syncState()

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)
        bottomNav.setupWithNavController(navController)

        database = Room.databaseBuilder(
            applicationContext,
            UserDatabase::class.java,
            "UserDatabase"
        ).fallbackToDestructiveMigration().build()

        checkSmsPermission()
    }



    override fun onNavigationItemSelected(item: MenuItem): Boolean{
        when(item.itemId){
            R.id.nav_acount -> replaceFragment(AcountFragment())
            R.id.nav_info -> replaceFragment(InfoFragment())
        }

        drawerLayout.closeDrawer(GravityCompat.START)
        return true

    }

    private fun replaceFragment(fragment: Fragment) {
        val transaction : FragmentTransaction =supportFragmentManager.beginTransaction()
        transaction.replace(R.id.nav_host_fragment,fragment)
        transaction.commit()
    }


    override fun onBackPressed() {
        super.onBackPressed()
        if(drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)

        }else{
            onBackPressedDispatcher.onBackPressed()
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