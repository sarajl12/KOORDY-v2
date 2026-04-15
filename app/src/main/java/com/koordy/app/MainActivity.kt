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

        // ── Mode développement : pré-remplit la session sans login ─────────────
        if (com.koordy.app.utils.Constants.DEV_MODE) {
            if (session.idAssociation == -1)
                session.idAssociation = com.koordy.app.utils.Constants.DEV_ID_ASSO
            if (session.idMembre == -1)
                session.idMembre = com.koordy.app.utils.Constants.DEV_ID_MEMBRE
        }
        // ───────────────────────────────────────────────────────────────────────

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Détermine le graph de démarrage selon la session
        val navGraph = navController.navInflater.inflate(R.navigation.nav_graph)
        // TODO: remettre la vérification session quand le login sera stable
        // if (session.isLoggedIn()) {
        //     navGraph.setStartDestination(R.id.homeAssociationFragment)
        // } else {
        //     navGraph.setStartDestination(R.id.loginFragment)
        // }
        navGraph.setStartDestination(R.id.homeAssociationFragment)
        navController.graph = navGraph

        // Bottom nav visible seulement quand connecté
        binding.bottomNav.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val authScreens = setOf(
                R.id.loginFragment,
                R.id.inscriptionFragment,
                R.id.inscriptionAssociationFragment,
                R.id.formAssociationFragment,
                R.id.designAssociationFragment,
                R.id.successAssociationFragment,
                R.id.rechercheAssociationFragment
            )
            // La bottom nav est visible sur toutes les pages "asso" (hors auth)
            binding.bottomNav.visibility =
                if (destination.id in authScreens)
                    android.view.View.GONE
                else
                    android.view.View.VISIBLE
        }
    }
}
