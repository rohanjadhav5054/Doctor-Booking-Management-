package com.clinic.appointmentbooking.view

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.clinic.appointmentbooking.databinding.ActivityLoginBinding
import com.clinic.appointmentbooking.util.Resource
import com.clinic.appointmentbooking.viewmodel.AuthViewModel

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Check if user is already logged in
        checkExistingSession()

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

    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            authViewModel.login(email, password)
        }
    }

    private fun setupObservers() {
        authViewModel.loginState.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> showLoading(true)
                is Resource.Success -> {
                    // Login succeeded, now fetch role
                    authViewModel.fetchUserRole(resource.data)
                }
                is Resource.Error -> {
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
                    navigateByRole(resource.data)
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
            "doctor" -> Intent(this, DoctorDashboardActivity::class.java)
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
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !show
        binding.etEmail.isEnabled = !show
        binding.etPassword.isEnabled = !show
    }
}
