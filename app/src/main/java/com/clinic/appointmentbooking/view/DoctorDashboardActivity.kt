package com.clinic.appointmentbooking.view

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.clinic.appointmentbooking.R
import com.clinic.appointmentbooking.adapter.DoctorAppointmentAdapter
import com.clinic.appointmentbooking.databinding.ActivityDoctorDashboardBinding
import com.clinic.appointmentbooking.model.Appointment
import com.clinic.appointmentbooking.util.Resource
import com.clinic.appointmentbooking.viewmodel.AppointmentViewModel
import com.clinic.appointmentbooking.viewmodel.AuthViewModel
import java.util.Calendar

class DoctorDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDoctorDashboardBinding
    private val appointmentViewModel: AppointmentViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()
    private lateinit var appointmentAdapter: DoctorAppointmentAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDoctorDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        // Populate dynamic doctor name from Firebase Auth
        val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        val displayName = firebaseUser?.displayName
        if (!displayName.isNullOrBlank()) {
            binding.tvDoctorName.text = "Dr. $displayName"
        } else {
            binding.tvDoctorName.text = "Doctor"
        }

        setupRecyclerView()
        setupObservers()
        appointmentViewModel.startListeningToAppointments()
    }

    // ── Toolbar overflow menu ────────────────────────────────────────────────

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_doctor_toolbar, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                showLogoutConfirmationDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // ── RecyclerView ─────────────────────────────────────────────────────────

    private fun setupRecyclerView() {
        appointmentAdapter = DoctorAppointmentAdapter(
            onMarkCompleted = { appointment -> markAsCompleted(appointment) },
            onSetNextVisit  = { appointment -> showNextVisitPicker(appointment) },
            onPatientClick  = { appointment -> openPatientDetails(appointment) }
        )
        binding.rvAppointments.apply {
            layoutManager = LinearLayoutManager(this@DoctorDashboardActivity)
            adapter = appointmentAdapter
            // NestedScrollView handles scroll; disable inner scrolling
            isNestedScrollingEnabled = false
        }
    }

    // ── Actions ──────────────────────────────────────────────────────────────

    private fun openPatientDetails(appointment: Appointment) {
        val intent = Intent(this, PatientDetailsActivity::class.java).apply {
            putExtra(PatientDetailsActivity.EXTRA_NAME,       appointment.patientName)
            putExtra(PatientDetailsActivity.EXTRA_PHONE,      appointment.patientPhone)
            putExtra(PatientDetailsActivity.EXTRA_AGE,        "") // age not in Appointment; pass blank
            putExtra(PatientDetailsActivity.EXTRA_DOCTOR,     appointment.doctorName)
            putExtra(PatientDetailsActivity.EXTRA_DATE,       appointment.date)
            putExtra(PatientDetailsActivity.EXTRA_TIME,       appointment.time)
            putExtra(PatientDetailsActivity.EXTRA_STATUS,     appointment.status)
            putExtra(PatientDetailsActivity.EXTRA_NEXT_VISIT, appointment.nextVisitDate)
        }
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_right, 0)
    }

    private fun markAsCompleted(appointment: Appointment) {
        if (appointment.id.isBlank()) {
            Toast.makeText(this, "Invalid appointment", Toast.LENGTH_SHORT).show()
            return
        }
        appointmentViewModel.updateAppointmentStatus(appointment.id, "completed")
    }

    private fun showNextVisitPicker(appointment: Appointment) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val nextVisit = String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year)
                appointmentViewModel.updateNextVisitDate(appointment.id, nextVisit)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.minDate = System.currentTimeMillis() - 1000
        }.show()
    }

    // ── Observers ────────────────────────────────────────────────────────────

    private fun setupObservers() {
        appointmentViewModel.appointments.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.tvEmptyState.visibility = View.GONE
                }
                is Resource.Success -> {
                    binding.progressBar.visibility = View.GONE
                    val list = resource.data
                    if (list.isEmpty()) {
                        binding.tvEmptyState.visibility = View.VISIBLE
                        binding.rvAppointments.visibility = View.GONE
                    } else {
                        binding.tvEmptyState.visibility = View.GONE
                        binding.rvAppointments.visibility = View.VISIBLE
                        appointmentAdapter.submitList(list)
                    }
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this, resource.message, Toast.LENGTH_LONG).show()
                }
                null -> {}
            }
        }

        appointmentViewModel.todayCount.observe(this) { count ->
            binding.tvTodayCount.text = count.toString()
        }

        appointmentViewModel.monthlyCount.observe(this) { count ->
            binding.tvMonthlyCount.text = count.toString()
        }

        appointmentViewModel.updateStatusState.observe(this) { resource ->
            when (resource) {
                is Resource.Success -> {
                    Toast.makeText(this, "✅ Appointment marked as done!", Toast.LENGTH_SHORT).show()
                    appointmentViewModel.resetUpdateState()
                }
                is Resource.Error -> {
                    Toast.makeText(this, resource.message, Toast.LENGTH_LONG).show()
                    appointmentViewModel.resetUpdateState()
                }
                else -> {}
            }
        }

        appointmentViewModel.updateNextVisitState.observe(this) { resource ->
            when (resource) {
                is Resource.Success -> {
                    Toast.makeText(this, "📅 Next visit date saved!", Toast.LENGTH_SHORT).show()
                    appointmentViewModel.resetNextVisitState()
                }
                is Resource.Error -> {
                    Toast.makeText(this, resource.message, Toast.LENGTH_LONG).show()
                    appointmentViewModel.resetNextVisitState()
                }
                else -> {}
            }
        }
    }

    // ── Logout ───────────────────────────────────────────────────────────────

    private fun showLogoutConfirmationDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setIcon(R.drawable.ic_logout)
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton("Logout") { _, _ ->
                logout()
            }
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
