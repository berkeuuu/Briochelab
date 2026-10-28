package com.briochelab.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "recipes")
data class Recipe(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val code: String,
    val name: String,
    val description: String = "",
    val goal: String = "",
    val notes: String = "",
    val createdAt: Long
)

@Entity(
    tableName = "recipe_versions",
    foreignKeys = [ForeignKey(entity = Recipe::class, parentColumns = ["id"], childColumns = ["recipeId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("recipeId")]
)
data class RecipeVersion(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recipeId: Long,
    val versionNo: Int,
    val parentVersionId: Long? = null,
    val createdAt: Long,
    val note: String = "",
    val flourBrand: String = "",
    val flourType: String = "",
    val flourProtein: Double? = null,
    val flourSpecs: String = "",
    val yeastType: String = "",
    val yeastBrand: String = "",
    val mixMethod: String = "",
    val fermentMethod: String = "",
    val bakeMethod: String = "",
    val expectedDoughG: Double? = null,
    val targetPieceG: Double? = null,
    val lossPct: Double? = null
)

@Entity(
    tableName = "ingredients",
    foreignKeys = [ForeignKey(entity = RecipeVersion::class, parentColumns = ["id"], childColumns = ["versionId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("versionId")]
)
data class Ingredient(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val versionId: Long,
    val name: String,
    val category: String,
    val grams: Double,
    val waterPercent: Double? = null,
    val price: Double? = null,
    val priceUnit: String = "KG",
    val unitGrams: Double? = null,
    val sortOrder: Int = 0
)

/** Kategori bazli, kullanici tarafindan duzenlenebilir su orani tablosu. */
@Entity(tableName = "ingredient_properties")
data class IngredientProperty(
    @PrimaryKey val category: String,
    val waterPercent: Double?
)

@Entity(
    tableName = "cost_records",
    foreignKeys = [ForeignKey(entity = RecipeVersion::class, parentColumns = ["id"], childColumns = ["versionId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["versionId"], unique = true)]
)
data class CostRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val versionId: Long,
    val packagingPerPiece: Double? = null,
    val energyTotal: Double? = null,
    val laborTotal: Double? = null,
    val otherTotal: Double? = null,
    val salePrice: Double? = null
)

@Entity(
    tableName = "experiments",
    indices = [Index(value = ["code"], unique = true), Index("versionId")]
)
data class Experiment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val code: String,
    val name: String,
    val date: Long,
    val versionId: Long? = null,
    val previousExperimentId: Long? = null,
    val purpose: String = "",
    val problem: String = "",
    val hypothesis: String = "",
    val expected: String = "",
    val successCriteria: String = "",
    val priority: Int = 1,
    val notes: String = "",
    val status: String = "ACTIVE",
    val createdAt: Long,
    val completedAt: Long? = null,
    val tags: String = ""
)

@Entity(
    tableName = "experiment_variables",
    foreignKeys = [ForeignKey(entity = Experiment::class, parentColumns = ["id"], childColumns = ["experimentId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("experimentId")]
)
data class ExperimentVariable(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val experimentId: Long,
    val label: String,
    val oldValue: String = "",
    val newValue: String = ""
)

@Entity(
    tableName = "process_measurements",
    foreignKeys = [ForeignKey(entity = Experiment::class, parentColumns = ["id"], childColumns = ["experimentId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["experimentId", "fieldKey"], unique = true)]
)
data class ProcessMeasurement(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val experimentId: Long,
    val fieldKey: String,
    val numValue: Double? = null,
    val textValue: String = "",
    /** true = olculdu, false = tahmin / plan. */
    val measured: Boolean = true
)

@Entity(
    tableName = "checklist_items",
    foreignKeys = [ForeignKey(entity = Experiment::class, parentColumns = ["id"], childColumns = ["experimentId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("experimentId")]
)
data class ChecklistItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val experimentId: Long,
    val stage: Int,
    val text: String,
    /** 0 bekliyor, 1 tamamlandi, 2 atlandi, 3 ilgili degil */
    val status: Int = 0,
    val comment: String = "",
    val sortOrder: Int = 0
)

@Entity(
    tableName = "experiment_notes",
    foreignKeys = [ForeignKey(entity = Experiment::class, parentColumns = ["id"], childColumns = ["experimentId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("experimentId")]
)
data class ExperimentNote(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val experimentId: Long,
    val stage: Int = 0,
    val title: String = "",
    val tags: String = "",
    val body: String,
    val createdAt: Long,
    val updatedAt: Long? = null
)

/** Fotograf deneye veya recete surumune baglanir. dosya adi uygulamanin photos klasorune gore tutulur. */
@Entity(tableName = "experiment_photos", indices = [Index("experimentId"), Index("versionId")])
data class ExperimentPhoto(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val experimentId: Long? = null,
    val versionId: Long? = null,
    val slot: String = "",
    val fileName: String,
    val caption: String = "",
    val createdAt: Long
)

@Entity(
    tableName = "sensory_evaluations",
    foreignKeys = [ForeignKey(entity = Experiment::class, parentColumns = ["id"], childColumns = ["experimentId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["experimentId"], unique = true)]
)
data class SensoryEvaluation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val experimentId: Long,
    val softness: Int? = null,
    val crumb: Int? = null,
    val aroma: Int? = null,
    val volume: Int? = null,
    val height: Int? = null,
    val roundness: Int? = null,
    val color: Int? = null,
    val slicing: Int? = null,
    val sauce: Int? = null,
    val nextDaySoft: Int? = null,
    val overall: Int? = null,
    val day1Soft: Int? = null,
    /** null belirsiz, 1 ulasildi, 0 ulasilamadi */
    val goalMet: Int? = null,
    val freeText: String = "",
    val failures: String = "",
    val nextSuggestion: String = "",
    val updatedAt: Long
)

@Entity(
    tableName = "experiment_recommendations",
    foreignKeys = [ForeignKey(entity = Experiment::class, parentColumns = ["id"], childColumns = ["experimentId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["experimentId", "ruleKey"], unique = true)]
)
data class ExperimentRecommendation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val experimentId: Long,
    val ruleKey: String,
    /** 0 yeni, 1 kabul, 2 reddedildi, 3 kendi plani */
    val status: Int = 0,
    val userPlan: String = "",
    val updatedAt: Long
)

@Entity(tableName = "backup_metadata")
data class BackupMetadata(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val createdAt: Long,
    val kind: String,
    val appVersion: String,
    val schemaVersion: Int,
    val itemCount: Int
)
