package com.clinic.appointmentbooking.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clinic.appointmentbooking.model.Appointment
import com.clinic.appointmentbooking.repository.FirebaseRepository
import com.clinic.appointmentbooking.util.Resource
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class AppointmentViewModel : ViewModel() {

    private val repository = FirebaseRepository()

    private val _appointments = MutableLiveData<Resource<List<Appointment>>>()
    val appointments: LiveData<Resource<List<Appointment>>> = _appointments

    private val _addAppointmentState = MutableLiveData<Resource<Unit>>()
    val addAppointmentState: LiveData<Resource<Unit>> = _addAppointmentState

    private val _updateStatusState = MutableLiveData<Resource<Unit>>()
    val updateStatusState: LiveData<Resource<Unit>> = _updateStatusState

    // Start real-time listener for appointments
    fun startListeningToAppointments() {
        _appointments.value = Resource.Loading
        viewModelScope.launch {
            repository.getAppointmentsFlow().collect { result ->
                _appointments.postValue(result)
            }
        }
    }

    fun addAppointment(
        patientName: String,
        doctorName: String,
        time: String,
        date: String
    ) {
        // Validation
        if (patientName.isBlank()) {
            _addAppointmentState.value = Resource.Error("Patient name is required")
            return
        }
        if (doctorName.isBlank() || doctorName == "Select Doctor") {
            _addAppointmentState.value = Resource.Error("Please select a doctor")
            return
        }
        if (time.isBlank()) {
            _addAppointmentState.value = Resource.Error("Please select a time")
            return
        }
        if (date.isBlank()) {
            _addAppointmentState.value = Resource.Error("Please select a date")
            return
        }

        _addAppointmentState.value = Resource.Loading
        viewModelScope.launch {
            val appointment = Appointment(
                patientName = patientName.trim(),
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

    fun resetAddState() {
        _addAppointmentState.value = null
    }

    fun resetUpdateState() {
        _updateStatusState.value = null
    }
}
