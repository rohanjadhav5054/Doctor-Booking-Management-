package com.clinic.appointmentbooking.view

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.clinic.appointmentbooking.adapter.ReceptionistAppointmentAdapter
import com.clinic.appointmentbooking.databinding.ActivityViewAppointmentsBinding
import com.clinic.appointmentbooking.util.Resource
import com.clinic.appointmentbooking.viewmodel.AppointmentViewModel

class ViewAppointmentsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityViewAppointmentsBinding
    private val viewModel: AppointmentViewModel by viewModels()
    private lateinit var adapter: ReceptionistAppointmentAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewAppointmentsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "All Appointments"

        setupRecyclerView()
        setupObservers()
        viewModel.startListeningToAppointments()
    }

    private fun setupRecyclerView() {
        adapter = ReceptionistAppointmentAdapter { appointment ->
            // Rebook: launch BookAppointment pre-filled with patient + next visit date
            val intent = Intent(this, BookAppointmentActivity::class.java).apply {
                putExtra("REBOOK_PATIENT_ID",    appointment.patientId)
                putExtra("REBOOK_PATIENT_NAME",  appointment.patientName)
                putExtra("REBOOK_PATIENT_PHONE", appointment.patientPhone)
                putExtra("REBOOK_DATE",          appointment.nextVisitDate)
            }
            startActivity(intent)
        }

        // LinearLayoutManager = native scrolling inside ConstraintLayout
        binding.rvAppointments.apply {
            layoutManager = LinearLayoutManager(this@ViewAppointmentsActivity)
            adapter = this@ViewAppointmentsActivity.adapter
            // No NestedScrollView wrapper → scrolling is provided by RecyclerView itself
            isNestedScrollingEnabled = true
        }
    }

    private fun setupObservers() {
        viewModel.appointments.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.progressBar.visibility   = View.VISIBLE
                    binding.layoutEmpty.visibility   = View.GONE
                    binding.rvAppointments.visibility = View.GONE
                }
                is Resource.Success -> {
                    binding.progressBar.visibility = View.GONE
                    val appointments = resource.data
                    if (appointments.isEmpty()) {
                        binding.layoutEmpty.visibility    = View.VISIBLE
                        binding.rvAppointments.visibility = View.GONE
                    } else {
                        binding.layoutEmpty.visibility    = View.GONE
                        binding.rvAppointments.visibility = View.VISIBLE
                        adapter.submitList(appointments)
                    }
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this, resource.message, Toast.LENGTH_LONG).show()
                }
                null -> {}
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
