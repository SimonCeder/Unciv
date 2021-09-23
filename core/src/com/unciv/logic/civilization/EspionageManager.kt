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

    fun recruitSpy() {
        // TODO
    }

    fun handleSpies() {
        for (spy in spies) {
            // TODO
        }
    }
}

class Spy (var name: String,
           var rank: SpyRank,
           var currentCity: CityInfo?,
           var status: SpyStatus) {

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