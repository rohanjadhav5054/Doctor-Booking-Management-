package com.clinic.appointmentbooking.repository

import com.clinic.appointmentbooking.model.Appointment
import com.clinic.appointmentbooking.model.User
import com.clinic.appointmentbooking.util.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
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
