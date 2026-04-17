package com.clinic.appointmentbooking.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.clinic.appointmentbooking.databinding.ItemPatientSearchBinding
import com.clinic.appointmentbooking.model.Patient

class PatientSearchAdapter(
    private val onPatientSelected: (Patient) -> Unit
) : ListAdapter<Patient, PatientSearchAdapter.PatientViewHolder>(DiffCallback()) {

    inner class PatientViewHolder(
        private val binding: ItemPatientSearchBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(patient: Patient) {
            binding.tvPatientName.text = patient.name
            binding.tvPatientInfo.text = "📞 ${patient.phone}  •  Age: ${patient.age}"
            binding.root.setOnClickListener { onPatientSelected(patient) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PatientViewHolder {
        val binding = ItemPatientSearchBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return PatientViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PatientViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DiffCallback : DiffUtil.ItemCallback<Patient>() {
        override fun areItemsTheSame(oldItem: Patient, newItem: Patient) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Patient, newItem: Patient) = oldItem == newItem
    }
}
