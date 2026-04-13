package com.clinic.appointmentbooking.model

data class Appointment(
    val id: String = "",
    val patientName: String = "",
    val doctorName: String = "",
    val time: String = "",
    val date: String = "",
    val status: String = "pending",
    val createdAt: Long = System.currentTimeMillis()
)
