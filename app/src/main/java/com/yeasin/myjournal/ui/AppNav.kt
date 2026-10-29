package com.yeasin.myjournal.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.yeasin.myjournal.AppContainer
import com.yeasin.myjournal.MyJournalApp
import com.yeasin.myjournal.ui.browse.FoldersScreen
import com.yeasin.myjournal.ui.browse.MoreScreen
import com.yeasin.myjournal.ui.browse.ProjectsScreen
import com.yeasin.myjournal.ui.browse.TagsScreen
import com.yeasin.myjournal.ui.components.Tabs
import com.yeasin.myjournal.ui.editor.EditorScreen
import com.yeasin.myjournal.ui.home.HomeScreen
import com.yeasin.myjournal.ui.lists.ListKind
import com.yeasin.myjournal.ui.lists.NoteListScreen
import com.yeasin.myjournal.ui.search.SearchScreen
import com.yeasin.myjournal.ui.settings.SettingsScreen
import com.yeasin.myjournal.ui.timeline.TimelineScreen

/** Shortcut to the app container from inside Compose. */
@Composable
fun appContainer(): AppContainer = (LocalContext.current.applicationContext as MyJournalApp).container

/** Switch bottom-bar tabs without stacking them up; Back from any tab returns to Notes. */
private fun NavHostController.goTab(route: String) = navigate(route) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

@Composable
fun AppNav() {
    val nav = rememberNavController()
    val catalog by appContainer().catalog.collectAsStateWithLifecycle()
    val open = { id: String -> nav.navigate("editor/$id") }
    val back: () -> Unit = { nav.popBackStack() }
    val idArg = listOf(navArgument("id") { type = NavType.StringType })

    CompositionLocalProvider(LocalCatalog provides catalog) {
        NavHost(navController = nav, startDestination = Tabs.NOTES) {
            // ----- tabs -----
            composable(Tabs.NOTES) {
                HomeScreen(
                    openNote = open,
                    openSearch = { nav.navigate("search") },
                    openTimeline = { nav.navigate("timeline") },
                    navigateTab = nav::goTab,
                )
            }
            composable(Tabs.PROJECTS) { ProjectsScreen(openProject = { nav.navigate("project/$it") }, navigateTab = nav::goTab) }
            composable(Tabs.TAGS) { TagsScreen(openTag = { nav.navigate("tag/$it") }, navigateTab = nav::goTab) }
            composable(Tabs.MORE) {
                MoreScreen(
                    navigateTab = nav::goTab,
                    openFolders = { nav.navigate("folders") },
                    openTimeline = { nav.navigate("timeline") },
                    openArchive = { nav.navigate("archive") },
                    openTrash = { nav.navigate("trash") },
                    openSettings = { nav.navigate("settings") },
                )
            }

            // ----- lists -----
            composable("project/{id}", idArg) { NoteListScreen(ListKind.PROJECT, it.arguments?.getString("id"), back, open) }
            composable("tag/{id}", idArg) { NoteListScreen(ListKind.TAG, it.arguments?.getString("id"), back, open) }
            composable("folder/{id}", idArg) {
                NoteListScreen(ListKind.FOLDER, it.arguments?.getString("id"), back, open, openFolder = { f -> nav.navigate("folder/$f") })
            }
            composable("archive") { NoteListScreen(ListKind.ARCHIVE, null, back, open) }
            composable("trash") { NoteListScreen(ListKind.TRASH, null, back, open) }
            composable("folders") { FoldersScreen(onBack = back, openFolder = { nav.navigate("folder/$it") }) }
            composable("timeline") { TimelineScreen(onBack = back, openNote = open) }
            composable("settings") { SettingsScreen(onBack = back) }
            composable("search") { SearchScreen(onBack = back, openNote = open) }

            // ----- editor -----
            composable("editor/{id}", idArg) { entry ->
                EditorScreen(noteId = requireNotNull(entry.arguments?.getString("id")), onClose = back)
            }
        }
    }
}
