package com.clinic.appointmentbooking.view

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.clinic.appointmentbooking.R
import com.clinic.appointmentbooking.adapter.PatientSearchAdapter
import com.clinic.appointmentbooking.databinding.ActivityBookAppointmentBinding
import com.clinic.appointmentbooking.model.Patient
import com.clinic.appointmentbooking.util.Resource
import com.clinic.appointmentbooking.viewmodel.AppointmentViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class BookAppointmentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBookAppointmentBinding
    private val viewModel: AppointmentViewModel by viewModels()
    private lateinit var patientAdapter: PatientSearchAdapter

    private var selectedPatient: Patient? = null
    private var selectedDate = ""
    private var selectedTime = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookAppointmentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Book Appointment"

        // Auto-set current date & time
        val now = Calendar.getInstance()
        val dateFmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val timeFmt = SimpleDateFormat("hh:mm a", Locale.getDefault())
        selectedDate = dateFmt.format(now.time)
        selectedTime = timeFmt.format(now.time)
        binding.tvSelectedDate.text = selectedDate
        binding.tvSelectedTime.text = selectedTime

        setupDoctorSpinner()
        setupPatientSearch()
        setupObservers()
        setupClickListeners()
        viewModel.startListeningToPatients()

        // Handle Rebook pre-fill (called from ViewAppointments → Rebook)
        handleRebookIntent()
    }

    /**
     * If launched from rebook, pre-fill patient and next-visit date
     */
    private fun handleRebookIntent() {
        val rebookPatientId = intent.getStringExtra("REBOOK_PATIENT_ID") ?: return
        val rebookPatientName = intent.getStringExtra("REBOOK_PATIENT_NAME") ?: return
        val rebookPatientPhone = intent.getStringExtra("REBOOK_PATIENT_PHONE") ?: ""
        val rebookDate = intent.getStringExtra("REBOOK_DATE") ?: ""

        // Pre-fill the search field
        binding.etPatientSearch.setText(rebookPatientName)
        binding.tvPatientInfo.text = "📞 $rebookPatientPhone  (Rebook)"
        binding.layoutPatientInfo.visibility = View.VISIBLE
        binding.dividerPatientInfo.visibility = View.VISIBLE

        // Create a temporary Patient object
        selectedPatient = Patient(
            id = rebookPatientId,
            name = rebookPatientName,
            phone = rebookPatientPhone
        )

        // Pre-fill next visit date if available
        if (rebookDate.isNotEmpty()) {
            selectedDate = rebookDate
            binding.tvSelectedDate.text = selectedDate
        }
    }

    private fun setupDoctorSpinner() {
        val doctors = resources.getStringArray(R.array.doctors_list)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, doctors)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerDoctor.adapter = adapter
    }

    private fun setupPatientSearch() {
        patientAdapter = PatientSearchAdapter { patient ->
            selectedPatient = patient
            binding.etPatientSearch.setText(patient.name)
            binding.tvPatientInfo.text = "${patient.name}  •  📞 ${patient.phone}  •  Age: ${patient.age}"
            binding.dividerPatientInfo.visibility = View.VISIBLE
            binding.layoutPatientInfo.visibility = View.VISIBLE
            binding.rvPatientList.visibility = View.GONE
        }
        binding.rvPatientList.apply {
            layoutManager = LinearLayoutManager(this@BookAppointmentActivity)
            adapter = patientAdapter
        }

        binding.etPatientSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()
                if (query.isEmpty()) {
                    binding.rvPatientList.visibility = View.GONE
                    selectedPatient = null
                    binding.dividerPatientInfo.visibility = View.GONE
                    binding.layoutPatientInfo.visibility = View.GONE
                } else {
                    filterPatients(query)
                    binding.rvPatientList.visibility = View.VISIBLE
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun filterPatients(query: String) {
        val allPatients = (viewModel.patients.value as? Resource.Success)?.data ?: return
        val filtered = allPatients.filter {
            it.name.contains(query, ignoreCase = true) ||
            it.phone.contains(query, ignoreCase = true)
        }
        patientAdapter.submitList(filtered)
    }

    private fun setupClickListeners() {
        binding.btnEditDate.setOnClickListener { showDatePicker() }
        binding.btnEditTime.setOnClickListener { showTimePicker() }
        binding.btnBookAppointment.setOnClickListener { submitAppointment() }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                selectedDate = String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year)
                binding.tvSelectedDate.text = selectedDate
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
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
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            false
        ).show()
    }

    private fun submitAppointment() {
        val patient = selectedPatient
        if (patient == null) {
            Toast.makeText(this, "Please select a patient from the list", Toast.LENGTH_SHORT).show()
            return
        }
        val doctorName = binding.spinnerDoctor.selectedItem?.toString() ?: ""
        if (doctorName == "Select Doctor" || doctorName.isBlank()) {
            Toast.makeText(this, "Please select a doctor", Toast.LENGTH_SHORT).show()
            return
        }
        viewModel.addAppointment(
            patientId = patient.id,
            patientName = patient.name,
            patientPhone = patient.phone,
            doctorName = doctorName,
            date = selectedDate,
            time = selectedTime
        )
    }

    private fun setupObservers() {
        viewModel.patients.observe(this) { resource ->
            if (resource is Resource.Success) {
                val query = binding.etPatientSearch.text.toString().trim()
                if (query.isNotEmpty()) filterPatients(query)
            }
        }

        viewModel.addAppointmentState.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> setFormEnabled(false)
                is Resource.Success -> {
                    setFormEnabled(true)
                    Toast.makeText(this, "Appointment booked successfully!", Toast.LENGTH_SHORT).show()
                    viewModel.resetAddState()
                    finish()
                }
                is Resource.Error -> {
                    setFormEnabled(true)
                    Toast.makeText(this, resource.message, Toast.LENGTH_LONG).show()
                    viewModel.resetAddState()
                }
                null -> setFormEnabled(true)
            }
        }
    }

    private fun setFormEnabled(enabled: Boolean) {
        binding.etPatientSearch.isEnabled = enabled
        binding.spinnerDoctor.isEnabled = enabled
        binding.btnBookAppointment.isEnabled = enabled
        binding.progressBar.visibility = if (enabled) View.GONE else View.VISIBLE
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
