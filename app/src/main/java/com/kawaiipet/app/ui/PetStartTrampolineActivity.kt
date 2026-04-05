package com.kawaiipet.app.ui

import android.app.Activity
import android.os.Bundle
import com.kawaiipet.app.util.PetLauncher

/**
 * Invisible launcher for the "Start pet" shortcut: starts the overlay (or permission flow) and exits.
 */
class PetStartTrampolineActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PetLauncher.startPetFromExternalTrigger(this)
        finish()
    }
}
