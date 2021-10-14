package com.unciv.ui.overviewscreen

import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.unciv.logic.city.CityInfo
import com.unciv.logic.civilization.CivilizationInfo
import com.unciv.logic.civilization.Spy
import com.unciv.logic.civilization.SpyStatus
import com.unciv.ui.utils.*

class EspionageOverviewTable(
    private val viewingPlayer: CivilizationInfo,
    private val overviewScreen: EmpireOverviewScreen
): Table() {

    init {
        add(getSpyListTable()).padRight(25f)
        add(getDestinationListTable())
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
        val turnsRemaining = spy.getTurnsRemaining()
        val taskString = if (turnsRemaining == -1) spy.status.displayName
            else "[${spy.status.displayName}]: [$turnsRemaining]"
        spyTable.add(taskString.toLabel()).width(300f).row()

        spyTable.add("MOVE".toLabel()) // TODO: Button
        spyTable.add("ACTION".toLabel()) // TODO: Button
        val location = spy.currentCity?.name ?: "Hideout"
        spyTable.add("At $location".toLabel()).width(300f)

        return spyTable
    }

    private fun getDestinationListTable(): Table {
        val destinationsTable = Table(CameraStageBaseScreen.skin)
        destinationsTable.defaults().pad(10f)

        val civList = viewingPlayer.gameInfo.civilizations.filter { !it.isBarbarian() }
        val sortedCivList = civList.sortedBy { if (it == viewingPlayer) 0 else if (it.isMajorCiv()) 1 else 2 }

        for (civ in sortedCivList) {
            for (city in civ.cities) {
                destinationsTable.add(getCityTable(city)).row()
            }

            if (civ.cities.isNotEmpty())
                destinationsTable.addSeparator()
        }

        return destinationsTable
    }

    private fun getCityTable(city: CityInfo): Table {
        val cityTable = Table()
        // TODO: Color by civ
        // TODO: Civ icons
        cityTable.add(city.name.toLabel())

        // TODO: Second row with assigned spy
        // TODO: Display science ranking if gathering intel

        return  cityTable
    }
}