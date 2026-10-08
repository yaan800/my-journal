package com.yeasin.myjournal.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.yeasin.myjournal.ui.editor.EditorScreen
import com.yeasin.myjournal.ui.home.HomeScreen
import com.yeasin.myjournal.ui.search.SearchScreen

const val DELETED_KEY = "deletedNoteId"

@Composable
fun AppNav() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = "home") {
        composable("home") { entry ->
            HomeScreen(
                openNote = { id -> nav.navigate("editor/$id") },
                openSearch = { nav.navigate("search") },
                deletedId = entry.savedStateHandle.getStateFlow<String?>(DELETED_KEY, null),
                clearDeleted = { entry.savedStateHandle[DELETED_KEY] = null },
            )
        }
        composable(
            "editor/{id}",
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
        ) { entry ->
            val id = requireNotNull(entry.arguments?.getString("id"))
            EditorScreen(
                noteId = id,
                onClose = { nav.popBackStack() },
                onDeleted = {
                    nav.previousBackStackEntry?.savedStateHandle?.set(DELETED_KEY, id)
                    nav.popBackStack()
                },
            )
        }
        composable("search") {
            SearchScreen(
                onBack = { nav.popBackStack() },
                openNote = { id -> nav.navigate("editor/$id") },
            )
        }
    }
}

/** Shortcut to the app container from inside Compose. */
@Composable
fun appContainer(): com.yeasin.myjournal.AppContainer =
    (androidx.compose.ui.platform.LocalContext.current.applicationContext as com.yeasin.myjournal.MyJournalApp).container
