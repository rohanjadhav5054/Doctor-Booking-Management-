package com.clinic.appointmentbooking.model

data class Appointment(
    val id: String = "",
    val patientId: String = "",
    val patientName: String = "",
    val patientPhone: String = "",
    val doctorName: String = "",
    val time: String = "",
    val date: String = "",
    val status: String = "pending",
    val nextVisitDate: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
