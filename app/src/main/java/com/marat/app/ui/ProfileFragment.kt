package com.marat.app.ui

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.textfield.TextInputEditText
import com.marat.app.R
import com.marat.app.data.PrefManager

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private lateinit var pref: PrefManager

    override fun onViewCreated(v: View, s: Bundle?) {
        pref = PrefManager(requireContext())

        v.findViewById<TextView>(R.id.tvUsername).text = pref.getUsername() ?: "user"

        v.findViewById<Button>(R.id.btnChangePass).setOnClickListener { showDialog() }

        v.findViewById<Button>(R.id.btnLogout).setOnClickListener {
            pref.logout(); findNavController().navigate(R.id.loginFragment)
        }
    }

    private fun showDialog() {
        val dlgView = layoutInflater.inflate(R.layout.dialog_change_pass, null)
        val oldEt = dlgView.findViewById<TextInputEditText>(R.id.etOld)
        val newEt = dlgView.findViewById<TextInputEditText>(R.id.etNew)

        AlertDialog.Builder(requireContext())
            .setTitle("Сменить пароль")
            .setView(dlgView)
            .setPositiveButton("OK") { _, _ ->
                val ok = pref.changePassword(oldEt.text.toString(), newEt.text.toString())
                Toast.makeText(requireContext(),
                    if (ok) "Пароль изменён" else "Старый пароль неверный",
                    Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
}
