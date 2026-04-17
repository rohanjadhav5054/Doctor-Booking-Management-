package com.clinic.appointmentbooking.view

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.clinic.appointmentbooking.databinding.ActivityAddPatientBinding
import com.clinic.appointmentbooking.util.Resource
import com.clinic.appointmentbooking.viewmodel.AppointmentViewModel

class AddPatientActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddPatientBinding
    private val viewModel: AppointmentViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddPatientBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Add Patient"

        setupObservers()

        binding.btnSavePatient.setOnClickListener {
            val name = binding.etPatientName.text.toString().trim()
            val phone = binding.etPhone.text.toString().trim()
            val age = binding.etAge.text.toString().trim()
            viewModel.addPatient(name, phone, age)
        }
    }

    private fun setupObservers() {
        viewModel.addPatientState.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> setFormEnabled(false)
                is Resource.Success -> {
                    setFormEnabled(true)
                    Toast.makeText(this, "Patient added successfully!", Toast.LENGTH_SHORT).show()
                    viewModel.resetAddPatientState()
                    clearForm()
                }
                is Resource.Error -> {
                    setFormEnabled(true)
                    Toast.makeText(this, resource.message, Toast.LENGTH_LONG).show()
                    viewModel.resetAddPatientState()
                }
                null -> setFormEnabled(true)
            }
        }
    }

    private fun clearForm() {
        binding.etPatientName.text?.clear()
        binding.etPhone.text?.clear()
        binding.etAge.text?.clear()
    }

    private fun setFormEnabled(enabled: Boolean) {
        binding.etPatientName.isEnabled = enabled
        binding.etPhone.isEnabled = enabled
        binding.etAge.isEnabled = enabled
        binding.btnSavePatient.isEnabled = enabled
        binding.progressBar.visibility = if (enabled) View.GONE else View.VISIBLE
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
