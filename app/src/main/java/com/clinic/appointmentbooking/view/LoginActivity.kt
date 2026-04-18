package com.clinic.appointmentbooking.view

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.clinic.appointmentbooking.R
import com.clinic.appointmentbooking.databinding.ActivityLoginBinding
import com.clinic.appointmentbooking.util.Resource
import com.clinic.appointmentbooking.viewmodel.AuthViewModel

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val authViewModel: AuthViewModel by viewModels()

    private var localSelectedRole: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkExistingSession()
        setupRoleToggle()
        setupObservers()
        setupClickListeners()
    }

    private fun checkExistingSession() {
        val currentUser = authViewModel.getCurrentUser()
        if (currentUser != null) {
            showLoading(true)
            authViewModel.fetchUserRole(currentUser.uid)
        }
    }

    private fun setupRoleToggle() {
        binding.btnRoleDoctor.setOnClickListener { selectRole("doctor") }
        binding.btnRoleReceptionist.setOnClickListener { selectRole("receptionist") }
        // Pre-select Doctor by default
        selectRole("doctor")
    }

    private fun selectRole(role: String) {
        localSelectedRole = role
        val primaryColor = ContextCompat.getColor(this, R.color.primary)
        val bgLight = ContextCompat.getColor(this, R.color.bg_light)
        val white = ContextCompat.getColor(this, R.color.white)
        val textSecondary = ContextCompat.getColor(this, R.color.text_secondary)

        if (role == "doctor") {
            binding.btnRoleDoctor.backgroundTintList = ColorStateList.valueOf(primaryColor)
            binding.btnRoleDoctor.setTextColor(white)
            binding.btnRoleReceptionist.backgroundTintList = ColorStateList.valueOf(bgLight)
            binding.btnRoleReceptionist.setTextColor(textSecondary)
        } else {
            binding.btnRoleReceptionist.backgroundTintList = ColorStateList.valueOf(primaryColor)
            binding.btnRoleReceptionist.setTextColor(white)
            binding.btnRoleDoctor.backgroundTintList = ColorStateList.valueOf(bgLight)
            binding.btnRoleDoctor.setTextColor(textSecondary)
        }

        val label = if (role == "doctor")
            "👨‍⚕️ Signing in as Doctor"
        else
            "🖥️ Signing in as Receptionist"
        binding.tvSelectedRole.text = label
        binding.tvSelectedRole.visibility = View.VISIBLE
    }

    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            val email    = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isBlank() || password.isBlank()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            authViewModel.login(email, password)
        }
    }

    private fun setupObservers() {
        authViewModel.loginState.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> showLoading(true)
                is Resource.Success -> authViewModel.fetchUserRole(resource.data)
                is Resource.Error   -> {
                    showLoading(false)
                    Toast.makeText(this, resource.message, Toast.LENGTH_LONG).show()
                }
                null -> {}
            }
        }

        authViewModel.userRole.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> showLoading(true)
                is Resource.Success -> {
                    showLoading(false)
                    val roleFromDb = resource.data
                    if (localSelectedRole.isNotEmpty() &&
                        roleFromDb.lowercase() != localSelectedRole.lowercase()
                    ) {
                        Toast.makeText(
                            this,
                            "Access denied. You are registered as: " +
                                    roleFromDb.replaceFirstChar { it.uppercase() },
                            Toast.LENGTH_LONG
                        ).show()
                        authViewModel.logout()
                        return@observe
                    }
                    navigateByRole(roleFromDb)
                }
                is Resource.Error -> {
                    showLoading(false)
                    Toast.makeText(this, resource.message, Toast.LENGTH_LONG).show()
                }
                null -> {}
            }
        }
    }

    private fun navigateByRole(role: String) {
        val intent = when (role.lowercase()) {
            "doctor"       -> Intent(this, DoctorDashboardActivity::class.java)
            "receptionist" -> Intent(this, ReceptionistDashboardActivity::class.java)
            else -> {
                Toast.makeText(this, "Unknown role: $role", Toast.LENGTH_LONG).show()
                return
            }
        }
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility        = if (show) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled            = !show
        binding.etEmail.isEnabled             = !show
        binding.etPassword.isEnabled          = !show
        binding.btnRoleDoctor.isEnabled       = !show
        binding.btnRoleReceptionist.isEnabled = !show
    }
}
