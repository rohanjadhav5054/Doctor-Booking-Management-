package com.clinic.appointmentbooking.view

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.clinic.appointmentbooking.R
import com.clinic.appointmentbooking.databinding.ActivityReceptionistDashboardBinding
import com.clinic.appointmentbooking.util.Resource
import com.clinic.appointmentbooking.viewmodel.AppointmentViewModel
import com.clinic.appointmentbooking.viewmodel.AuthViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ReceptionistDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReceptionistDashboardBinding
    private val appointmentViewModel: AppointmentViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReceptionistDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        setupCardClicks()
        setupObservers()
        appointmentViewModel.startListeningToAppointments()
    }

    // ── Toolbar overflow menu ────────────────────────────────────────────────

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_receptionist_toolbar, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_add_patient -> {
                startActivity(Intent(this, AddPatientActivity::class.java))
                true
            }
            R.id.action_book_appointment -> {
                startActivity(Intent(this, BookAppointmentActivity::class.java))
                true
            }
            R.id.action_view_appointments -> {
                startActivity(Intent(this, ViewAppointmentsActivity::class.java))
                true
            }
            R.id.action_logout -> {
                showLogoutConfirmationDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // ── Quick-action cards ───────────────────────────────────────────────────

    private fun setupCardClicks() {
        binding.cardAddPatient.setOnClickListener {
            startActivity(Intent(this, AddPatientActivity::class.java))
        }
        binding.cardBookAppointment.setOnClickListener {
            startActivity(Intent(this, BookAppointmentActivity::class.java))
        }
        binding.cardViewAppointments.setOnClickListener {
            startActivity(Intent(this, ViewAppointmentsActivity::class.java))
        }
    }

    // ── Stats observers ──────────────────────────────────────────────────────

    private fun setupObservers() {
        appointmentViewModel.appointments.observe(this) { resource ->
            if (resource is Resource.Success) {
                val all       = resource.data
                val pending   = all.count { it.status.equals("pending",   ignoreCase = true) }
                val completed = all.count { it.status.equals("completed", ignoreCase = true) }
                binding.tvTotalAppointments.text = all.size.toString()
                binding.tvPendingCount.text      = pending.toString()
                binding.tvCompletedCount.text    = completed.toString()
            }
        }
    }

    // ── Logout ───────────────────────────────────────────────────────────────

    private fun showLogoutConfirmationDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setIcon(R.drawable.ic_logout)
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .setPositiveButton("Logout")  { _, _      -> logout() }
            .show()
    }

    private fun logout() {
        authViewModel.logout()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
