package com.clinic.appointmentbooking.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.clinic.appointmentbooking.R
import com.clinic.appointmentbooking.databinding.ItemAppointmentReceptionistBinding
import com.clinic.appointmentbooking.model.Appointment
import com.google.android.material.chip.Chip

class ReceptionistAppointmentAdapter(
    private val onRebook: (Appointment) -> Unit = {}
) : ListAdapter<Appointment, ReceptionistAppointmentAdapter.ViewHolder>(DiffCallback()) {

    inner class ViewHolder(
        private val binding: ItemAppointmentReceptionistBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(appointment: Appointment) {
            val ctx = binding.root.context

            // ── Basic info ────────────────────────────────────────────────
            binding.tvPatientName.text = appointment.patientName
            val docName = appointment.doctorName.trim()
            binding.tvDoctor.text = if (docName.startsWith("Dr.", ignoreCase = true)) docName else "Dr. $docName"
            binding.tvDateTime.text    = "${appointment.date}  •  ${appointment.time}"

            // ── Status chip ──────────────────────────────────────────────
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

            // ── Next visit + Rebook row ───────────────────────────────────
            if (appointment.nextVisitDate.isNotEmpty()) {
                binding.tvNextVisit.text = "Next Visit: ${appointment.nextVisitDate}"
                binding.layoutNextVisitRow.visibility = View.VISIBLE
                binding.btnRebook.setOnClickListener { onRebook(appointment) }
            } else {
                binding.layoutNextVisitRow.visibility = View.GONE
            }

            // ── Instructions (read-only) ──────────────────────────────────
            val instrList = appointment.instructionList()
            if (instrList.isNotEmpty()) {
                binding.layoutInstructions.visibility = View.VISIBLE
                binding.chipGroupInstructions.removeAllViews()
                instrList.forEach { label ->
                    val chip = Chip(ctx).apply {
                        text = label
                        isClickable = false
                        isCheckable = false
                        chipBackgroundColor =
                            ContextCompat.getColorStateList(ctx, R.color.primary_light_selector)
                        setTextColor(ContextCompat.getColor(ctx, R.color.primary))
                    }
                    binding.chipGroupInstructions.addView(chip)
                }
            } else {
                binding.layoutInstructions.visibility = View.GONE
                binding.chipGroupInstructions.removeAllViews()
            }
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
