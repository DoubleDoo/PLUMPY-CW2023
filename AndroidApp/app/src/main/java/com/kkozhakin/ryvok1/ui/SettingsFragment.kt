package com.kkozhakin.ryvok1.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import com.kkozhakin.ryvok1.R
import com.kkozhakin.ryvok1.databinding.FragmentSettingsBinding
import com.kkozhakin.ryvok1.personalData

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        root.findViewById<EditText>(R.id.userName).setText(personalData["userName"])
        root.findViewById<EditText>(R.id.userSurname).setText(personalData["userSurname"])
        root.findViewById<EditText>(R.id.userAge).setText(personalData["userAge"])
        root.findViewById<EditText>(R.id.userHeight).setText(personalData["userHeight"])
        root.findViewById<EditText>(R.id.userWeight).setText(personalData["userWeight"])

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}