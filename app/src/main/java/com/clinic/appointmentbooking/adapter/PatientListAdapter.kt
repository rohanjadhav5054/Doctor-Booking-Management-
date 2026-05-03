package com.clinic.appointmentBooking.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.clinic.appointmentBooking.databinding.ItemPatientBinding
import com.clinic.appointmentBooking.model.Patient

class PatientListAdapter(
    private val onPatientClick: (Patient) -> Unit,
    private val onDeleteClick: (Patient) -> Unit
) : ListAdapter<Patient, PatientListAdapter.PatientViewHolder>(DiffCallback()) {

    inner class PatientViewHolder(
        private val binding: ItemPatientBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(patient: Patient) {
            binding.tvPatientItemName.text = patient.name
            binding.tvPatientItemPhone.text = "📞 ${patient.phone}"
            binding.tvPatientItemAge.text = "Age: ${patient.age}"

            binding.root.setOnClickListener { onPatientClick(patient) }
            binding.btnDeletePatient.setOnClickListener { onDeleteClick(patient) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PatientViewHolder {
        val binding = ItemPatientBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return PatientViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PatientViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DiffCallback : DiffUtil.ItemCallback<Patient>() {
        override fun areItemsTheSame(oldItem: Patient, newItem: Patient) =
            oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Patient, newItem: Patient) =
            oldItem == newItem
    }
}
