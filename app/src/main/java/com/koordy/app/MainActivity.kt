package com.koordy.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.koordy.app.databinding.ActivityMainBinding
import com.koordy.app.utils.SessionManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        session = SessionManager(this)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Détermine le graph de démarrage selon la session
        val navGraph = navController.navInflater.inflate(R.navigation.nav_graph)
        if (session.isLoggedIn()) {
            navGraph.setStartDestination(R.id.homeAssociationFragment)
        } else {
            navGraph.setStartDestination(R.id.landingFragment)
        }
        navController.graph = navGraph

        // Bottom nav visible seulement quand connecté
        binding.bottomNav.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val authScreens = setOf(
                R.id.landingFragment,
                R.id.loginFragment,
                R.id.inscriptionFragment,
                R.id.inscriptionAssociationFragment,
                R.id.formAssociationFragment,
                R.id.successAssociationFragment,
                R.id.rechercheAssociationFragment
            )
            binding.bottomNav.visibility =
                if (destination.id in authScreens)
                    android.view.View.GONE
                else
                    android.view.View.VISIBLE
        }
    }
}
