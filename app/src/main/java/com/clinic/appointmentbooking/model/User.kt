package com.clinic.appointmentBooking.model

data class User(
    val uid: String = "",
    val email: String = "",
    val role: String = "" // "doctor" or "receptionist"
)
