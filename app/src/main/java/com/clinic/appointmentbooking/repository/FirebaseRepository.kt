package com.clinic.appointmentbooking.repository

import com.clinic.appointmentbooking.model.Appointment
import com.clinic.appointmentbooking.model.Patient
import com.clinic.appointmentbooking.model.User
import com.clinic.appointmentbooking.util.Resource
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

    fun getAppointmentsFlow(): Flow<Resource<List<Appointment>>> = callbackFlow {
        val ref = database.child("appointments")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<Appointment>()
                for (child in snapshot.children) {
                    val appt = child.getValue(Appointment::class.java)
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
                val appt = child.getValue(Appointment::class.java)
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
