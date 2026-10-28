package com.briochelab.app.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BDao {
    // ---- Receteler ----
    @Query("SELECT * FROM recipes ORDER BY createdAt DESC, id DESC")
    fun recipes(): Flow<List<Recipe>>

    @Query("SELECT * FROM recipes WHERE id = :id")
    suspend fun getRecipe(id: Long): Recipe?

    @Insert
    suspend fun insertRecipe(r: Recipe): Long

    @Update
    suspend fun updateRecipe(r: Recipe)

    @Delete
    suspend fun deleteRecipe(r: Recipe)

    // ---- Surumler ----
    @Query("SELECT * FROM recipe_versions ORDER BY recipeId, versionNo")
    fun allVersions(): Flow<List<RecipeVersion>>

    @Query("SELECT * FROM recipe_versions WHERE recipeId = :recipeId ORDER BY versionNo")
    fun versionsOf(recipeId: Long): Flow<List<RecipeVersion>>

    @Query("SELECT * FROM recipe_versions WHERE recipeId = :recipeId ORDER BY versionNo")
    suspend fun versionsOfOnce(recipeId: Long): List<RecipeVersion>

    @Query("SELECT * FROM recipe_versions WHERE id = :id")
    suspend fun getVersion(id: Long): RecipeVersion?

    @Query("SELECT * FROM recipe_versions WHERE id = :id")
    fun version(id: Long): Flow<RecipeVersion?>

    @Insert
    suspend fun insertVersion(v: RecipeVersion): Long

    @Update
    suspend fun updateVersion(v: RecipeVersion)

    // ---- Malzemeler ----
    @Query("SELECT * FROM ingredients WHERE versionId = :versionId ORDER BY sortOrder, id")
    fun ingredientsOf(versionId: Long): Flow<List<Ingredient>>

    @Query("SELECT * FROM ingredients WHERE versionId = :versionId ORDER BY sortOrder, id")
    suspend fun ingredientsOfOnce(versionId: Long): List<Ingredient>

    @Query("SELECT * FROM ingredients ORDER BY versionId, sortOrder, id")
    fun allIngredients(): Flow<List<Ingredient>>

    @Insert
    suspend fun insertIngredient(i: Ingredient): Long

    @Update
    suspend fun updateIngredient(i: Ingredient)

    @Delete
    suspend fun deleteIngredient(i: Ingredient)

    @Query("DELETE FROM ingredients WHERE versionId = :versionId")
    suspend fun deleteIngredientsOf(versionId: Long)

    // ---- Su orani tablosu ----
    @Query("SELECT * FROM ingredient_properties")
    fun properties(): Flow<List<IngredientProperty>>

    @Query("SELECT * FROM ingredient_properties")
    suspend fun propertiesOnce(): List<IngredientProperty>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProperty(p: IngredientProperty)

    // ---- Maliyet ----
    @Query("SELECT * FROM cost_records WHERE versionId = :versionId")
    fun costOf(versionId: Long): Flow<CostRecord?>

    @Query("SELECT * FROM cost_records WHERE versionId = :versionId")
    suspend fun costOnce(versionId: Long): CostRecord?

    @Query("SELECT * FROM cost_records")
    fun allCosts(): Flow<List<CostRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCost(c: CostRecord)

    // ---- Deneyler ----
    @Query("SELECT * FROM experiments ORDER BY date DESC, id DESC")
    fun experiments(): Flow<List<Experiment>>

    @Query("SELECT * FROM experiments WHERE id = :id")
    fun experiment(id: Long): Flow<Experiment?>

    @Query("SELECT * FROM experiments WHERE id = :id")
    suspend fun getExperiment(id: Long): Experiment?

    @Query("SELECT * FROM experiments WHERE versionId = :versionId")
    suspend fun experimentsOfVersion(versionId: Long): List<Experiment>

    @Query("SELECT COUNT(*) FROM experiments WHERE versionId = :versionId")
    fun experimentCountForVersion(versionId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM experiments WHERE versionId = :versionId")
    suspend fun experimentCountForVersionOnce(versionId: Long): Int

    @Query("SELECT code FROM experiments WHERE code LIKE :prefix || '%'")
    suspend fun codesWithPrefix(prefix: String): List<String>

    @Insert
    suspend fun insertExperiment(e: Experiment): Long

    @Update
    suspend fun updateExperiment(e: Experiment)

    @Delete
    suspend fun deleteExperiment(e: Experiment)

    // ---- Degisen degiskenler ----
    @Query("SELECT * FROM experiment_variables WHERE experimentId = :expId ORDER BY id")
    fun variablesOf(expId: Long): Flow<List<ExperimentVariable>>

    @Query("SELECT * FROM experiment_variables ORDER BY id")
    fun allVariables(): Flow<List<ExperimentVariable>>

    @Insert
    suspend fun insertVariable(v: ExperimentVariable): Long

    @Delete
    suspend fun deleteVariable(v: ExperimentVariable)

    // ---- Olcumler ----
    @Query("SELECT * FROM process_measurements WHERE experimentId = :expId")
    fun measurementsOf(expId: Long): Flow<List<ProcessMeasurement>>

    @Query("SELECT * FROM process_measurements WHERE experimentId = :expId")
    suspend fun measurementsOfOnce(expId: Long): List<ProcessMeasurement>

    @Query("SELECT * FROM process_measurements")
    fun allMeasurements(): Flow<List<ProcessMeasurement>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMeasurement(m: ProcessMeasurement)

    @Query("DELETE FROM process_measurements WHERE experimentId = :expId AND fieldKey = :key")
    suspend fun deleteMeasurement(expId: Long, key: String)

    // ---- Kontrol listesi ----
    @Query("SELECT * FROM checklist_items WHERE experimentId = :expId ORDER BY stage, sortOrder, id")
    fun checklistOf(expId: Long): Flow<List<ChecklistItem>>

    @Query("SELECT * FROM checklist_items")
    fun allChecklist(): Flow<List<ChecklistItem>>

    @Insert
    suspend fun insertChecklistItems(items: List<ChecklistItem>)

    @Update
    suspend fun updateChecklistItem(i: ChecklistItem)

    // ---- Notlar ----
    @Query("SELECT * FROM experiment_notes WHERE experimentId = :expId ORDER BY createdAt DESC, id DESC")
    fun notesOf(expId: Long): Flow<List<ExperimentNote>>

    @Query("SELECT * FROM experiment_notes")
    fun allNotes(): Flow<List<ExperimentNote>>

    @Insert
    suspend fun insertNote(n: ExperimentNote): Long

    @Update
    suspend fun updateNote(n: ExperimentNote)

    @Delete
    suspend fun deleteNote(n: ExperimentNote)

    // ---- Fotograflar ----
    @Query("SELECT * FROM experiment_photos WHERE experimentId = :expId ORDER BY createdAt, id")
    fun photosOfExperiment(expId: Long): Flow<List<ExperimentPhoto>>

    @Query("SELECT * FROM experiment_photos WHERE versionId = :versionId ORDER BY createdAt, id")
    fun photosOfVersion(versionId: Long): Flow<List<ExperimentPhoto>>

    @Query("SELECT * FROM experiment_photos")
    fun allPhotos(): Flow<List<ExperimentPhoto>>

    @Insert
    suspend fun insertPhoto(p: ExperimentPhoto): Long

    @Delete
    suspend fun deletePhoto(p: ExperimentPhoto)

    // ---- Degerlendirme ----
    @Query("SELECT * FROM sensory_evaluations WHERE experimentId = :expId")
    fun evaluationOf(expId: Long): Flow<SensoryEvaluation?>

    @Query("SELECT * FROM sensory_evaluations")
    fun allEvaluations(): Flow<List<SensoryEvaluation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEvaluation(e: SensoryEvaluation)

    // ---- Oneriler ----
    @Query("SELECT * FROM experiment_recommendations WHERE experimentId = :expId")
    fun recsOf(expId: Long): Flow<List<ExperimentRecommendation>>

    @Query("SELECT * FROM experiment_recommendations")
    fun allRecs(): Flow<List<ExperimentRecommendation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRec(r: ExperimentRecommendation)

    // ---- Yedek bilgisi ----
    @Insert
    suspend fun insertBackupMeta(b: BackupMetadata): Long

    @Query("SELECT * FROM backup_metadata ORDER BY createdAt DESC")
    fun backups(): Flow<List<BackupMetadata>>

    // ---- Yedekleme: tum tablolari bir kerede okuma / yazma ----
    @Query("SELECT * FROM recipes") suspend fun dumpRecipes(): List<Recipe>
    @Query("SELECT * FROM recipe_versions") suspend fun dumpVersions(): List<RecipeVersion>
    @Query("SELECT * FROM ingredients") suspend fun dumpIngredients(): List<Ingredient>
    @Query("SELECT * FROM ingredient_properties") suspend fun dumpProperties(): List<IngredientProperty>
    @Query("SELECT * FROM cost_records") suspend fun dumpCosts(): List<CostRecord>
    @Query("SELECT * FROM experiments") suspend fun dumpExperiments(): List<Experiment>
    @Query("SELECT * FROM experiment_variables") suspend fun dumpVariables(): List<ExperimentVariable>
    @Query("SELECT * FROM process_measurements") suspend fun dumpMeasurements(): List<ProcessMeasurement>
    @Query("SELECT * FROM checklist_items") suspend fun dumpChecklist(): List<ChecklistItem>
    @Query("SELECT * FROM experiment_notes") suspend fun dumpNotes(): List<ExperimentNote>
    @Query("SELECT * FROM experiment_photos") suspend fun dumpPhotos(): List<ExperimentPhoto>
    @Query("SELECT * FROM sensory_evaluations") suspend fun dumpEvaluations(): List<SensoryEvaluation>
    @Query("SELECT * FROM experiment_recommendations") suspend fun dumpRecs(): List<ExperimentRecommendation>

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putRecipes(l: List<Recipe>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putVersions(l: List<RecipeVersion>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putIngredients(l: List<Ingredient>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putProperties(l: List<IngredientProperty>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putCosts(l: List<CostRecord>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putExperiments(l: List<Experiment>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putVariables(l: List<ExperimentVariable>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putMeasurements(l: List<ProcessMeasurement>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putChecklist(l: List<ChecklistItem>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putNotes(l: List<ExperimentNote>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putPhotos(l: List<ExperimentPhoto>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putEvaluations(l: List<SensoryEvaluation>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putRecs(l: List<ExperimentRecommendation>)

    @Query("DELETE FROM recipes") suspend fun clearRecipes()
    @Query("DELETE FROM experiments") suspend fun clearExperiments()
    @Query("DELETE FROM experiment_photos") suspend fun clearPhotos()
    @Query("DELETE FROM ingredient_properties") suspend fun clearProperties()
}

@Database(
    entities = [
        Recipe::class, RecipeVersion::class, Ingredient::class, IngredientProperty::class, CostRecord::class,
        Experiment::class, ExperimentVariable::class, ProcessMeasurement::class, ChecklistItem::class,
        ExperimentNote::class, ExperimentPhoto::class, SensoryEvaluation::class, ExperimentRecommendation::class,
        BackupMetadata::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDb : RoomDatabase() {
    abstract fun dao(): BDao

    companion object {
        @Volatile
        private var inst: AppDb? = null

        fun get(ctx: Context): AppDb {
            val cur = inst
            if (cur != null) return cur
            return synchronized(this) {
                val again = inst
                if (again != null) again
                else {
                    val created = Room.databaseBuilder(ctx.applicationContext, AppDb::class.java, "briochelab.db").build()
                    inst = created
                    created
                }
            }
        }
    }
}
