package org.staacks.alpharemote

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.staacks.alpharemote.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    companion object {
        const val NAVIGATE_TO_INTENT_EXTRA = "nav_to"
        val TAG: String = "alpharemote"
    }
    val SELECTED_PAGE = "selected_page"

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()

        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)

        navView.setupWithNavController(navController)

        var startPage = intent?.getIntExtra(NAVIGATE_TO_INTENT_EXTRA, R.id.navigation_selfie) ?: R.id.navigation_selfie
        startPage = savedInstanceState?.getInt(SELECTED_PAGE, startPage) ?: startPage
        navigateTo(startPage)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        intent?.getIntExtra(NAVIGATE_TO_INTENT_EXTRA, R.id.navigation_selfie)?.let {
            navigateTo(it)
        }
    }

    fun navigateTo(id: Int) {
        binding.navView.selectedItemId = when (id) {
            R.id.navigation_selfie, R.id.navigation_settings, R.id.navigation_manual, R.id.navigation_about -> id
            else -> R.id.navigation_selfie
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(SELECTED_PAGE, binding.navView.selectedItemId)
    }

}
