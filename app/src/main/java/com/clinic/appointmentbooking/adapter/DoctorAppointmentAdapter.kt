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
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/** All selectable instruction options shown in the dialog. */
private val INSTRUCTION_OPTIONS = listOf("X-Ray", "Lab", "Blood Test", "ECG", "Physio", "Other")

class DoctorAppointmentAdapter(
    private val onMarkCompleted: (Appointment) -> Unit,
    private val onSetNextVisit: (Appointment) -> Unit,
    private val onPatientClick: (Appointment) -> Unit = {},
    private val onUpdateInstructions: (Appointment, List<String>) -> Unit = { _, _ -> }
) : ListAdapter<Appointment, DoctorAppointmentAdapter.AppointmentViewHolder>(DiffCallback()) {

    inner class AppointmentViewHolder(
        private val binding: ItemAppointmentDoctorBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(appointment: Appointment) {
            val ctx = binding.root.context

            // Card-level click → open Patient Details
            binding.root.setOnClickListener { onPatientClick(appointment) }

            // Patient info
            binding.tvPatientName.text = appointment.patientName
            val docName = appointment.doctorName.trim()
            binding.tvDoctor.text = if (docName.startsWith("Dr.", ignoreCase = true)) docName else "Dr. $docName"
            binding.tvTime.text = "${appointment.date}  •  ${appointment.time}"

            // Phone
            if (appointment.patientPhone.isNotEmpty()) {
                binding.tvPatientPhone.text = "📞 ${appointment.patientPhone}"
                binding.tvPatientPhone.visibility = View.VISIBLE
            } else {
                binding.tvPatientPhone.visibility = View.GONE
            }

            // Status chip
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

            // ── Instructions chips ────────────────────────────────────────────
            val instrList = appointment.instructionList()
            if (instrList.isNotEmpty()) {
                binding.layoutInstructions.visibility = View.VISIBLE
                binding.chipGroupInstructions.removeAllViews()
                instrList.forEach { label ->
                    val chip = Chip(ctx).apply {
                        text = label
                        isClickable = false
                        isCheckable = false
                        chipBackgroundColor = ContextCompat.getColorStateList(ctx, R.color.primary_light_selector)
                        setTextColor(ContextCompat.getColor(ctx, R.color.primary))
                    }
                    binding.chipGroupInstructions.addView(chip)
                }
            } else {
                binding.layoutInstructions.visibility = View.GONE
                binding.chipGroupInstructions.removeAllViews()
            }

            // ── Action buttons ────────────────────────────────────────────────
            binding.btnMarkCompleted.isEnabled = !isCompleted
            binding.btnMarkCompleted.alpha = if (!isCompleted) 1f else 0.45f
            binding.btnMarkCompleted.setOnClickListener {
                if (!isCompleted) onMarkCompleted(appointment)
            }

            binding.btnSetNextVisit.visibility = if (isCompleted) View.VISIBLE else View.GONE
            binding.btnSetNextVisit.setOnClickListener { onSetNextVisit(appointment) }

            // "📋 Instructions" button → open multi-select dialog
            binding.btnAddInstructions.setOnClickListener {
                showInstructionsDialog(ctx, appointment)
            }
        }

        /** Shows a multi-select dialog to choose instructions and save them. */
        private fun showInstructionsDialog(ctx: android.content.Context, appointment: Appointment) {
            val options  = INSTRUCTION_OPTIONS.toTypedArray()
            val current  = appointment.instructionList()
            val checked  = BooleanArray(options.size) { options[it] in current }

            MaterialAlertDialogBuilder(ctx)
                .setTitle("Doctor Instructions")
                .setMultiChoiceItems(options, checked) { _, which, isChecked ->
                    checked[which] = isChecked
                }
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save") { _, _ ->
                    val selected = options.filterIndexed { i, _ -> checked[i] }
                    onUpdateInstructions(appointment, selected)
                }
                .show()
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
