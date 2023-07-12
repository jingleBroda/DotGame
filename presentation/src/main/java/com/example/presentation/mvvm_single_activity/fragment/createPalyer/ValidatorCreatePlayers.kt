package com.example.presentation.mvvm_single_activity.fragment.createPalyer

import androidx.navigation.NavDirections

class ValidatorCreatePlayers {
    private var emptyPlayer1 = true
    private var emptyPlayer2 = true

    fun getBattlefieldDirection(namePlayer1:String?, namePlayer2:String?): NavDirections {
        var correctNamePlayer1 = namePlayer1
        var correctNamePlayer2 = namePlayer2
        if(namePlayer1 == null || namePlayer1 =="") correctNamePlayer1 = "Player1"
        if(namePlayer2 == null || namePlayer2 =="") correctNamePlayer2 = "Player2"
        return CreatePlayerFragmentDirections.
        actionCreatePlayerFragmentToBattlefieldFragment(
            correctNamePlayer1!!,
            correctNamePlayer2!!
        )
    }
}