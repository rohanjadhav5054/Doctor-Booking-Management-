package com.clinic.appointmentbooking.view

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import com.clinic.appointmentbooking.R
import com.clinic.appointmentbooking.databinding.ActivityReceptionistDashboardBinding
import com.clinic.appointmentbooking.util.Resource
import com.clinic.appointmentbooking.viewmodel.AppointmentViewModel
import com.clinic.appointmentbooking.viewmodel.AuthViewModel
import com.google.android.material.navigation.NavigationView

class ReceptionistDashboardActivity : AppCompatActivity(),
    NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityReceptionistDashboardBinding
    private val appointmentViewModel: AppointmentViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReceptionistDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        // Drawer toggle (hamburger ↔ arrow)
        val toggle = ActionBarDrawerToggle(
            this, binding.drawerLayout, binding.toolbar,
            R.string.nav_open, R.string.nav_close
        )
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        binding.navigationView.setNavigationItemSelectedListener(this)

        // Quick-action card click listeners
        setupCardClicks()

        // Live appointment stats
        setupObservers()
        appointmentViewModel.startListeningToAppointments()
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

    // ── Navigation drawer ────────────────────────────────────────────────────

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_dashboard        -> { /* already here */ }
            R.id.nav_add_patient      -> startActivity(Intent(this, AddPatientActivity::class.java))
            R.id.nav_book_appointment -> startActivity(Intent(this, BookAppointmentActivity::class.java))
            R.id.nav_view_appointments-> startActivity(Intent(this, ViewAppointmentsActivity::class.java))
            R.id.nav_logout           -> logout()
        }
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    // ── Stats ────────────────────────────────────────────────────────────────

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

    private fun logout() {
        authViewModel.logout()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}
