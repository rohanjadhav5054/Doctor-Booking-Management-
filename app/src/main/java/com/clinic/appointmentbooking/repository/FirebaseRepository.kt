package com.clinic.appointmentBooking.repository

import com.clinic.appointmentBooking.model.Appointment
import com.clinic.appointmentBooking.model.Patient
import com.clinic.appointmentBooking.model.User
import com.clinic.appointmentBooking.util.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import android.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseRepository {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val database: DatabaseReference = FirebaseDatabase
        .getInstance("https://clinicbook-9e723-default-rtdb.asia-southeast1.firebasedatabase.app")
        .reference

    // ─── Auth ─────────────────────────────────────────────────────────────────

    suspend fun login(email: String, password: String): Resource<String> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            Resource.Success(result.user?.uid ?: "")
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Login failed")
        }
    }

    suspend fun getUserRole(uid: String): Resource<String> {
        return try {
            val snapshot = database.child("users").child(uid).get().await()
            val role = snapshot.child("role").getValue(String::class.java) ?: ""
            if (role.isNotEmpty()) Resource.Success(role)
            else Resource.Error("Role not found for user")
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to fetch role")
        }
    }

    fun getCurrentUser() = auth.currentUser

    fun logout() = auth.signOut()

    // ─── Patients ─────────────────────────────────────────────────────────────

    suspend fun addPatient(patient: Patient): Resource<String> {
        return try {
            val ref = database.child("patients").push()
            val patientWithId = patient.copy(id = ref.key ?: "")
            ref.setValue(patientWithId).await()
            Resource.Success(patientWithId.id)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to add patient")
        }
    }

    fun getPatientsFlow(): Flow<Resource<List<Patient>>> = callbackFlow {
        val ref = database.child("patients")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<Patient>()
                for (child in snapshot.children) {
                    val patient = child.getValue(Patient::class.java)
                    if (patient != null) list.add(patient)
                }
                list.sortBy { it.name }
                trySend(Resource.Success(list))
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(Resource.Error(error.message))
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun updatePatient(patient: Patient): Resource<Unit> {
        return try {
            database.child("patients").child(patient.id).setValue(patient).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to update patient")
        }
    }

    suspend fun deletePatient(patientId: String): Resource<Unit> {
        return try {
            database.child("patients").child(patientId).removeValue().await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to delete patient")
        }
    }

    // ─── Appointments ─────────────────────────────────────────────────────────

    suspend fun addAppointment(appointment: Appointment): Resource<Unit> {
        return try {
            val ref = database.child("appointments").push()
            val appointmentWithId = appointment.copy(id = ref.key ?: "")
            ref.setValue(appointmentWithId).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to add appointment")
        }
    }

    /**
     * Safely deserializes one appointment child from Firebase.
     *
     * Older records stored `instructions` as a native JSON array (ArrayList).
     * Firebase's CustomClassMapper expects a Map and throws DatabaseException
     * when it finds an ArrayList — crashing the whole app.
     *
     * Fix: attempt normal deserialization first; on failure, read each field
     * individually and convert the `instructions` array to an indexed Map.
     */
    private fun safeReadAppointment(child: DataSnapshot): Appointment? {
        return try {
            child.getValue(Appointment::class.java)
        } catch (e: com.google.firebase.database.DatabaseException) {
            Log.w("FirebaseRepository",
                "Legacy array-instructions detected for key=${child.key}; converting. (${e.message})")
            try {
                // Build an indexed map from the legacy ArrayList
                val instrNode = child.child("instructions")
                val instrMap = mutableMapOf<String, String>()
                instrNode.children.forEachIndexed { i, item ->
                    val v = item.getValue(String::class.java)
                    if (!v.isNullOrBlank()) instrMap[i.toString()] = v
                }
                // Also handle plain string values stored directly under an index key
                if (instrMap.isEmpty()) {
                    instrNode.children.forEach { item ->
                        val key = item.key ?: return@forEach
                        val v   = item.getValue(String::class.java)
                        if (!v.isNullOrBlank()) instrMap[key] = v
                    }
                }
                Appointment(
                    id            = child.child("id").getValue(String::class.java) ?: "",
                    patientId     = child.child("patientId").getValue(String::class.java) ?: "",
                    patientName   = child.child("patientName").getValue(String::class.java) ?: "",
                    patientPhone  = child.child("patientPhone").getValue(String::class.java) ?: "",
                    doctorName    = child.child("doctorName").getValue(String::class.java) ?: "",
                    time          = child.child("time").getValue(String::class.java) ?: "",
                    date          = child.child("date").getValue(String::class.java) ?: "",
                    status        = child.child("status").getValue(String::class.java) ?: "pending",
                    nextVisitDate = child.child("nextVisitDate").getValue(String::class.java) ?: "",
                    createdAt     = child.child("createdAt").getValue(Long::class.java) ?: System.currentTimeMillis(),
                    instructions  = instrMap
                )
            } catch (inner: Exception) {
                Log.e("FirebaseRepository", "Could not recover appointment key=${child.key}: ${inner.message}")
                null
            }
        }
    }

    fun getAppointmentsFlow(): Flow<Resource<List<Appointment>>> = callbackFlow {
        val ref = database.child("appointments")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<Appointment>()
                for (child in snapshot.children) {
                    val appt = safeReadAppointment(child)  // ← crash-safe read
                    if (appt != null) list.add(appt)
                }
                // Sort by createdAt descending
                list.sortByDescending { it.createdAt }
                trySend(Resource.Success(list))
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(Resource.Error(error.message))
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    /**
     * One-time read that always fetches fresh data from the Firebase server.
     * Uses .get() which bypasses the local disk cache, ensuring report data
     * includes the very latest instructions or status changes.
     */
    suspend fun getFreshAppointments(): Resource<List<Appointment>> {
        return try {
            Log.d("ReportData", "► getFreshAppointments(): fetching from Firebase server…")
            val snapshot = database.child("appointments").get().await()
            val list = mutableListOf<Appointment>()
            for (child in snapshot.children) {
                val appt = safeReadAppointment(child)  // ← crash-safe read
                if (appt != null) list.add(appt)
            }
            list.sortByDescending { it.createdAt }
            Log.d("ReportData", "✅ getFreshAppointments(): received ${list.size} appointments from server")
            list.forEachIndexed { i, appt ->
                Log.d("ReportData",
                    "  [$i] id=${appt.id.takeLast(6)} | patient=${appt.patientName} " +
                    "| instructions=${appt.instructionList()}")
            }
            Resource.Success(list)
        } catch (e: Exception) {
            Log.e("ReportData", "❌ getFreshAppointments() failed: ${e.message}")
            Resource.Error(e.message ?: "Failed to fetch appointments")
        }
    }

    suspend fun updateAppointmentStatus(appointmentId: String, status: String): Resource<Unit> {
        return try {
            database.child("appointments")
                .child(appointmentId)
                .child("status")
                .setValue(status)
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to update status")
        }
    }

    suspend fun updateNextVisitDate(appointmentId: String, nextVisitDate: String): Resource<Unit> {
        return try {
            database.child("appointments")
                .child(appointmentId)
                .child("nextVisitDate")
                .setValue(nextVisitDate)
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to update next visit date")
        }
    }

    /** Saves doctor instructions as an indexed map (Firebase-compatible array). */
    suspend fun updateInstructions(appointmentId: String, instructions: List<String>): Resource<Unit> {
        return try {
            // Convert list to {"0":"X-Ray","1":"Lab"} — Firebase's indexed format
            val map = instructions.mapIndexed { i, v -> i.toString() to v }.toMap()
            database.child("appointments")
                .child(appointmentId)
                .child("instructions")
                .setValue(map)
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to save instructions")
        }
    }

    // ─── User Management ──────────────────────────────────────────────────────

    suspend fun createUserProfile(uid: String, email: String, role: String): Resource<Unit> {
        return try {
            val user = mapOf("email" to email, "role" to role)
            database.child("users").child(uid).setValue(user).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to create profile")
        }
    }
}
