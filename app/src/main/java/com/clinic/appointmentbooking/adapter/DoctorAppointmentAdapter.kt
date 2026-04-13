package com.clinic.appointmentbooking.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.clinic.appointmentbooking.R
import com.clinic.appointmentbooking.databinding.ItemAppointmentDoctorBinding
import com.clinic.appointmentbooking.model.Appointment

class DoctorAppointmentAdapter(
    private val onMarkCompleted: (Appointment) -> Unit
) : ListAdapter<Appointment, DoctorAppointmentAdapter.AppointmentViewHolder>(DiffCallback()) {

    inner class AppointmentViewHolder(
        private val binding: ItemAppointmentDoctorBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(appointment: Appointment) {
            binding.tvPatientName.text = appointment.patientName
            binding.tvDoctor.text = "Dr. ${appointment.doctorName}"
            binding.tvTime.text = "${appointment.date}  •  ${appointment.time}"

            val isPending = appointment.status.lowercase() == "pending"
            binding.tvStatus.text = if (isPending) "Pending" else "Completed"
            binding.tvStatus.setBackgroundResource(
                if (isPending) R.drawable.bg_status_pending else R.drawable.bg_status_completed
            )

            binding.btnMarkCompleted.isEnabled = isPending
            binding.btnMarkCompleted.alpha = if (isPending) 1f else 0.4f
            binding.btnMarkCompleted.setOnClickListener {
                if (isPending) onMarkCompleted(appointment)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppointmentViewHolder {
        val binding = ItemAppointmentDoctorBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return AppointmentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AppointmentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DiffCallback : DiffUtil.ItemCallback<Appointment>() {
        override fun areItemsTheSame(oldItem: Appointment, newItem: Appointment) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Appointment, newItem: Appointment) =
            oldItem == newItem
    }
}
