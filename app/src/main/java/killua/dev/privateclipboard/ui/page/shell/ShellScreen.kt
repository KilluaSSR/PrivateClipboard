package killua.dev.privateclipboard.ui.page.shell

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavBackStackEntry
import dagger.hilt.android.qualifiers.ApplicationContext
import killua.dev.arsenal.material.templates.topbars.ArsenalTopAppBarDefaults
import killua.dev.arsenal.navigation.annotation.NavDestination
import killua.dev.arsenal.navigation.navbar.ArsenalNavBar
import killua.dev.arsenal.navigation.navbar.NavBarEntry
import killua.dev.arsenal.navigation.navbar.NavBarStyle
import killua.dev.arsenal.spi.annotation.SpiImpl
import killua.dev.privateclipboard.R
import killua.dev.privateclipboard.ui.page.home.HomeTab
import killua.dev.privateclipboard.ui.page.settings.SettingsTab
import javax.inject.Inject

@NavDestination(route = "shell", start = true)
@Composable
fun ShellScreen() {
    ArsenalNavBar(
        style = NavBarStyle.Adaptive,
        containerColor = ArsenalTopAppBarDefaults.containerColor(),
    )
}

@SpiImpl
class HomeNavEntry @Inject constructor(@ApplicationContext context: Context) : NavBarEntry {
    override val route = "tab_home"
    override val order = 0
    override val label: String = context.getString(R.string.nav_home)
    override val selectedIcon: ImageVector = Icons.Rounded.Home
    override val unselectedIcon: ImageVector = Icons.Outlined.Home

    @Composable
    override fun Content(entry: NavBackStackEntry) = HomeTab()
}

@SpiImpl
class SettingsNavEntry @Inject constructor(@ApplicationContext context: Context) : NavBarEntry {
    override val route = "tab_settings"
    override val order = 1
    override val label: String = context.getString(R.string.nav_settings)
    override val selectedIcon: ImageVector = Icons.Rounded.Settings
    override val unselectedIcon: ImageVector = Icons.Outlined.Settings

    @Composable
    override fun Content(entry: NavBackStackEntry) = SettingsTab()
}
