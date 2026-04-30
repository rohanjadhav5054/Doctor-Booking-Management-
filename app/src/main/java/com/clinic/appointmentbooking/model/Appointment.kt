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
    val createdAt: Long = System.currentTimeMillis(),
    /** Firebase stores arrays as {"0":"X-Ray","1":"Lab"} — keep as Map for safe deserialisation. */
    val instructions: Map<String, String> = emptyMap()
) {
    /** Converts the Firebase-indexed map back to a plain ordered list. */
    fun instructionList(): List<String> =
        instructions.entries.sortedBy { it.key }.map { it.value }.filter { it.isNotBlank() }
}
