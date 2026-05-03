package com.clinic.appointmentBooking.view

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.clinic.appointmentBooking.adapter.PatientListAdapter
import com.clinic.appointmentBooking.databinding.ActivityPatientListBinding
import com.clinic.appointmentBooking.model.Patient
import com.clinic.appointmentBooking.util.Resource
import com.clinic.appointmentBooking.viewmodel.AppointmentViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class PatientListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPatientListBinding
    private val viewModel: AppointmentViewModel by viewModels()
    private lateinit var adapter: PatientListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPatientListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setupRecyclerView()
        setupObservers()
        viewModel.startListeningToPatients()
    }

    private fun setupRecyclerView() {
        adapter = PatientListAdapter(
            onPatientClick = { patient -> openEditPatient(patient) },
            onDeleteClick  = { patient -> confirmDelete(patient) }
        )
        binding.rvPatients.apply {
            layoutManager = LinearLayoutManager(this@PatientListActivity)
            this.adapter = this@PatientListActivity.adapter
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            isNestedScrollingEnabled = false
        }
    }

    private fun setupObservers() {
        viewModel.patients.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.rvPatients.visibility = View.GONE
                    binding.layoutEmpty.visibility = View.GONE
                }
                is Resource.Success -> {
                    binding.progressBar.visibility = View.GONE
                    val list = resource.data
                    binding.tvListPatientCount.text = list.size.toString()
                    if (list.isEmpty()) {
                        binding.rvPatients.visibility = View.GONE
                        binding.layoutEmpty.visibility = View.VISIBLE
                    } else {
                        binding.layoutEmpty.visibility = View.GONE
                        binding.rvPatients.visibility = View.VISIBLE
                        adapter.submitList(list)
                    }
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this, resource.message, Toast.LENGTH_LONG).show()
                }
                null -> {}
            }
        }

        viewModel.deletePatientState.observe(this) { resource ->
            when (resource) {
                is Resource.Success -> {
                    Toast.makeText(this, "✅ Patient deleted", Toast.LENGTH_SHORT).show()
                    viewModel.resetDeletePatientState()
                }
                is Resource.Error -> {
                    Toast.makeText(this, resource.message, Toast.LENGTH_LONG).show()
                    viewModel.resetDeletePatientState()
                }
                else -> {}
            }
        }
    }

    private fun openEditPatient(patient: Patient) {
        val intent = Intent(this, EditPatientActivity::class.java).apply {
            putExtra(EditPatientActivity.EXTRA_PATIENT_ID,    patient.id)
            putExtra(EditPatientActivity.EXTRA_PATIENT_NAME,  patient.name)
            putExtra(EditPatientActivity.EXTRA_PATIENT_PHONE, patient.phone)
            putExtra(EditPatientActivity.EXTRA_PATIENT_AGE,   patient.age)
        }
        startActivity(intent)
    }

    private fun confirmDelete(patient: Patient) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Patient")
            .setMessage("Delete \"${patient.name}\"? This cannot be undone.")
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deletePatient(patient.id)
            }
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
