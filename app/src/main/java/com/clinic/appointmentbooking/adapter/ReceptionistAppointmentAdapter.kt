package com.clinic.appointmentbooking.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.clinic.appointmentbooking.R
import com.clinic.appointmentbooking.databinding.ItemAppointmentReceptionistBinding
import com.clinic.appointmentbooking.model.Appointment

class ReceptionistAppointmentAdapter :
    ListAdapter<Appointment, ReceptionistAppointmentAdapter.ViewHolder>(DiffCallback()) {

    inner class ViewHolder(
        private val binding: ItemAppointmentReceptionistBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(appointment: Appointment) {
            binding.tvPatientName.text = appointment.patientName
            binding.tvDoctor.text = "Dr. ${appointment.doctorName}"
            binding.tvDateTime.text = "${appointment.date}  •  ${appointment.time}"
            binding.tvStatus.text = appointment.status.replaceFirstChar { it.uppercase() }
            binding.tvStatus.setBackgroundResource(
                if (appointment.status.lowercase() == "pending")
                    R.drawable.bg_status_pending
                else
                    R.drawable.bg_status_completed
            )
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAppointmentReceptionistBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DiffCallback : DiffUtil.ItemCallback<Appointment>() {
        override fun areItemsTheSame(oldItem: Appointment, newItem: Appointment) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Appointment, newItem: Appointment) =
            oldItem == newItem
    }
}
