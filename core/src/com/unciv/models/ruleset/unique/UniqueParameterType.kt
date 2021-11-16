package com.unciv.models.ruleset.unique

import com.unciv.models.ruleset.BeliefType
import com.unciv.models.ruleset.Ruleset
import com.unciv.models.ruleset.tile.ResourceType
import com.unciv.models.ruleset.tile.TerrainType
import com.unciv.models.stats.Stat

// parameterName values should be compliant with autogenerated values in TranslationFileWriter.generateStringsFromJSONs
// Eventually we'll merge the translation generation to take this as the source of that
@Suppress("unused") // Some are used only via enumerating the enum matching on parameterName
enum class UniqueParameterType(val parameterName:String) {
    Number("amount") {
        override fun getErrorSeverity(parameterText: String, ruleset: Ruleset):
                UniqueType.UniqueComplianceErrorSeverity? {
            return if (parameterText.toIntOrNull() == null) UniqueType.UniqueComplianceErrorSeverity.RulesetInvariant
            else null
        }
    },
    // todo potentially remove if OneTimeRevealSpecificMapTiles changes
    KeywordAll("'all'") {
        override fun getErrorSeverity(parameterText: String, ruleset: Ruleset) =
            if (parameterText == "All") null else UniqueType.UniqueComplianceErrorSeverity.RulesetInvariant
    },
    CombatantFilter("combatantFilter") {
        override fun getErrorSeverity(parameterText: String, ruleset: Ruleset):
                UniqueType.UniqueComplianceErrorSeverity? {
            if (parameterText == "City") return null
            return MapUnitFilter.getErrorSeverity(parameterText, ruleset)
        }

    },
    MapUnitFilter("mapUnitFilter") {
        private val knownValues = setOf("Wounded", "Barbarians", "City-State", "Embarked", "Non-City")
        override fun getErrorSeverity(parameterText: String, ruleset: Ruleset):
                UniqueType.UniqueComplianceErrorSeverity? {
            if ('{' in parameterText) // "{filter} {filter}" for and logic
                return parameterText.removePrefix("{").removeSuffix("}").split("} {")
                    .mapNotNull { getErrorSeverity(it, ruleset) }
                    .maxByOrNull { it.ordinal }
            if (parameterText in knownValues) return null
            return BaseUnitFilter.getErrorSeverity(parameterText, ruleset)
        }
    },
    BaseUnitFilter("baseUnitFilter") {
        // As you can see there is a difference between these and what's in unitTypeStrings (for translation) -
        // the goal is to unify, but for now this is the "real" list
        private val knownValues = setOf("All", "Melee", "Ranged", "Civilian", "Military", "Land", "Water", "Air",
            "non-air", "Nuclear Weapon", "Great Person", "Religious")
        override fun getErrorSeverity(parameterText: String, ruleset: Ruleset):
                UniqueType.UniqueComplianceErrorSeverity? {
            if (parameterText in knownValues) return null
            if (ruleset.unitTypes.containsKey(parameterText)) return null
            if (ruleset.units.containsKey(parameterText)) return null

            // We could add a giant hashset of all uniques used by units,
            //  so we could accept that unique-targeting uniques are OK. Maybe later.

            return UniqueType.UniqueComplianceErrorSeverity.WarningOnly
        }
    },
    Stats("stats") {
        override fun getErrorSeverity(parameterText: String, ruleset: Ruleset):
                UniqueType.UniqueComplianceErrorSeverity? {
            if (!com.unciv.models.stats.Stats.isStats(parameterText))
                return UniqueType.UniqueComplianceErrorSeverity.RulesetInvariant
            return null
        }
    },
    StatName("stat") {
        override fun getErrorSeverity(parameterText: String, ruleset: Ruleset):
                UniqueType.UniqueComplianceErrorSeverity? {
            if (Stat.values().any { it.name == parameterText }) return null
            return UniqueType.UniqueComplianceErrorSeverity.RulesetInvariant
        }
    },
    CityFilter("cityFilter") {
        override fun getErrorSeverity(parameterText: String, ruleset: Ruleset):
                UniqueType.UniqueComplianceErrorSeverity? {
            if (parameterText !in cityFilterStrings)
                return UniqueType.UniqueComplianceErrorSeverity.RulesetInvariant
            return null
        }
    },
    BuildingName("buildingName") {
        override fun getErrorSeverity(
            parameterText: String,
            ruleset: Ruleset
        ): UniqueType.UniqueComplianceErrorSeverity? {
            if (ruleset.buildings.containsKey(parameterText)) return null
            return UniqueType.UniqueComplianceErrorSeverity.RulesetSpecific
        }
    },
    BuildingFilter("buildingFilter") {
        override fun getErrorSeverity(
            parameterText: String,
            ruleset: Ruleset
        ): UniqueType.UniqueComplianceErrorSeverity? {
            if (parameterText == "All") return null
            if (Stat.values().any { it.name == parameterText }) return null
            if (BuildingName.getErrorSeverity(parameterText, ruleset) == null) return null
            return UniqueType.UniqueComplianceErrorSeverity.WarningOnly
        } 
    },
    // Only used in values deprecated in 3.17.10
        ConstructionFilter("constructionFilter") {
            override fun getErrorSeverity(
                parameterText: String,
                ruleset: Ruleset
            ): UniqueType.UniqueComplianceErrorSeverity? {
                if (BuildingFilter.getErrorSeverity(parameterText, ruleset) == null) return null
                if (BaseUnitFilter.getErrorSeverity(parameterText, ruleset) == null) return null
                return UniqueType.UniqueComplianceErrorSeverity.WarningOnly
            }
        },
    //
    TerrainFilter("terrainFilter") {
        private val knownValues = setOf("All",
            "Coastal", "River", "Open terrain", "Rough terrain", "Water resource",
            "Foreign Land", "Foreign", "Friendly Land", "Friendly", "Enemy Land", "Enemy",
            "Featureless", "Lowland", "Fresh Water", "Dry")
        override fun getErrorSeverity(parameterText: String, ruleset: Ruleset):
                UniqueType.UniqueComplianceErrorSeverity? {
            if (parameterText in knownValues) return null
            if (ruleset.terrains.containsKey(parameterText)) return null
            if (TerrainType.values().any { parameterText == it.name }) return null
            if (ruleset.tileResources.containsKey(parameterText)) return null
            if (ResourceType.values().any { parameterText == it.name + " resource" }) return null
            return UniqueType.UniqueComplianceErrorSeverity.WarningOnly
        }
    },
    TileFilter("tileFilter") {
        private val knownValues = setOf("unimproved", "All Road", "Great Improvement")
        override fun getErrorSeverity(parameterText: String, ruleset: Ruleset):
                UniqueType.UniqueComplianceErrorSeverity? {
            if (parameterText in knownValues) return null
            if (ruleset.tileImprovements.containsKey(parameterText)) return null
            return TerrainFilter.getErrorSeverity(parameterText, ruleset)
        }
    },
    /** Used by NaturalWonderGenerator, only tests base terrain or a feature */
    SimpleTerrain("simpleTerrain") {
        private val knownValues = setOf("Elevated", "Water", "Land")
        override fun getErrorSeverity(parameterText: String, ruleset: Ruleset):
                UniqueType.UniqueComplianceErrorSeverity? {
            if (parameterText in knownValues) return null
            if (ruleset.terrains.containsKey(parameterText)) return null
            return UniqueType.UniqueComplianceErrorSeverity.RulesetSpecific
        }
    },
    /** Used by NaturalWonderGenerator, only tests base terrain */
    BaseTerrain("baseTerrain") {
        override fun getErrorSeverity(parameterText: String, ruleset: Ruleset):
                UniqueType.UniqueComplianceErrorSeverity? {
            if (ruleset.terrains[parameterText]?.type?.isBaseTerrain == true) return null
            return UniqueType.UniqueComplianceErrorSeverity.RulesetSpecific
        }
    },
    /** Used for region definitions, can be a terrain type with region unique, or "Hybrid" */
    RegionType("regionType") {
        private val knownValues = setOf("Hybrid")
        override fun getErrorSeverity(parameterText: String, ruleset: Ruleset):
                UniqueType.UniqueComplianceErrorSeverity? {
            if (parameterText in knownValues) return null
            if (ruleset.terrains[parameterText]?.hasUnique(UniqueType.RegionRequirePercentSingleType) == true ||
                    ruleset.terrains[parameterText]?.hasUnique(UniqueType.RegionRequirePercentTwoTypes) == true)
                return null
            return UniqueType.UniqueComplianceErrorSeverity.RulesetSpecific
        }
    },
    /** Used for start placements */
    TerrainQuality("terrainQuality") {
        private val knownValues = setOf("Undesirable", "Food", "Desirable", "Production")
        override fun getErrorSeverity(parameterText: String, ruleset: Ruleset):
                UniqueType.UniqueComplianceErrorSeverity? {
            if (parameterText in knownValues) return null
            return UniqueType.UniqueComplianceErrorSeverity.RulesetInvariant
        }
    },
    Promotion("promotion") {
        override fun getErrorSeverity(parameterText: String, ruleset: Ruleset):
            UniqueType.UniqueComplianceErrorSeverity? = when (parameterText) {
                in ruleset.unitPromotions -> null
                else -> UniqueType.UniqueComplianceErrorSeverity.RulesetSpecific
            }
    },
    Era("era") {
        override fun getErrorSeverity(parameterText: String, ruleset: Ruleset):
            UniqueType.UniqueComplianceErrorSeverity? = when (parameterText) {
                in ruleset.eras -> null
                else -> UniqueType.UniqueComplianceErrorSeverity.RulesetSpecific
            }
    },
    /** should mirror TileImprovement.matchesFilter exactly */
    ImprovementFilter("improvementFilter") {
        private val knownValues = setOf("All", "All Road", "Great Improvement", "Great")
        override fun getErrorSeverity(parameterText: String, ruleset: Ruleset):
                UniqueType.UniqueComplianceErrorSeverity? {
            if (parameterText in knownValues) return null
            if (ruleset.tileImprovements.containsKey(parameterText)) return null
            return UniqueType.UniqueComplianceErrorSeverity.RulesetSpecific
        }
    },
    Resource("resource") {
        override fun getErrorSeverity(parameterText: String, ruleset: Ruleset):
            UniqueType.UniqueComplianceErrorSeverity? = when (parameterText) {
                in ruleset.tileResources -> null
                else -> UniqueType.UniqueComplianceErrorSeverity.RulesetSpecific
            }
    },
    BeliefTypeName("beliefType") {
        override fun getErrorSeverity(parameterText: String, ruleset: Ruleset):
                UniqueType.UniqueComplianceErrorSeverity? = when (parameterText) {
            in BeliefType.values().map { it.name } -> null
            else -> UniqueType.UniqueComplianceErrorSeverity.RulesetInvariant
        }
    },
    Belief("belief") {
        override fun getErrorSeverity(parameterText: String, ruleset: Ruleset):
                UniqueType.UniqueComplianceErrorSeverity? = when (parameterText) {
            in ruleset.beliefs -> null
            else -> UniqueType.UniqueComplianceErrorSeverity.RulesetSpecific
        }
    },
    FoundingOrEnhancing("foundingOrEnhancing") {
        private val knownValues = setOf("founding", "enhancing")
        override fun getErrorSeverity(parameterText: String, ruleset: Ruleset):
                UniqueType.UniqueComplianceErrorSeverity? = when (parameterText) {
            in knownValues -> null
            else -> UniqueType.UniqueComplianceErrorSeverity.RulesetInvariant
        }
    },
    Technology("tech") {
        override fun getErrorSeverity(parameterText: String, ruleset: Ruleset):
                UniqueType.UniqueComplianceErrorSeverity? = when (parameterText) {
            in ruleset.technologies -> null
            else -> UniqueType.UniqueComplianceErrorSeverity.RulesetSpecific
        }
    },
    Specialist("specialist") {
        override fun getErrorSeverity(parameterText: String, ruleset: Ruleset):
                UniqueType.UniqueComplianceErrorSeverity? = when (parameterText) {
            in ruleset.specialists -> null
            else -> UniqueType.UniqueComplianceErrorSeverity.RulesetSpecific
        }
    },
    Policy("policy") {
        override fun getErrorSeverity(
            parameterText: String,
            ruleset: Ruleset
        ): UniqueType.UniqueComplianceErrorSeverity? {
            return when (parameterText) {
                in ruleset.policies -> null
                else -> UniqueType.UniqueComplianceErrorSeverity.RulesetSpecific
            }
        }
    },
    /** Behaves like [Unknown], but states explicitly the parameter is OK and its contents are ignored */
    Comment("comment") {
        override fun getErrorSeverity(parameterText: String, ruleset: Ruleset):
                UniqueType.UniqueComplianceErrorSeverity? = null
    },
    Unknown("param") {
        override fun getErrorSeverity(parameterText: String, ruleset: Ruleset):
                UniqueType.UniqueComplianceErrorSeverity? = null
    };

    abstract fun getErrorSeverity(parameterText:String, ruleset: Ruleset): UniqueType.UniqueComplianceErrorSeverity?

    companion object {
        val unitTypeStrings = hashSetOf(
            "Military",
            "Civilian",
            "non-air",
            "relevant",
            "Nuclear Weapon",
            "City",
            // These are up for debate
            "Air",
            "land units",
            "water units",
            "air units",
            "military units",
            "submarine units",
            // Note: this can't handle combinations of parameters (e.g. [{Military} {Water}])
        )

        val cityFilterStrings = setOf( // taken straight from the translation!
            "in this city",
            "in all cities",
            "in all coastal cities",
            "in capital",
            "in all non-occupied cities",
            "in all cities with a world wonder",
            "in all cities connected to capital",
            "in all cities with a garrison",
            "in all cities in which the majority religion is a major religion",
            "in all cities in which the majority religion is an enhanced religion",
            "in non-enemy foreign cities",
            "in foreign cities",
            "in annexed cities",
            "in holy cities",
            "in City-State cities",
            "in cities following this religion",
        )

        fun safeValueOf(param: String) = values().firstOrNull { it.parameterName == param } ?: Unknown
    }
}


class UniqueComplianceError(
    val parameterName: String,
    val acceptableParameterTypes: List<UniqueParameterType>,
    val errorSeverity: UniqueType.UniqueComplianceErrorSeverity
)
