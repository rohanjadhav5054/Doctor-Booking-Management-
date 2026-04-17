package com.clinic.appointmentbooking.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.clinic.appointmentbooking.R
import com.clinic.appointmentbooking.databinding.ItemAppointmentDoctorBinding
import com.clinic.appointmentbooking.model.Appointment

class DoctorAppointmentAdapter(
    private val onMarkCompleted: (Appointment) -> Unit,
    private val onSetNextVisit: (Appointment) -> Unit
) : ListAdapter<Appointment, DoctorAppointmentAdapter.AppointmentViewHolder>(DiffCallback()) {

    inner class AppointmentViewHolder(
        private val binding: ItemAppointmentDoctorBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(appointment: Appointment) {
            val ctx = binding.root.context

            // Patient info
            binding.tvPatientName.text = appointment.patientName
            binding.tvDoctor.text = "Dr. ${appointment.doctorName}"
            binding.tvTime.text = "${appointment.date}  •  ${appointment.time}"

            // Phone
            if (appointment.patientPhone.isNotEmpty()) {
                binding.tvPatientPhone.text = "📞 ${appointment.patientPhone}"
                binding.tvPatientPhone.visibility = View.VISIBLE
            } else {
                binding.tvPatientPhone.visibility = View.GONE
            }

            // Status chip — colored text on tinted background
            val isCompleted = appointment.status.equals("completed", ignoreCase = true)
            if (isCompleted) {
                binding.tvStatus.text = "Completed"
                binding.tvStatus.setTextColor(ContextCompat.getColor(ctx, R.color.success))
                binding.tvStatus.setBackgroundResource(R.drawable.bg_status_completed)
            } else {
                binding.tvStatus.text = "Pending"
                binding.tvStatus.setTextColor(ContextCompat.getColor(ctx, R.color.warning))
                binding.tvStatus.setBackgroundResource(R.drawable.bg_status_pending)
            }

            // Next Visit
            if (appointment.nextVisitDate.isNotEmpty()) {
                binding.tvNextVisit.text = "Next Visit: ${appointment.nextVisitDate}"
                binding.layoutNextVisit.visibility = View.VISIBLE
            } else {
                binding.layoutNextVisit.visibility = View.GONE
            }

            // Buttons
            binding.btnMarkCompleted.isEnabled = !isCompleted
            binding.btnMarkCompleted.alpha = if (!isCompleted) 1f else 0.45f
            binding.btnMarkCompleted.setOnClickListener {
                if (!isCompleted) onMarkCompleted(appointment)
            }

            // "Next Visit" button — only visible for completed
            binding.btnSetNextVisit.visibility = if (isCompleted) View.VISIBLE else View.GONE
            binding.btnSetNextVisit.setOnClickListener { onSetNextVisit(appointment) }
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
