package com.briochelab.app.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable fun HomeScreen() { Text("Ana ekran (geçici)") }
@Composable fun RecipesScreen() { Text("Reçeteler (geçici)") }
@Composable fun RecipeDetailScreen(recipeId: Long) { Text("Reçete $recipeId (geçici)") }
@Composable fun VersionEditScreen(versionId: Long) { Text("Sürüm $versionId (geçici)") }
@Composable fun NewExperimentScreen(draft: ExpDraft?) { Text("Yeni deneme (geçici)") }
@Composable fun HistoryScreen() { Text("Geçmiş (geçici)") }
@Composable fun ExperimentDetailScreen(id: Long, tab: Int) { Text("Deney $id (geçici)") }
@Composable fun ChecklistPickerScreen() { Text("Kontrol listesi (geçici)") }
@Composable fun CompareScreen(ids: List<Long>) { Text("Karşılaştırma (geçici)") }
@Composable fun InfoScreen() { Text("Bilgi (geçici)") }
@Composable fun SettingsScreen() { Text("Ayarlar (geçici)") }
