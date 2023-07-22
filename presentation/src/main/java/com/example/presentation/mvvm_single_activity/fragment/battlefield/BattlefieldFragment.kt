package com.example.presentation.mvvm_single_activity.fragment.battlefield

import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.navigation.fragment.navArgs
import com.example.dot_game_view_module.Dot
import com.example.dot_game_view_module.DotGameField
import com.example.presentation.R
import com.example.presentation.databinding.FragmentBattlefieldBinding
import com.example.presentation.mvvm_single_activity.utils.baseFragment.BaseFragment
import kotlin.properties.Delegates

class BattlefieldFragment : BaseFragment(R.layout.fragment_battlefield) {
    private lateinit var binding:FragmentBattlefieldBinding
    private val args:BattlefieldFragmentArgs by navArgs()
    private var isFirstPlayer by Delegates.notNull<Boolean>()
    private lateinit var dotGameField:DotGameField

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding = FragmentBattlefieldBinding.bind(view)

        with(binding){
            val namePlayerTemplate = getString(R.string.player_name_string)
            player1Name.text = String.format(namePlayerTemplate, args.namePlayer1)
            player2Name.text = String.format(namePlayerTemplate, args.namePlayer2)

            battlefieldView.dotGameField = dotGameField
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
                if(field.gameOverProgress()) endGame()
            }
            battlefieldView.captureCounterListener = {color, numbCounter->
                if(color == Dot.PLAYER_1){
                    player1Counter.text = "$numbCounter"
                }
                else{
                    player2Counter.text = "$numbCounter"
                }
            }
            earlyEndGame.setOnClickListener(this@BattlefieldFragment)
        }

        super.onViewCreated(view, savedInstanceState)
    }

    private fun endGame(){
        Toast.makeText(
            requireContext(),
            "GAME OVER!",
            Toast.LENGTH_SHORT
        ).show()
        activity?.onBackPressedDispatcher?.onBackPressed()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isFirstPlayer = savedInstanceState?.getBoolean(KEY_TURN_PLAYER) ?: true
        dotGameField = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            savedInstanceState?.getParcelable(KEY_FIELD, DotGameField.Memento::class.java)?.restoreField() ?: DotGameField()
        } else{
            savedInstanceState?.getParcelable<DotGameField.Memento>(KEY_FIELD)?.restoreField() ?: DotGameField()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val field = binding.battlefieldView.dotGameField
        val saveLines = binding.battlefieldView.createSaveExistingLinesList()
        outState.putParcelable(KEY_FIELD, field!!.saveState(saveLines))
        outState.putBoolean(KEY_TURN_PLAYER, isFirstPlayer)
    }

    override fun onClick(v: View) {
        when(v.id){
            R.id.early_end_game-> endGame()
        }
    }

    companion object{
        private const val KEY_FIELD = "KEY_FIELD"
        private const val KEY_TURN_PLAYER = "KEY_TURN_PLAYER"
    }
}