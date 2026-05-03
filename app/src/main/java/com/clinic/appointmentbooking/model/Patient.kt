package com.clinic.appointmentBooking.model

data class Patient(
    val id: String = "",
    val name: String = "",
    val phone: String = "",
    val age: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
