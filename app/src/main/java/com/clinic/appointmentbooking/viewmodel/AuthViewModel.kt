package com.clinic.appointmentbooking.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clinic.appointmentbooking.repository.FirebaseRepository
import com.clinic.appointmentbooking.util.Resource
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {

    private val repository = FirebaseRepository()

    private val _loginState = MutableLiveData<Resource<String>>()
    val loginState: LiveData<Resource<String>> = _loginState

    private val _userRole = MutableLiveData<Resource<String>>()
    val userRole: LiveData<Resource<String>> = _userRole

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _loginState.value = Resource.Error("Email and password cannot be empty")
            return
        }
        _loginState.value = Resource.Loading
        viewModelScope.launch {
            val result = repository.login(email, password)
            _loginState.value = result
        }
    }

    fun fetchUserRole(uid: String) {
        _userRole.value = Resource.Loading
        viewModelScope.launch {
            val result = repository.getUserRole(uid)
            _userRole.value = result
        }
    }

    fun getCurrentUser() = repository.getCurrentUser()

    fun logout() {
        repository.logout()
    }
}
