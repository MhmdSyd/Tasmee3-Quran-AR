package com.tasmee3.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tasmee3.app.mushaf.MushafRepository
import com.tasmee3.app.ui.modeltest.ModelTestScreen
import com.tasmee3.app.ui.reader.ReaderScreen
import com.tasmee3.app.ui.recitation.RecitationScreen
import com.tasmee3.app.ui.settings.SettingsScreen
import com.tasmee3.app.ui.summary.SummaryScreen

/** Flow: Reader -> Recitation -> Summary -> (Reader | Recitation). Settings opens from the recitation screen. */
@Composable
fun TasmeeNavHost(modifier: Modifier = Modifier, startPage: Int = 1) {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.READER, modifier = modifier) {
        composable(Routes.READER) {
            ReaderScreen(
                initialPage = startPage,
                onStartRecitation = { page -> nav.navigate(Routes.recitation(page)) },
                onOpenMenu = { nav.navigate(Routes.SETTINGS) }
            )
        }
        composable(
            Routes.RECITATION,
            arguments = listOf(navArgument("page") { type = NavType.IntType })
        ) {
            RecitationScreen(
                onBack = { nav.popBackStack() },
                onSettings = { nav.navigate(Routes.SETTINGS) },
                onFinished = {
                    nav.navigate(Routes.SUMMARY) { popUpTo(Routes.READER) }
                }
            )
        }
        composable(Routes.SUMMARY) {
            SummaryScreen(
                onRepeat = { page -> nav.navigate(Routes.recitation(page)) { popUpTo(Routes.READER) } },
                onNext = { page ->
                    nav.navigate(Routes.recitation(page.coerceAtMost(MushafRepository.PAGE_COUNT))) { popUpTo(Routes.READER) }
                },
                onClose = { nav.popBackStack(Routes.READER, inclusive = false) }
            )
        }
        composable(Routes.MODEL_TEST) {
            ModelTestScreen(onBack = { nav.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { nav.popBackStack() },
                onOpenModelTest = { nav.navigate(Routes.MODEL_TEST) }
            )
        }
    }
}
