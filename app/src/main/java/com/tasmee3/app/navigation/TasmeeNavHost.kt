package com.tasmee3.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tasmee3.app.ui.modeltest.ModelTestScreen
import com.tasmee3.app.ui.reader.ReaderScreen
import com.tasmee3.app.ui.screens.PlaceholderScreen
import com.tasmee3.app.R
import androidx.compose.ui.res.stringResource

/** Flow: Reader -> Listening -> Progress -> Summary -> (Reader | Listening). Settings from recitation screens. */
@Composable
fun TasmeeNavHost(modifier: Modifier = Modifier, startPage: Int = 1) {
    val nav = rememberNavController()
    val settings = stringResource(R.string.settings)
    val finish = stringResource(R.string.finish)
    val repeat = stringResource(R.string.repeat_recitation)
    val back = stringResource(R.string.back)

    NavHost(navController = nav, startDestination = Routes.READER, modifier = modifier) {
        composable(Routes.READER) {
            ReaderScreen(
                initialPage = startPage,
                onStartRecitation = { nav.navigate(Routes.LISTENING) },
                onOpenMenu = { nav.navigate(Routes.MODEL_TEST) }
            )
        }
        composable(Routes.LISTENING) {
            PlaceholderScreen(
                title = stringResource(R.string.listening_title),
                actions = listOf(
                    stringResource(R.string.hint) to { nav.navigate(Routes.PROGRESS) },
                    settings to { nav.navigate(Routes.SETTINGS) },
                    finish to { nav.navigate(Routes.SUMMARY) }
                )
            )
        }
        composable(Routes.PROGRESS) {
            PlaceholderScreen(
                title = stringResource(R.string.progress_title),
                actions = listOf(
                    settings to { nav.navigate(Routes.SETTINGS) },
                    finish to { nav.navigate(Routes.SUMMARY) }
                )
            )
        }
        composable(Routes.SUMMARY) {
            PlaceholderScreen(
                title = stringResource(R.string.summary_title),
                actions = listOf(
                    repeat to { nav.navigate(Routes.LISTENING) { popUpTo(Routes.READER) } },
                    stringResource(R.string.next_page) to {
                        nav.navigate(Routes.READER) { popUpTo(Routes.READER) { inclusive = true } }
                    }
                )
            )
        }
        composable(Routes.MODEL_TEST) {
            ModelTestScreen(onBack = { nav.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            PlaceholderScreen(
                title = stringResource(R.string.settings_title),
                actions = listOf(back to { nav.popBackStack() })
            )
        }
    }
}
