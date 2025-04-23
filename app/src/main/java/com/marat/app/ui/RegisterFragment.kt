package com.marat.app.ui

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.marat.app.R
import com.marat.app.data.PrefManager
import com.marat.app.databinding.FragmentRegisterBinding

class RegisterFragment: Fragment() {
    private var _b: FragmentRegisterBinding? = null
    private val b get() = _b!!
    private lateinit var pref: PrefManager
    override fun onCreateView(inflater: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentRegisterBinding.inflate(inflater, c, false)
        return b.root
    }
    override fun onViewCreated(v: View, s: Bundle?) {
        pref = PrefManager(requireContext())
        b.btnRegister.setOnClickListener {
            val u = b.etUser.text.toString()
            val p = b.etPass.text.toString()
            if (pref.register(u,p)) {
                Toast.makeText(requireContext(),"Успешно, войдите", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_register_to_login)
            } else {
                Toast.makeText(requireContext(),"Аккаунт уже существует", Toast.LENGTH_SHORT).show()
            }
        }
        b.tvLogin.setOnClickListener {
            findNavController().navigate(R.id.action_register_to_login)
        }
    }
    override fun onDestroyView() { super.onDestroyView(); _b=null }
}