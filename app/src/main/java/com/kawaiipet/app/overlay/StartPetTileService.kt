package com.kawaiipet.app.overlay

import android.service.quicksettings.TileService
import com.kawaiipet.app.util.PetLauncher

class StartPetTileService : TileService() {

    override fun onClick() {
        unlockAndRun {
            PetLauncher.startPetFromExternalTrigger(this)
        }
    }
}
