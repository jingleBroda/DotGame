package com.example.presentation.mvvm_single_activity.fragment.createPalyer

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.example.presentation.R
import com.example.presentation.databinding.FragmentCreatePlayerBinding
import com.example.presentation.mvvm_single_activity.utils.baseFragment.BaseFragment

class CreatePlayerFragment : BaseFragment(R.layout.fragment_create_player) {
    private lateinit var binding:FragmentCreatePlayerBinding
    private val validator = ValidatorCreatePlayers()
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding = FragmentCreatePlayerBinding.bind(view)
        with(binding){
            startGame.setOnClickListener(this@CreatePlayerFragment)
        }
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onClick(v: View) {
        when(v.id){
            R.id.startGame->{
                findNavController().navigate(
                    validator.getBattlefieldDirection(
                        binding.editNamePlayer1.text.toString(),
                        binding.editNamePlayer2.text.toString()
                    ),
                    navOptions {
                        popUpTo(R.id.createPlayerFragment){
                            inclusive = true
                        }
                    }
                )
            }
        }
    }
}