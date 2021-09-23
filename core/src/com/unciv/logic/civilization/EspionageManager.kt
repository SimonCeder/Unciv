package com.unciv.logic.civilization

import com.unciv.logic.city.CityInfo

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
        for (spy in spies)
            spy.civInfo = civInfo
    }

    fun recruitSpy() {
        if (!civInfo.gameInfo.gameParameters.espionageEnabled) return

        val spyNames = listOf("Bob", "Biff", "Mr Cool ICE", "Dude", "Geezer", "Slartibartfast", "Tim") // TODO: Civ-specific lists
        val unusedSpyNames = spyNames.filterNot { spyName -> spyName in spies.map { it.name } }
        val name = if (unusedSpyNames.isEmpty()) spyNames.random()
                    else unusedSpyNames.random()

        val startRank = SpyRank.Recruit // TODO: +1 if National Intelligence Agency

        val spy = Spy (name, startRank, null, SpyStatus.Unassigned)
        spies.add(spy)
    }

    fun endTurn() {
        for (spy in spies) {
            spy.processTurn()
        }
    }
}

class Spy (var name: String,
           var rank: SpyRank,
           var currentCity: CityInfo?,
           var status: SpyStatus) {
    var progress = 0
    var goal = 0

    @Transient
    lateinit var civInfo: CivilizationInfo

    fun processTurn() {
        when (status) {
            // These are passive and never expire
            SpyStatus.Unassigned,
            SpyStatus.Counterintelligence -> return
            // Other missions have timers
            else -> {
                progress += getProgressPerTurn()
                if (progress >= goal)
                    reachedGoal()
            }
        }
    }

    private fun reachedGoal() {
        when (status) {
            SpyStatus.Travelling -> {
                if (currentCity!!.civInfo == civInfo) { // Friendly city
                    status = SpyStatus.Counterintelligence
                } else { // Other civ's city, start surveillance
                    status = SpyStatus.Surveillance
                    progress = 0
                    goal = getProgressRequired(status)
                }
            }

        }
    }

    fun getProgressPerTurn(): Int {
        return 1 // TODO
    }

    fun getProgressRequired(activity: SpyStatus): Int {
        return 5 // TODO
    }
}

enum class SpyStatus {
    Unassigned,
    Travelling,
    Surveillance,
    GatheringIntel,
    RiggingElection,
    Counterintelligence,
    Dead
}

enum class SpyRank {
    Recruit,
    Agent,
    SpecialAgent
}