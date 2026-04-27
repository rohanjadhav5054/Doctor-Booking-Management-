package com.clinic.appointmentbooking.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clinic.appointmentbooking.model.Appointment
import com.clinic.appointmentbooking.model.Patient
import com.clinic.appointmentbooking.repository.FirebaseRepository
import com.clinic.appointmentbooking.util.ReportGenerator
import com.clinic.appointmentbooking.util.ReportType
import com.clinic.appointmentbooking.util.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class AppointmentViewModel : ViewModel() {

    private val repository = FirebaseRepository()

    // ────── Appointments ──────────────────────────────────────────────────────

    private val _appointments = MutableLiveData<Resource<List<Appointment>>>()
    val appointments: LiveData<Resource<List<Appointment>>> = _appointments

    private val _addAppointmentState = MutableLiveData<Resource<Unit>>()
    val addAppointmentState: LiveData<Resource<Unit>> = _addAppointmentState

    private val _updateStatusState = MutableLiveData<Resource<Unit>>()
    val updateStatusState: LiveData<Resource<Unit>> = _updateStatusState

    private val _updateNextVisitState = MutableLiveData<Resource<Unit>>()
    val updateNextVisitState: LiveData<Resource<Unit>> = _updateNextVisitState

    // ────── Patients ──────────────────────────────────────────────────────────

    private val _patients = MutableLiveData<Resource<List<Patient>>>()
    val patients: LiveData<Resource<List<Patient>>> = _patients

    private val _addPatientState = MutableLiveData<Resource<String>>()
    val addPatientState: LiveData<Resource<String>> = _addPatientState

    // ────── Reports ───────────────────────────────────────────────────────────

    private val _todayCount = MutableLiveData<Int>(0)
    val todayCount: LiveData<Int> = _todayCount

    private val _monthlyCount = MutableLiveData<Int>(0)
    val monthlyCount: LiveData<Int> = _monthlyCount

    private val _reportState = MutableLiveData<Resource<File>>()
    val reportState: LiveData<Resource<File>> = _reportState

    // Start real-time listener for appointments
    fun startListeningToAppointments() {
        _appointments.value = Resource.Loading
        viewModelScope.launch {
            repository.getAppointmentsFlow().collect { result ->
                _appointments.postValue(result)
                // Compute report counts whenever data changes
                if (result is Resource.Success) {
                    computeReports(result.data)
                }
            }
        }
    }

    // Start real-time listener for patients
    fun startListeningToPatients() {
        _patients.value = Resource.Loading
        viewModelScope.launch {
            repository.getPatientsFlow().collect { result ->
                _patients.postValue(result)
            }
        }
    }

    private fun computeReports(appointments: List<Appointment>) {
        val todayFmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val monthFmt = SimpleDateFormat("MM/yyyy", Locale.getDefault())
        val today = todayFmt.format(System.currentTimeMillis())
        val currentMonth = monthFmt.format(System.currentTimeMillis())

        // Count all appointments for today and this month (not just completed)
        _todayCount.postValue(appointments.count { it.date == today })
        _monthlyCount.postValue(appointments.count {
            // date format is dd/MM/yyyy → substring(3) gives MM/yyyy
            it.date.length >= 10 && it.date.substring(3) == currentMonth
        })
    }

    // Add Patient
    fun addPatient(name: String, phone: String, age: String) {
        if (name.isBlank()) {
            _addPatientState.value = Resource.Error("Patient name is required")
            return
        }
        if (phone.isBlank()) {
            _addPatientState.value = Resource.Error("Phone number is required")
            return
        }
        if (age.isBlank()) {
            _addPatientState.value = Resource.Error("Age is required")
            return
        }
        _addPatientState.value = Resource.Loading
        viewModelScope.launch {
            val patient = Patient(
                name = name.trim(),
                phone = phone.trim(),
                age = age.trim()
            )
            val result = repository.addPatient(patient)
            _addPatientState.postValue(result)
        }
    }

    // Add Appointment
    fun addAppointment(
        patientId: String,
        patientName: String,
        patientPhone: String,
        doctorName: String,
        date: String,
        time: String
    ) {
        if (patientId.isBlank()) {
            _addAppointmentState.value = Resource.Error("Please select a patient")
            return
        }
        if (doctorName.isBlank() || doctorName == "Select Doctor") {
            _addAppointmentState.value = Resource.Error("Please select a doctor")
            return
        }

        _addAppointmentState.value = Resource.Loading
        viewModelScope.launch {
            val appointment = Appointment(
                patientId = patientId,
                patientName = patientName.trim(),
                patientPhone = patientPhone.trim(),
                doctorName = doctorName,
                time = time,
                date = date,
                status = "pending",
                createdAt = System.currentTimeMillis()
            )
            val result = repository.addAppointment(appointment)
            _addAppointmentState.postValue(result)
        }
    }

    fun updateAppointmentStatus(appointmentId: String, status: String) {
        if (appointmentId.isBlank()) {
            _updateStatusState.value = Resource.Error("Invalid appointment ID")
            return
        }
        _updateStatusState.value = Resource.Loading
        viewModelScope.launch {
            val result = repository.updateAppointmentStatus(appointmentId, status)
            _updateStatusState.postValue(result)
        }
    }

    fun updateNextVisitDate(appointmentId: String, nextVisitDate: String) {
        if (appointmentId.isBlank()) {
            _updateNextVisitState.value = Resource.Error("Invalid appointment ID")
            return
        }
        _updateNextVisitState.value = Resource.Loading
        viewModelScope.launch {
            val result = repository.updateNextVisitDate(appointmentId, nextVisitDate)
            _updateNextVisitState.postValue(result)
        }
    }

    fun resetAddState() { _addAppointmentState.value = null }
    fun resetUpdateState() { _updateStatusState.value = null }
    fun resetNextVisitState() { _updateNextVisitState.value = null }
    fun resetAddPatientState() { _addPatientState.value = null }
    fun resetReportState() { _reportState.value = null }

    // ────── PDF Report Generation ──────────────────────────────────────────────

    fun generateTodayReport(context: Context, appointments: List<Appointment>) {
        _reportState.value = Resource.Loading
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                ReportGenerator.generate(context, appointments, ReportType.TODAY)
            }
            result.fold(
                onSuccess = { file -> _reportState.postValue(Resource.Success(file)) },
                onFailure = { e   -> _reportState.postValue(Resource.Error(e.message ?: "Failed to generate report")) }
            )
        }
    }

    fun generateMonthReport(context: Context, appointments: List<Appointment>) {
        _reportState.value = Resource.Loading
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                ReportGenerator.generate(context, appointments, ReportType.MONTH)
            }
            result.fold(
                onSuccess = { file -> _reportState.postValue(Resource.Success(file)) },
                onFailure = { e   -> _reportState.postValue(Resource.Error(e.message ?: "Failed to generate report")) }
            )
        }
    }
}
