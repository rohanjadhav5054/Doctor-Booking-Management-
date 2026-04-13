package com.clinic.appointmentbooking.view

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.clinic.appointmentbooking.R
import com.clinic.appointmentbooking.adapter.ReceptionistAppointmentAdapter
import com.clinic.appointmentbooking.databinding.ActivityReceptionistDashboardBinding
import com.clinic.appointmentbooking.util.Resource
import com.clinic.appointmentbooking.viewmodel.AppointmentViewModel
import com.clinic.appointmentbooking.viewmodel.AuthViewModel
import java.util.Calendar

class ReceptionistDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReceptionistDashboardBinding
    private val appointmentViewModel: AppointmentViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()
    private lateinit var appointmentAdapter: ReceptionistAppointmentAdapter

    private var selectedTime = ""
    private var selectedDate = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReceptionistDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupDoctorSpinner()
        setupRecyclerView()
        setupClickListeners()
        setupObservers()
        appointmentViewModel.startListeningToAppointments()
    }

    private fun setupDoctorSpinner() {
        val doctors = resources.getStringArray(R.array.doctors_list)
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            doctors
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerDoctor.adapter = adapter
    }

    private fun setupRecyclerView() {
        appointmentAdapter = ReceptionistAppointmentAdapter()
        binding.rvAppointments.apply {
            layoutManager = LinearLayoutManager(this@ReceptionistDashboardActivity)
            adapter = appointmentAdapter
        }
    }

    private fun setupClickListeners() {
        binding.btnSelectTime.setOnClickListener { showTimePicker() }
        binding.btnSelectDate.setOnClickListener { showDatePicker() }
        binding.btnSubmit.setOnClickListener { submitAppointment() }
        binding.btnLogout.setOnClickListener { logout() }
    }

    private fun showTimePicker() {
        val calendar = Calendar.getInstance()
        TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                val amPm = if (hourOfDay < 12) "AM" else "PM"
                val hour = if (hourOfDay % 12 == 0) 12 else hourOfDay % 12
                selectedTime = String.format("%02d:%02d %s", hour, minute, amPm)
                binding.tvSelectedTime.text = selectedTime
                binding.tvSelectedTime.visibility = View.VISIBLE
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            false
        ).show()
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                selectedDate = String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year)
                binding.tvSelectedDate.text = selectedDate
                binding.tvSelectedDate.visibility = View.VISIBLE
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun submitAppointment() {
        val patientName = binding.etPatientName.text.toString()
        val doctorName = binding.spinnerDoctor.selectedItem?.toString() ?: ""
        appointmentViewModel.addAppointment(patientName, doctorName, selectedTime, selectedDate)
    }

    private fun setupObservers() {
        appointmentViewModel.addAppointmentState.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> setFormEnabled(false)
                is Resource.Success -> {
                    setFormEnabled(true)
                    clearForm()
                    Toast.makeText(this, "Appointment added successfully!", Toast.LENGTH_SHORT).show()
                    appointmentViewModel.resetAddState()
                }
                is Resource.Error -> {
                    setFormEnabled(true)
                    Toast.makeText(this, resource.message, Toast.LENGTH_LONG).show()
                    appointmentViewModel.resetAddState()
                }
                null -> setFormEnabled(true)
            }
        }

        appointmentViewModel.appointments.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.progressBarList.visibility = View.VISIBLE
                    binding.tvEmptyState.visibility = View.GONE
                }
                is Resource.Success -> {
                    binding.progressBarList.visibility = View.GONE
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
                    binding.progressBarList.visibility = View.GONE
                    Toast.makeText(this, resource.message, Toast.LENGTH_LONG).show()
                }
                null -> {}
            }
        }
    }

    private fun clearForm() {
        binding.etPatientName.text?.clear()
        binding.spinnerDoctor.setSelection(0)
        selectedTime = ""
        selectedDate = ""
        binding.tvSelectedTime.visibility = View.GONE
        binding.tvSelectedDate.visibility = View.GONE
        binding.tvSelectedTime.text = ""
        binding.tvSelectedDate.text = ""
    }

    private fun setFormEnabled(enabled: Boolean) {
        binding.etPatientName.isEnabled = enabled
        binding.spinnerDoctor.isEnabled = enabled
        binding.btnSelectTime.isEnabled = enabled
        binding.btnSelectDate.isEnabled = enabled
        binding.btnSubmit.isEnabled = enabled
        binding.progressBarSubmit.visibility = if (enabled) View.GONE else View.VISIBLE
    }

    private fun logout() {
        authViewModel.logout()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
