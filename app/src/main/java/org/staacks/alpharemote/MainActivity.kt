package org.staacks.alpharemote

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.navOptions
import org.staacks.alpharemote.ui.components.SelfieNavigationBar
import org.staacks.alpharemote.ui.appearance.AppearanceViewModel
import org.staacks.alpharemote.ui.appearance.RuntimeSelfieTheme
import androidx.lifecycle.ViewModelProvider
import org.staacks.alpharemote.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val selectedPage = mutableIntStateOf(R.id.navigation_selfie)
    private val destinationListener = NavController.OnDestinationChangedListener { _, destination, _ ->
        selectedPage.intValue = destination.id
    }

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

        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        navController.addOnDestinationChangedListener(destinationListener)
        binding.navView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        val appearance = ViewModelProvider(this)[AppearanceViewModel::class.java]
        binding.navView.setContent {
            RuntimeSelfieTheme(appearance) { SelfieNavigationBar(selectedPage.intValue, ::navigateTo) }
        }

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
        val target = when (id) {
            R.id.navigation_selfie, R.id.navigation_settings, R.id.navigation_manual, R.id.navigation_about -> id
            else -> R.id.navigation_selfie
        }
        val nav = findNavController(R.id.nav_host_fragment_activity_main)
        if (nav.currentDestination?.id != target) {
            nav.navigate(target, null, navOptions {
                launchSingleTop = true
                restoreState = true
                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            })
        }
    }

    override fun onDestroy() {
        findNavController(R.id.nav_host_fragment_activity_main).removeOnDestinationChangedListener(destinationListener)
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(SELECTED_PAGE, selectedPage.intValue)
    }

}
