package com.clinic.appointmentbooking.view

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.clinic.appointmentbooking.R
import com.clinic.appointmentbooking.databinding.ActivityPatientDetailsBinding

class PatientDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPatientDetailsBinding

    companion object {
        const val EXTRA_NAME       = "extra_name"
        const val EXTRA_PHONE      = "extra_phone"
        const val EXTRA_AGE        = "extra_age"
        const val EXTRA_DOCTOR     = "extra_doctor"
        const val EXTRA_DATE       = "extra_date"
        const val EXTRA_TIME       = "extra_time"
        const val EXTRA_STATUS     = "extra_status"
        const val EXTRA_NEXT_VISIT = "extra_next_visit"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPatientDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ── Toolbar with back navigation ─────────────────────────────────
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        populateData()
    }

    private fun populateData() {
        val name      = intent.getStringExtra(EXTRA_NAME)      ?: "—"
        val phone     = intent.getStringExtra(EXTRA_PHONE)     ?: "—"
        val age       = intent.getStringExtra(EXTRA_AGE)       ?: "—"
        val doctor    = intent.getStringExtra(EXTRA_DOCTOR)    ?: "—"
        val date      = intent.getStringExtra(EXTRA_DATE)      ?: "—"
        val time      = intent.getStringExtra(EXTRA_TIME)      ?: "—"
        val status    = intent.getStringExtra(EXTRA_STATUS)    ?: "pending"
        val nextVisit = intent.getStringExtra(EXTRA_NEXT_VISIT) ?: ""

        // Hero card ──────────────────────────────────────────────────────
        binding.tvHeroName.text = name

        val isCompleted = status.equals("completed", ignoreCase = true)
        if (isCompleted) {
            binding.tvHeroStatus.text = "✔ Completed"
            binding.tvHeroStatus.setTextColor(ContextCompat.getColor(this, R.color.success))
            binding.tvHeroStatus.setBackgroundResource(R.drawable.bg_status_completed)
        } else {
            binding.tvHeroStatus.text = "⏳ Pending"
            binding.tvHeroStatus.setTextColor(ContextCompat.getColor(this, R.color.warning))
            binding.tvHeroStatus.setBackgroundResource(R.drawable.bg_status_pending)
        }

        // Patient Info ───────────────────────────────────────────────────
        binding.tvPhone.text = if (phone.isNotBlank()) phone else "Not provided"
        binding.tvAge.text   = if (age.isNotBlank()) "$age years" else "Not provided"

        // Appointment Info ───────────────────────────────────────────────
        val docName = doctor.trim()
        binding.tvDoctorName.text = if (docName.startsWith("Dr.", ignoreCase = true)) docName else "Dr. $docName"
        binding.tvDateTime.text   = "$date  •  $time"

        // Status detail row ──────────────────────────────────────────────
        if (isCompleted) {
            binding.tvStatusDetail.text = "Completed"
            binding.tvStatusDetail.setTextColor(ContextCompat.getColor(this, R.color.success))
        } else {
            binding.tvStatusDetail.text = "Pending"
            binding.tvStatusDetail.setTextColor(ContextCompat.getColor(this, R.color.warning))
        }

        // Next Visit (optional) ──────────────────────────────────────────
        if (nextVisit.isNotBlank()) {
            binding.tvNextVisit.text     = nextVisit
            binding.sectionNextVisit.visibility = View.VISIBLE
        } else {
            binding.sectionNextVisit.visibility = View.GONE
        }
    }

    override fun finish() {
        super.finish()
        // Play slide-out animation when going back
        overridePendingTransition(0, R.anim.slide_out_right)
    }
}
