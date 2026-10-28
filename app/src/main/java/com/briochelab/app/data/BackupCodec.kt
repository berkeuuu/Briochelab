package com.briochelab.app.data

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class BackupException(message: String) : Exception(message)

data class BackupData(
    val recipes: List<Recipe> = emptyList(),
    val versions: List<RecipeVersion> = emptyList(),
    val ingredients: List<Ingredient> = emptyList(),
    val properties: List<IngredientProperty> = emptyList(),
    val costs: List<CostRecord> = emptyList(),
    val experiments: List<Experiment> = emptyList(),
    val variables: List<ExperimentVariable> = emptyList(),
    val measurements: List<ProcessMeasurement> = emptyList(),
    val checklist: List<ChecklistItem> = emptyList(),
    val notes: List<ExperimentNote> = emptyList(),
    val photos: List<ExperimentPhoto> = emptyList(),
    val evaluations: List<SensoryEvaluation> = emptyList(),
    val recs: List<ExperimentRecommendation> = emptyList()
) {
    fun itemCount(): Int = recipes.size + versions.size + ingredients.size + properties.size + costs.size +
        experiments.size + variables.size + measurements.size + checklist.size + notes.size +
        photos.size + evaluations.size + recs.size
}

object BackupCodec {
    const val SCHEMA = 1
    const val APP = "BriocheLab"

    private fun JSONObject.p(k: String, v: Any?): JSONObject {
        if (v != null) put(k, v)
        return this
    }

    private fun JSONObject.dn(k: String): Double? = if (has(k) && !isNull(k)) getDouble(k) else null
    private fun JSONObject.ln(k: String): Long? = if (has(k) && !isNull(k)) getLong(k) else null
    private fun JSONObject.inn(k: String): Int? = if (has(k) && !isNull(k)) getInt(k) else null
    private fun JSONObject.s(k: String): String = if (has(k) && !isNull(k)) getString(k) else ""

    private fun <T> arr(items: List<T>, f: (T) -> JSONObject): JSONArray {
        val a = JSONArray()
        for (x in items) a.put(f(x))
        return a
    }

    private fun <T> JSONObject.list(name: String, f: (JSONObject) -> T): List<T> {
        val a = optJSONArray(name) ?: return emptyList()
        val out = ArrayList<T>(a.length())
        for (i in 0 until a.length()) out.add(f(a.getJSONObject(i)))
        return out
    }

    fun encode(d: BackupData, createdAt: Long, appVersion: String): String {
        val root = JSONObject()
        root.put("app", APP)
        root.put("schema", SCHEMA)
        root.put("appVersion", appVersion)
        root.put("createdAt", createdAt)
        root.put("recipes", arr(d.recipes) {
            JSONObject().p("id", it.id).p("code", it.code).p("name", it.name).p("description", it.description)
                .p("goal", it.goal).p("notes", it.notes).p("createdAt", it.createdAt)
        })
        root.put("versions", arr(d.versions) {
            JSONObject().p("id", it.id).p("recipeId", it.recipeId).p("versionNo", it.versionNo)
                .p("parentVersionId", it.parentVersionId).p("createdAt", it.createdAt).p("note", it.note)
                .p("flourBrand", it.flourBrand).p("flourType", it.flourType).p("flourProtein", it.flourProtein)
                .p("flourSpecs", it.flourSpecs).p("yeastType", it.yeastType).p("yeastBrand", it.yeastBrand)
                .p("mixMethod", it.mixMethod).p("fermentMethod", it.fermentMethod).p("bakeMethod", it.bakeMethod)
                .p("expectedDoughG", it.expectedDoughG).p("targetPieceG", it.targetPieceG).p("lossPct", it.lossPct)
        })
        root.put("ingredients", arr(d.ingredients) {
            JSONObject().p("id", it.id).p("versionId", it.versionId).p("name", it.name).p("category", it.category)
                .p("grams", it.grams).p("waterPercent", it.waterPercent).p("price", it.price)
                .p("priceUnit", it.priceUnit).p("unitGrams", it.unitGrams).p("sortOrder", it.sortOrder)
        })
        root.put("properties", arr(d.properties) {
            JSONObject().p("category", it.category).p("waterPercent", it.waterPercent)
        })
        root.put("costs", arr(d.costs) {
            JSONObject().p("id", it.id).p("versionId", it.versionId).p("packagingPerPiece", it.packagingPerPiece)
                .p("energyTotal", it.energyTotal).p("laborTotal", it.laborTotal).p("otherTotal", it.otherTotal)
                .p("salePrice", it.salePrice)
        })
        root.put("experiments", arr(d.experiments) {
            JSONObject().p("id", it.id).p("code", it.code).p("name", it.name).p("date", it.date)
                .p("versionId", it.versionId).p("previousExperimentId", it.previousExperimentId)
                .p("purpose", it.purpose).p("problem", it.problem).p("hypothesis", it.hypothesis)
                .p("expected", it.expected).p("successCriteria", it.successCriteria).p("priority", it.priority)
                .p("notes", it.notes).p("status", it.status).p("createdAt", it.createdAt)
                .p("completedAt", it.completedAt).p("tags", it.tags)
        })
        root.put("variables", arr(d.variables) {
            JSONObject().p("id", it.id).p("experimentId", it.experimentId).p("label", it.label)
                .p("oldValue", it.oldValue).p("newValue", it.newValue)
        })
        root.put("measurements", arr(d.measurements) {
            JSONObject().p("id", it.id).p("experimentId", it.experimentId).p("fieldKey", it.fieldKey)
                .p("numValue", it.numValue).p("textValue", it.textValue).p("measured", it.measured)
        })
        root.put("checklist", arr(d.checklist) {
            JSONObject().p("id", it.id).p("experimentId", it.experimentId).p("stage", it.stage).p("text", it.text)
                .p("status", it.status).p("comment", it.comment).p("sortOrder", it.sortOrder)
        })
        root.put("notes", arr(d.notes) {
            JSONObject().p("id", it.id).p("experimentId", it.experimentId).p("stage", it.stage).p("title", it.title)
                .p("tags", it.tags).p("body", it.body).p("createdAt", it.createdAt).p("updatedAt", it.updatedAt)
        })
        root.put("photos", arr(d.photos) {
            JSONObject().p("id", it.id).p("experimentId", it.experimentId).p("versionId", it.versionId)
                .p("slot", it.slot).p("fileName", it.fileName).p("caption", it.caption).p("createdAt", it.createdAt)
        })
        root.put("evaluations", arr(d.evaluations) {
            JSONObject().p("id", it.id).p("experimentId", it.experimentId).p("softness", it.softness)
                .p("crumb", it.crumb).p("aroma", it.aroma).p("volume", it.volume).p("height", it.height)
                .p("roundness", it.roundness).p("color", it.color).p("slicing", it.slicing).p("sauce", it.sauce)
                .p("nextDaySoft", it.nextDaySoft).p("overall", it.overall).p("day1Soft", it.day1Soft)
                .p("goalMet", it.goalMet).p("freeText", it.freeText).p("failures", it.failures)
                .p("nextSuggestion", it.nextSuggestion).p("updatedAt", it.updatedAt)
        })
        root.put("recs", arr(d.recs) {
            JSONObject().p("id", it.id).p("experimentId", it.experimentId).p("ruleKey", it.ruleKey)
                .p("status", it.status).p("userPlan", it.userPlan).p("updatedAt", it.updatedAt)
        })
        return root.toString()
    }

    /** Yedek metnini okur ve dogrular. Gecersiz veya uyumsuz ise BackupException firlatir. */
    fun decode(text: String): BackupData {
        val root = try {
            JSONObject(text)
        } catch (e: JSONException) {
            throw BackupException("Dosya geçerli bir yedek dosyası değil (bozuk veya yanlış biçim).")
        }
        if (root.optString("app") != APP) {
            throw BackupException("Bu dosya BriocheLab yedeği değil.")
        }
        val schema = root.optInt("schema", -1)
        if (schema < 1) throw BackupException("Yedek sürümü okunamadı.")
        if (schema > SCHEMA) throw BackupException("Bu yedek daha yeni bir uygulama sürümünden. Önce uygulamayı güncelleyin.")
        try {
            return BackupData(
                recipes = root.list("recipes") {
                    Recipe(it.getLong("id"), it.s("code"), it.s("name"), it.s("description"), it.s("goal"), it.s("notes"), it.getLong("createdAt"))
                },
                versions = root.list("versions") {
                    RecipeVersion(
                        it.getLong("id"), it.getLong("recipeId"), it.getInt("versionNo"), it.ln("parentVersionId"),
                        it.getLong("createdAt"), it.s("note"), it.s("flourBrand"), it.s("flourType"), it.dn("flourProtein"),
                        it.s("flourSpecs"), it.s("yeastType"), it.s("yeastBrand"), it.s("mixMethod"), it.s("fermentMethod"),
                        it.s("bakeMethod"), it.dn("expectedDoughG"), it.dn("targetPieceG"), it.dn("lossPct")
                    )
                },
                ingredients = root.list("ingredients") {
                    Ingredient(
                        it.getLong("id"), it.getLong("versionId"), it.s("name"), it.s("category"), it.getDouble("grams"),
                        it.dn("waterPercent"), it.dn("price"), it.optString("priceUnit", "KG"), it.dn("unitGrams"), it.optInt("sortOrder", 0)
                    )
                },
                properties = root.list("properties") { IngredientProperty(it.s("category"), it.dn("waterPercent")) },
                costs = root.list("costs") {
                    CostRecord(
                        it.getLong("id"), it.getLong("versionId"), it.dn("packagingPerPiece"), it.dn("energyTotal"),
                        it.dn("laborTotal"), it.dn("otherTotal"), it.dn("salePrice")
                    )
                },
                experiments = root.list("experiments") {
                    Experiment(
                        it.getLong("id"), it.s("code"), it.s("name"), it.getLong("date"), it.ln("versionId"),
                        it.ln("previousExperimentId"), it.s("purpose"), it.s("problem"), it.s("hypothesis"), it.s("expected"),
                        it.s("successCriteria"), it.optInt("priority", 1), it.s("notes"), it.optString("status", "ACTIVE"),
                        it.getLong("createdAt"), it.ln("completedAt"), it.s("tags")
                    )
                },
                variables = root.list("variables") {
                    ExperimentVariable(it.getLong("id"), it.getLong("experimentId"), it.s("label"), it.s("oldValue"), it.s("newValue"))
                },
                measurements = root.list("measurements") {
                    ProcessMeasurement(
                        it.getLong("id"), it.getLong("experimentId"), it.s("fieldKey"), it.dn("numValue"),
                        it.s("textValue"), it.optBoolean("measured", true)
                    )
                },
                checklist = root.list("checklist") {
                    ChecklistItem(
                        it.getLong("id"), it.getLong("experimentId"), it.getInt("stage"), it.s("text"),
                        it.optInt("status", 0), it.s("comment"), it.optInt("sortOrder", 0)
                    )
                },
                notes = root.list("notes") {
                    ExperimentNote(
                        it.getLong("id"), it.getLong("experimentId"), it.optInt("stage", 0), it.s("title"), it.s("tags"),
                        it.s("body"), it.getLong("createdAt"), it.ln("updatedAt")
                    )
                },
                photos = root.list("photos") {
                    ExperimentPhoto(
                        it.getLong("id"), it.ln("experimentId"), it.ln("versionId"), it.s("slot"), it.s("fileName"),
                        it.s("caption"), it.getLong("createdAt")
                    )
                },
                evaluations = root.list("evaluations") {
                    SensoryEvaluation(
                        it.getLong("id"), it.getLong("experimentId"), it.inn("softness"), it.inn("crumb"), it.inn("aroma"),
                        it.inn("volume"), it.inn("height"), it.inn("roundness"), it.inn("color"), it.inn("slicing"),
                        it.inn("sauce"), it.inn("nextDaySoft"), it.inn("overall"), it.inn("day1Soft"), it.inn("goalMet"),
                        it.s("freeText"), it.s("failures"), it.s("nextSuggestion"), it.getLong("updatedAt")
                    )
                },
                recs = root.list("recs") {
                    ExperimentRecommendation(
                        it.getLong("id"), it.getLong("experimentId"), it.s("ruleKey"), it.optInt("status", 0),
                        it.s("userPlan"), it.getLong("updatedAt")
                    )
                }
            )
        } catch (e: JSONException) {
            throw BackupException("Yedek dosyasındaki kayıtlar okunamadı: " + (e.message ?: "bilinmeyen hata"))
        }
    }

    /** Yedegin kullaniciya gosterilecek ozeti (iceri aktarmadan once). */
    fun summaryLines(d: BackupData): List<String> = listOf(
        "Reçete: " + d.recipes.size,
        "Reçete sürümü: " + d.versions.size,
        "Malzeme satırı: " + d.ingredients.size,
        "Deney: " + d.experiments.size,
        "Ölçüm: " + d.measurements.size,
        "Kontrol listesi maddesi: " + d.checklist.size,
        "Not: " + d.notes.size,
        "Fotoğraf kaydı: " + d.photos.size,
        "Değerlendirme: " + d.evaluations.size
    )
}
