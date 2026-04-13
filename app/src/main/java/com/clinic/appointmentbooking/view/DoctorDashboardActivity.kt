package com.clinic.appointmentbooking.view

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.clinic.appointmentbooking.adapter.DoctorAppointmentAdapter
import com.clinic.appointmentbooking.databinding.ActivityDoctorDashboardBinding
import com.clinic.appointmentbooking.model.Appointment
import com.clinic.appointmentbooking.util.Resource
import com.clinic.appointmentbooking.viewmodel.AppointmentViewModel
import com.clinic.appointmentbooking.viewmodel.AuthViewModel

class DoctorDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDoctorDashboardBinding
    private val appointmentViewModel: AppointmentViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()
    private lateinit var appointmentAdapter: DoctorAppointmentAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDoctorDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupObservers()
        setupClickListeners()
        appointmentViewModel.startListeningToAppointments()
    }

    private fun setupRecyclerView() {
        appointmentAdapter = DoctorAppointmentAdapter { appointment ->
            markAsCompleted(appointment)
        }
        binding.rvAppointments.apply {
            layoutManager = LinearLayoutManager(this@DoctorDashboardActivity)
            adapter = appointmentAdapter
        }
    }

    private fun setupClickListeners() {
        binding.btnLogout.setOnClickListener { logout() }
    }

    private fun markAsCompleted(appointment: Appointment) {
        if (appointment.id.isBlank()) {
            Toast.makeText(this, "Invalid appointment", Toast.LENGTH_SHORT).show()
            return
        }
        appointmentViewModel.updateAppointmentStatus(appointment.id, "completed")
    }

    private fun setupObservers() {
        appointmentViewModel.appointments.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.tvEmptyState.visibility = View.GONE
                }
                is Resource.Success -> {
                    binding.progressBar.visibility = View.GONE
                    val appointments = resource.data
                    if (appointments.isEmpty()) {
                        binding.tvEmptyState.visibility = View.VISIBLE
                        binding.rvAppointments.visibility = View.GONE
                    } else {
                        binding.tvEmptyState.visibility = View.GONE
                        binding.rvAppointments.visibility = View.VISIBLE
                        appointmentAdapter.submitList(appointments)
                    }
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this, resource.message, Toast.LENGTH_LONG).show()
                }
                null -> {}
            }
        }

        appointmentViewModel.updateStatusState.observe(this) { resource ->
            when (resource) {
                is Resource.Success -> {
                    Toast.makeText(this, "Appointment marked as completed!", Toast.LENGTH_SHORT).show()
                    appointmentViewModel.resetUpdateState()
                }
                is Resource.Error -> {
                    Toast.makeText(this, resource.message, Toast.LENGTH_LONG).show()
                    appointmentViewModel.resetUpdateState()
                }
                else -> {}
            }
        }
    }

    private fun logout() {
        authViewModel.logout()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
