package com.example.presentation.mvvm_single_activity.fragment.mainMenu

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.example.presentation.R
import com.example.presentation.databinding.FragmentMainMenuBinding
import com.example.presentation.mvvm_single_activity.utils.baseFragment.BaseFragment

class MainMenuFragment : BaseFragment(R.layout.fragment_main_menu) {
    private lateinit var binding:FragmentMainMenuBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding = FragmentMainMenuBinding.bind(view)
        with(binding){
            createNewGame.setOnClickListener(this@MainMenuFragment)
            exitGame.setOnClickListener(this@MainMenuFragment)
        }
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onClick(v: View) {
        when(v.id){
            R.id.createNewGame->{
                findNavController().navigate(R.id.createPlayerFragment)
            }

            R.id.exitGame->{
                activity?.onBackPressedDispatcher?.onBackPressed()
            }
        }
    }
}