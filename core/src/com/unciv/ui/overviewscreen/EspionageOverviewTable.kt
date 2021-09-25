package com.unciv.ui.overviewscreen

import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.unciv.logic.civilization.CivilizationInfo
import com.unciv.logic.civilization.Spy
import com.unciv.logic.civilization.SpyStatus
import com.unciv.ui.utils.CameraStageBaseScreen
import com.unciv.ui.utils.addSeparator
import com.unciv.ui.utils.toLabel

class EspionageOverviewTable(
    private val viewingPlayer: CivilizationInfo,
    private val overviewScreen: EmpireOverviewScreen
): Table() {

    init {
        add(getSpyListTable()).padRight(25f)
        add(getDestinationTable())
        pack()
    }

    private fun getSpyListTable(): Table {
        val spyListTable = Table(CameraStageBaseScreen.skin)
        spyListTable.defaults().pad(20f)

        for (spy in viewingPlayer.espionageManager.spies.filter { it.status != SpyStatus.Dead }) {
            spyListTable.add(getSpyTable(spy)).row()
        }

        return spyListTable
    }

    private fun getSpyTable(spy: Spy): Table {
        val spyTable = Table()
        spyTable.defaults().width(100f).pad(10f)

        spyTable.add(spy.name.toLabel())
        spyTable.add(spy.rank.displayName.toLabel())
        // TODO: Progress bar

        spyTable.add("${spy.status.displayName}: ${spy.getTurnsRemaining()}".toLabel()).width(300f).row()

        spyTable.add("MOVE".toLabel())
        spyTable.add("ACTION".toLabel())
        val location = spy.currentCity?.name ?: "Hideout"
        spyTable.add("At $location".toLabel()).width(300f)

        return spyTable
    }

    private fun getDestinationTable(): Table {
        val destinationTable = Table(CameraStageBaseScreen.skin)
        destinationTable.defaults().pad(5f)

        val civList = viewingPlayer.gameInfo.civilizations.filter { !it.isBarbarian() }
        val sortedCivList = civList.sortedBy { if (it == viewingPlayer) 0 else if (it.isMajorCiv()) 1 else 2 }

        for (civ in sortedCivList) {
            for (city in civ.cities) {
                destinationTable.add(city.name.toLabel()).row()
            }

            if (civ.cities.isNotEmpty())
                destinationTable.addSeparator()
        }

        return destinationTable
    }
}