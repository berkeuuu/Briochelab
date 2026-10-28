@file:OptIn(ExperimentalMaterial3Api::class)

package com.briochelab.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.briochelab.app.data.ExperimentVariable
import com.briochelab.app.data.Repo

/** Yeni deneme formunu onceden doldurmak icin (sonraki deney planlayicisi). */
data class ExpDraft(
    val name: String = "",
    val purpose: String = "",
    val problem: String = "",
    val hypothesis: String = "",
    val expected: String = "",
    val successCriteria: String = "",
    val versionId: Long? = null,
    val previousId: Long? = null,
    val variables: List<ExperimentVariable> = emptyList(),
    val planned: Boolean = true
)

sealed class Screen {
    object Home : Screen()
    object Recipes : Screen()
    data class RecipeDetail(val recipeId: Long) : Screen()
    data class VersionEdit(val versionId: Long) : Screen()
    data class NewExperiment(val draft: ExpDraft? = null) : Screen()
    object History : Screen()
    data class ExperimentDetail(val id: Long, val tab: Int = 0) : Screen()
    object ChecklistPicker : Screen()
    data class Compare(val ids: List<Long> = emptyList()) : Screen()
    object Info : Screen()
    object Settings : Screen()
}

class Nav {
    val stack = mutableStateListOf<Screen>(Screen.Home)
    val current: Screen get() = stack.last()
    fun push(s: Screen) {
        stack.add(s)
    }

    fun pop() {
        if (stack.size > 1) stack.removeAt(stack.size - 1)
    }

    fun replace(s: Screen) {
        if (stack.size > 1) stack.removeAt(stack.size - 1)
        stack.add(s)
    }

    fun home() {
        while (stack.size > 1) stack.removeAt(stack.size - 1)
    }
}

val LocalRepo = staticCompositionLocalOf<Repo> { error("Repo yok") }
val LocalNav = staticCompositionLocalOf<Nav> { error("Nav yok") }

private fun titleOf(s: Screen): String = when (s) {
    is Screen.Home -> "BriocheLab"
    is Screen.Recipes -> "Reçetelerim"
    is Screen.RecipeDetail -> "Reçete"
    is Screen.VersionEdit -> "Reçete sürümü"
    is Screen.NewExperiment -> "Yeni Deneme"
    is Screen.History -> "Deney Geçmişi"
    is Screen.ExperimentDetail -> "Deney"
    is Screen.ChecklistPicker -> "Deney Kontrol Listesi"
    is Screen.Compare -> "Karşılaştırma"
    is Screen.Info -> "Bilgi Merkezi"
    is Screen.Settings -> "Yedekleme ve Ayarlar"
}

@Composable
fun AppRoot(repo: Repo) {
    val nav = remember { Nav() }
    BackHandler(enabled = nav.stack.size > 1) { nav.pop() }
    BriocheTheme {
        CompositionLocalProvider(LocalRepo provides repo, LocalNav provides nav) {
            val screen = nav.current
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(titleOf(screen)) },
                        navigationIcon = {
                            if (nav.stack.size > 1) {
                                IconButton(onClick = { nav.pop() }) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = "Geri", tint = Color.White)
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Brown,
                            titleContentColor = Color.White
                        )
                    )
                },
                containerColor = MaterialTheme.colorScheme.background
            ) { pad ->
                Box(Modifier.padding(pad).fillMaxSize()) {
                    when (screen) {
                        is Screen.Home -> HomeScreen()
                        is Screen.Recipes -> RecipesScreen()
                        is Screen.RecipeDetail -> RecipeDetailScreen(screen.recipeId)
                        is Screen.VersionEdit -> VersionEditScreen(screen.versionId)
                        is Screen.NewExperiment -> NewExperimentScreen(screen.draft)
                        is Screen.History -> HistoryScreen()
                        is Screen.ExperimentDetail -> ExperimentDetailScreen(screen.id, screen.tab)
                        is Screen.ChecklistPicker -> ChecklistPickerScreen()
                        is Screen.Compare -> CompareScreen(screen.ids)
                        is Screen.Info -> InfoScreen()
                        is Screen.Settings -> SettingsScreen()
                    }
                }
            }
        }
    }
}
