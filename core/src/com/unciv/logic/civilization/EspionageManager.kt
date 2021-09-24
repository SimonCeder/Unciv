package com.unciv.logic.civilization

import com.unciv.logic.city.CityInfo
import com.unciv.models.metadata.GameSpeed
import com.unciv.models.ruleset.Era
import com.unciv.models.ruleset.unique.UniqueType
import com.unciv.models.stats.INamed
import kotlin.math.max
import kotlin.random.Random

class EspionageManager {

    @Transient
    lateinit var civInfo: CivilizationInfo

    // Our active spies
    val spies = ArrayList<Spy>()

    fun clone(): EspionageManager {
        val clone = EspionageManager()
        clone.spies.addAll(spies)
        return clone
    }

    fun setTransients() {
        for (spy in spies) {
            spy.civInfo = civInfo
            spy.espionageManager = this
        }
    }

    /** Called upon reaching Era, gives a civ (and potentially other civs) the appropriate number of spies. */
    fun gainSpiesFromEra(era: Era) {
        for (unique in era.getMatchingUniques(UniqueType.EraProvideSpy)) {
            repeat(unique.params[0].toInt()) { recruitSpy() }
        }

        // only give the spies if we are first to this era
        if (civInfo.gameInfo.civilizations.none { it != civInfo && it.getEraNumber() >= era.eraNumber } ) {
            for (unique in era.getMatchingUniques(UniqueType.EraProvideGlobalSpy)) {
                for (civ in civInfo.gameInfo.civilizations)
                    repeat(unique.params[0].toInt()) { civ.espionageManager.recruitSpy() }
            }
        }
    }

    /** For later era starts, gives a civ all the spies they would have gotten up to that point */
    fun gainStartingSpies(startingEra: Era) {
        // Less than because starting in eg the Renaissance actually starts you in the Medieval currently
        for (era in civInfo.gameInfo.ruleSet.eras.values.filter { it.eraNumber < startingEra.eraNumber }) {
            for (unique in era.getMatchingUniques(UniqueType.EraProvideSpy)) {
                repeat(unique.params[0].toInt()) { recruitSpy() }
            }

            // we only need to give spies to ourselves, all the other civs will be doing this as well
            for (unique in era.getMatchingUniques(UniqueType.EraProvideGlobalSpy)) {
                repeat(unique.params[0].toInt()) { recruitSpy() }
            }
        }
    }

    fun recruitSpy() {
        if (!civInfo.gameInfo.gameParameters.espionageEnabled) return

        val spy = Spy (getSpyName(), getStartingRank(), null, SpyStatus.Unassigned)
        spy.civInfo = civInfo
        spy.espionageManager = this
        spies.add(spy)

        civInfo.addNotification("We have recruited a new spy! They are waiting in the hideout for their first orders.", NotificationIcon.Spy) // TODO: Open spy screen on click
    }

    fun endTurn() {
        for (spy in spies) {
            spy.processTurn()
        }
    }

    fun getSpyName(): String {
        val spyNames = listOf("Bob", "Biff", "Mr Cool ICE", "Dude", "Geezer", "Closeau", "Tim") // TODO: Civ-specific lists
        val unusedSpyNames = spyNames.filterNot { spyName -> spyName in spies.map { it.name } }
        return if (unusedSpyNames.isEmpty()) spyNames.random()
                else unusedSpyNames.random()
    }

    fun getStartingRank(): SpyRank {
        return SpyRank.Recruit // TODO: +1 if National Intelligence Agency
    }

    fun canMoveTo(destination: CityInfo): Boolean {
        if (destination.getCenterTile().position !in civInfo.exploredTiles) return false // Can't see
        if (spies.any { it.currentCity == destination } ) return false // Already taken
        return true
    }
}

class Spy (var name: String,
           var rank: SpyRank,
           var currentCity: CityInfo?,
           var status: SpyStatus) {
    var progress = 0
    var goal = 0

    // While this is true we have vision of their current city
    var surveillanceEstablished = false

    @Transient
    lateinit var civInfo: CivilizationInfo

    @Transient
    lateinit var espionageManager: EspionageManager

    fun processTurn() {
        if (status == SpyStatus.Unassigned) return

        when (status) {
            SpyStatus.GatheringIntel -> {
                checkStealableTechs() // Check every turn if we still can steal something
            }
        }

        // Counterintel has no timer
        if (status != SpyStatus.Counterintelligence) {
            progress += getProgressPerTurn()
            if (progress >= goal) {
                reachedGoal()
                progress = 0
                goal = getProgressRequired()
            }
        }
    }

    /** Called when a Spy has reached its goal. */
    private fun reachedGoal() {
        when (status) {
            SpyStatus.Travelling -> { // We arrived somewhere
                status = if (currentCity!!.civInfo == civInfo)  // Friendly city
                    SpyStatus.Counterintelligence
                else // Other civ's city, start surveillance
                    SpyStatus.Surveillance
            }
            SpyStatus.Surveillance -> { // Time to do the thing
                if (currentCity!!.civInfo.isCityState())
                    status = SpyStatus.RiggingElection
                else {
                    status = SpyStatus.GatheringIntel
                    checkStealableTechs() // Might switch back to surveillance
                    uncoverIntrigue()
                }
                if (!surveillanceEstablished) {
                    surveillanceEstablished = true
                    civInfo.updateViewableTiles() // We can see their city now
                }
            }
            SpyStatus.GatheringIntel -> { // The big heist
                var result: SpyResult
                val rng = Random(currentCity!!.location.x.toInt() + currentCity!!.location.y.toInt() * 1000 + civInfo.gameInfo.turns)
                var roll = rng.nextInt(300)
                val counterSpy = currentCity!!.getCounterSpy()

                if (counterSpy != null) {
                    roll += counterSpy.rank.skill * 30
                    // TODO: Policy-based catching modifiers
                    result = if (roll < 100)
                        SpyResult.Detected
                    else if (roll < 200)
                        SpyResult.Identified
                    else
                        SpyResult.Killed
                } else {
                    // TODO: Policy-based catching modifiers
                    result = if (roll < 100)
                        SpyResult.Undetected
                    else if (roll < 200)
                        SpyResult.Detected
                    else
                        SpyResult.Identified
                }

                if (result == SpyResult.Killed) {
                    civInfo.addNotification("[$name] was killed by enemy counterintelligence agents while attempting to steal technology from [${currentCity!!.civInfo.civName}]!",
                        currentCity!!.location, NotificationIcon.Spy, NotificationIcon.Death)
                    if (counterSpy != null)
                        counterSpy.levelUp()

                    currentCity = null
                    status = SpyStatus.Dead
                } else { // Made it
                    if (result == SpyResult.Identified) {
                        // TODO: Some diplomatic modifier
                    }
                    // Steal the tech
                    val stolenTech = getStealableTechs().random(rng)
                    civInfo.tech.addTechnology(stolenTech)
                    civInfo.addNotification("[$name] stole the secrets of [$stolenTech] from [${currentCity!!.civInfo.civName}]!",
                        currentCity!!.location, NotificationIcon.Spy, NotificationIcon.Science)

                    levelUp()
                    uncoverIntrigue()
                }
            }
            SpyStatus.RiggingElection -> {} // TODO
            SpyStatus.Dead -> { // Back to life!
                val oldName = name
                name = espionageManager.getSpyName()
                rank = espionageManager.getStartingRank()
                currentCity = null
                status = SpyStatus.Unassigned

                civInfo.addNotification("We have recruited [$name] to replace the loss of [$oldName]! They are waiting in the hideout for their first orders.",
                    NotificationIcon.Spy) // TODO: Open spy screen on click
            }
        }

        // Since we finished our old task, reset progress and goal
        progress = 0
        goal = getProgressRequired()
    }

    fun moveTo(destination: CityInfo) {
        if (!espionageManager.canMoveTo(destination)) return // no thx

        currentCity = destination
        status = SpyStatus.Travelling
        progress = 0
        goal = getProgressRequired()
        val hadSurveillance = surveillanceEstablished
        surveillanceEstablished = false // have to start over at new city
        if (hadSurveillance)
            civInfo.updateViewableTiles()
    }

    /** If no stealable techs at current location, reverts to Surveillance and displays a notification. */
    private fun checkStealableTechs() {
        if (status != SpyStatus.GatheringIntel || currentCity == null) return // What?
        if (getStealableTechs().isNotEmpty()) return // All's well, carry on

        status = SpyStatus.Surveillance
        civInfo.addNotification("[$name] is unable to steal any technology from [${currentCity!!.civInfo.civName}] because we have completely passed them in research!",
            currentCity!!.location, NotificationIcon.Spy, civInfo.civName)

        // Let him keep progress
        goal = getProgressRequired()
    }

    private fun getStealableTechs(): List<String> {
        return emptyList() // TODO
    }

    fun getProgressPerTurn(): Int {
        return when (status) {
            SpyStatus.Travelling,
            SpyStatus.Dead,
            SpyStatus.Surveillance -> 1
            SpyStatus.Unassigned,
            SpyStatus.Counterintelligence -> 0
            SpyStatus.RiggingElection -> {
                (rank.skill + 1) * (rank.skill + 1)
            }
            SpyStatus.GatheringIntel -> {
                var rate = currentCity!!.cityStats.currentCityStats.science
                if (civInfo.gameInfo.gameParameters.gameSpeed == GameSpeed.Quick)
                    rate *= 0.5f
                // TODO: City-level espionage modifiers constabulary etc
                // TODO: Civ uniques, policies
                rate = max(a = rate, b = 1f) // Can't be lower than 1

                rate *= 1f + (rank.skill * 0.25f)

                return rate.toInt()
            }
        }
    }

    fun getProgressRequired(): Int {
        return when (status) {
            SpyStatus.Travelling -> 1
            SpyStatus.Surveillance -> 3
            SpyStatus.Dead -> 5
            SpyStatus.RiggingElection -> 10 // TODO GetTurnsUntilMinorCivElection()
            SpyStatus.GatheringIntel -> {
                (1.25f * getStealableTechs().maxOf { civInfo.gameInfo.ruleSet.technologies[it]!!.cost }).toInt() // times 100?
            }
            else -> 999
        }
    }

    private fun levelUp() {
        if (rank == SpyRank.SpecialAgent) return // Already at max
        rank = when (rank) {
            SpyRank.Agent -> SpyRank.SpecialAgent
            SpyRank.Recruit -> SpyRank.Agent
            else -> SpyRank.Recruit
        }

        civInfo.addNotification("[$name] has been promoted and is now the rank [${rank.displayName}].",
            NotificationIcon.Spy) // TODO: Open spy screen on click
    }

    /** Uncovers an AI plan, such as "plans to invade the Huns by Sea", or "is lying to the Aztecs"
     *  or whatever. Currently difficult to implement as our AI doesn't exactly HAVE plans, but
     *  something to think about for the future
     */
    private fun uncoverIntrigue() {
        return
    }
}

enum class SpyStatus {
    Unassigned, // At hideout
    Travelling,
    Surveillance, // Setting up in a foreign city
    GatheringIntel, // Stealing tech and intrigue
    RiggingElection, // Gaining influence
    Counterintelligence, // Stopping enemy spies
    Dead
}

enum class SpyRank (val skill: Int, val displayName: String){
    Recruit (0, "Recruit"),
    Agent (1, "Agent"),
    SpecialAgent (2, "Special Agent")
}

enum class SpyResult {
    Undetected, // No warning for target
    Detected,   // Target knows someone did something but not who
    Identified, // Target knows who did it
    Killed      // The spy is dead
}