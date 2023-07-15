package com.example.presentation.mvvm_single_activity.fragment.battlefield

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.navigation.fragment.navArgs
import com.example.dot_game_view_module.Dot
import com.example.dot_game_view_module.DotGameField
import com.example.presentation.R
import com.example.presentation.databinding.FragmentBattlefieldBinding
import com.example.presentation.mvvm_single_activity.utils.baseFragment.BaseFragment

class BattlefieldFragment : BaseFragment(R.layout.fragment_battlefield) {
    private lateinit var binding:FragmentBattlefieldBinding
    private val args:BattlefieldFragmentArgs by navArgs()
    private var isFirstPlayer = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding = FragmentBattlefieldBinding.bind(view)

        with(binding){
            val namePlayerTemplate = getString(R.string.player_name_string)
            player1Name.text = String.format(namePlayerTemplate, args.namePlayer1)
            player2Name.text = String.format(namePlayerTemplate, args.namePlayer2)
            battlefieldView.dotGameField = DotGameField()
            battlefieldView.actionListener = { row,column,field->
                val point = field.getDot(row,column)
                if(point == Dot.EMPTY){
                    isFirstPlayer = if(isFirstPlayer){
                        field.setDot(row, column, Dot.PLAYER_1)
                        false
                    } else{
                        field.setDot(row, column, Dot.PLAYER_2)
                        true
                    }
                }
                if(field.gameOverProgress()){
                    Toast.makeText(
                        requireContext(),
                        "GAME OVER!",
                        Toast.LENGTH_SHORT
                    ).show()
                    activity?.onBackPressedDispatcher?.onBackPressed()
                }
            }
            battlefieldView.captureCounterListener = {color, numbCounter->
                if(color == Dot.PLAYER_1){
                    player1Counter.text =
                        "$numbCounter"
                }
                else{
                    player2Counter.text =
                        "$numbCounter"
                }
            }
        }

        super.onViewCreated(view, savedInstanceState)
    }

    override fun onClick(v: View?) {
        TODO("Not yet implemented")
    }
}