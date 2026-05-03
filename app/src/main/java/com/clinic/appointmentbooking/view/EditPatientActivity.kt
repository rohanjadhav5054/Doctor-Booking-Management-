package com.clinic.appointmentBooking.view

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.clinic.appointmentBooking.databinding.ActivityEditPatientBinding
import com.clinic.appointmentBooking.model.Patient
import com.clinic.appointmentBooking.util.Resource
import com.clinic.appointmentBooking.viewmodel.AppointmentViewModel

class EditPatientActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PATIENT_ID    = "extra_patient_id"
        const val EXTRA_PATIENT_NAME  = "extra_patient_name"
        const val EXTRA_PATIENT_PHONE = "extra_patient_phone"
        const val EXTRA_PATIENT_AGE   = "extra_patient_age"
    }

    private lateinit var binding: ActivityEditPatientBinding
    private val viewModel: AppointmentViewModel by viewModels()

    private var patientId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditPatientBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Pre-fill from Intent extras
        patientId = intent.getStringExtra(EXTRA_PATIENT_ID) ?: ""
        binding.etEditName.setText(intent.getStringExtra(EXTRA_PATIENT_NAME) ?: "")
        binding.etEditPhone.setText(intent.getStringExtra(EXTRA_PATIENT_PHONE) ?: "")
        binding.etEditAge.setText(intent.getStringExtra(EXTRA_PATIENT_AGE) ?: "")

        setupObservers()

        binding.btnUpdatePatient.setOnClickListener {
            val name  = binding.etEditName.text.toString().trim()
            val phone = binding.etEditPhone.text.toString().trim()
            val age   = binding.etEditAge.text.toString().trim()

            if (name.isBlank()) {
                binding.etEditName.error = "Name is required"
                return@setOnClickListener
            }
            if (phone.isBlank()) {
                binding.etEditPhone.error = "Phone is required"
                return@setOnClickListener
            }
            if (age.isBlank()) {
                binding.etEditAge.error = "Age is required"
                return@setOnClickListener
            }

            val updatedPatient = Patient(
                id    = patientId,
                name  = name,
                phone = phone,
                age   = age
            )
            viewModel.updatePatient(updatedPatient)
        }
    }

    private fun setupObservers() {
        viewModel.updatePatientState.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> setFormEnabled(false)
                is Resource.Success -> {
                    Toast.makeText(this, "✅ Patient updated!", Toast.LENGTH_SHORT).show()
                    viewModel.resetUpdatePatientState()
                    finish()
                }
                is Resource.Error -> {
                    setFormEnabled(true)
                    Toast.makeText(this, resource.message, Toast.LENGTH_LONG).show()
                    viewModel.resetUpdatePatientState()
                }
                null -> setFormEnabled(true)
            }
        }
    }

    private fun setFormEnabled(enabled: Boolean) {
        binding.etEditName.isEnabled  = enabled
        binding.etEditPhone.isEnabled = enabled
        binding.etEditAge.isEnabled   = enabled
        binding.btnUpdatePatient.isEnabled = enabled
        binding.progressBar.visibility = if (enabled) View.GONE else View.VISIBLE
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
