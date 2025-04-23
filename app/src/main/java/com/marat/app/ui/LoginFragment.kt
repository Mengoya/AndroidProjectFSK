package com.marat.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.marat.app.R
import com.marat.app.data.PrefManager
import com.marat.app.databinding.FragmentLoginBinding   // viewBinding включите если хотите

class LoginFragment: Fragment() {
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private lateinit var pref: PrefManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, s: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        pref = PrefManager(requireContext())

        binding.btnLogin.setOnClickListener {
            val user = binding.etUser.text.toString()
            val pass = binding.etPass.text.toString()
            if (pref.login(user, pass)) {
                findNavController().navigate(R.id.action_login_to_home)
            } else {
                Toast.makeText(requireContext(), "Неверные данные", Toast.LENGTH_SHORT).show()
            }
        }

        binding.tvRegister.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_register)
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}