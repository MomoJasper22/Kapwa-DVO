package com.kapwadvo.app.ui.main.explore

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.kapwadvo.app.data.models.Booking
import com.kapwadvo.app.data.models.Listing
import com.kapwadvo.app.data.repository.BookingRepository
import com.kapwadvo.app.databinding.DialogBookingBinding
import com.kapwadvo.app.databinding.FragmentReservationDetailBinding
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Calendar

private val safeJson = Json { ignoreUnknownKeys = true }

class ReservationDetailFragment : Fragment() {

    private var _binding: FragmentReservationDetailBinding? = null
    private val binding get() = _binding!!

    private var booking: Booking? = null
    private var listing: Listing? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bookingJson = arguments?.getString(ARG_BOOKING)
        val listingJson = arguments?.getString(ARG_LISTING)
        try {
            if (bookingJson != null && listingJson != null) {
                booking = safeJson.decodeFromString(bookingJson)
                listing = safeJson.decodeFromString(listingJson)
            }
        } catch (e: Exception) {}
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReservationDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        bindData()
    }

    private fun bindData() {
        val b = booking ?: return
        val l = listing ?: return

        binding.tvListingName.text = l.name
        binding.tvAddress.text = l.address ?: "No address provided"

        if (l.photoUrls.isNotEmpty()) {
            binding.ivListingPhoto.visibility = View.VISIBLE
            Glide.with(this)
                .load(l.photoUrls.first())
                .into(binding.ivListingPhoto)
        } else {
            binding.ivListingPhoto.visibility = View.GONE
        }

        val parts = b.date.split(" ")
        binding.tvDate.text = parts.getOrNull(0) ?: b.date
        binding.tvTime.text = parts.getOrNull(1) ?: ""
        
        binding.tvGuests.text = "${b.guests} guest(s)"
        binding.tvNotes.text = if (b.notes.isNullOrBlank()) "None" else b.notes

        val (label, bgColor, textColor) = when (b.status.lowercase()) {
            "confirmed", "accepted" -> Triple("Accepted", Color.parseColor("#D4EDDA"), Color.parseColor("#155724"))
            "declined", "rejected" -> Triple("Declined", Color.parseColor("#F8D7DA"), Color.parseColor("#721C24"))
            "cancelled" -> Triple("Cancelled", Color.parseColor("#E2E3E5"), Color.parseColor("#383D41"))
            else -> Triple("Pending", Color.parseColor("#FFF3CD"), Color.parseColor("#856404"))
        }

        binding.tvStatus.text = label
        binding.tvStatus.setBackgroundColor(bgColor)
        binding.tvStatus.setTextColor(textColor)

        if ((b.status.lowercase() == "declined" || b.status.lowercase() == "rejected") && !b.declineReason.isNullOrBlank()) {
            binding.tvDeclineReason.visibility = View.VISIBLE
            binding.tvDeclineReason.text = "Reason: ${b.declineReason}"
        } else {
            binding.tvDeclineReason.visibility = View.GONE
        }

        if (b.status.lowercase() == "pending") {
            binding.layoutActions.visibility = View.VISIBLE
            binding.btnEdit.setOnClickListener { showEditDialog() }
            binding.btnCancel.setOnClickListener { showCancelConfirmation() }
        } else {
            binding.layoutActions.visibility = View.GONE
        }
    }

    private fun showCancelConfirmation() {
        val b = booking ?: return
        AlertDialog.Builder(requireContext())
            .setTitle("Cancel Request")
            .setMessage("Are you sure you want to cancel this reservation request?")
            .setPositiveButton("Yes") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        BookingRepository.updateBookingStatus(b.id, b.listingId, "cancelled")
                        Toast.makeText(context, "Reservation cancelled", Toast.LENGTH_SHORT).show()
                        booking = booking?.copy(status = "cancelled")
                        bindData()
                        parentFragmentManager.setFragmentResult("booking_updated", Bundle())
                    } catch (e: Exception) {
                        Toast.makeText(context, "Failed to cancel", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun showEditDialog() {
        val b = booking ?: return
        val dialogBinding = DialogBookingBinding.inflate(layoutInflater)
        val d = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        dialogBinding.tvTitle.text = "Edit Reservation"
        dialogBinding.btnConfirm.text = "Save Changes"
        
        val parts = b.date.split(" ")
        var selectedDate = parts.getOrNull(0)
        var selectedTime = parts.getOrNull(1)

        dialogBinding.etDate.setText(selectedDate)
        dialogBinding.etTime.setText(selectedTime)
        dialogBinding.etGuests.setText(b.guests.toString())
        dialogBinding.etNotes.setText(b.notes ?: "")

        val calendar = Calendar.getInstance()

        dialogBinding.etDate.setOnClickListener {
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)
            DatePickerDialog(requireContext(), { _, y, m, dOfMonth ->
                val mStr = (m + 1).toString().padStart(2, '0')
                val dStr = dOfMonth.toString().padStart(2, '0')
                selectedDate = "$y-$mStr-$dStr"
                dialogBinding.etDate.setText(selectedDate)
            }, year, month, day).apply {
                datePicker.minDate = System.currentTimeMillis()
                show()
            }
        }

        dialogBinding.etTime.setOnClickListener {
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)
            TimePickerDialog(requireContext(), { _, h, min ->
                val hStr = h.toString().padStart(2, '0')
                val mStr = min.toString().padStart(2, '0')
                selectedTime = "$hStr:$mStr"
                dialogBinding.etTime.setText(selectedTime)
            }, hour, minute, false).show()
        }

        dialogBinding.btnCancel.setOnClickListener { d.dismiss() }
        dialogBinding.btnConfirm.setOnClickListener {
            val date = selectedDate; val time = selectedTime
            val guestsStr = dialogBinding.etGuests.text.toString().trim()
            val guests = guestsStr.toIntOrNull() ?: 1
            val notes = dialogBinding.etNotes.text.toString().trim()

            if (date == null || time == null) {
                Toast.makeText(context, "Please select both date and time", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (guests < 1) {
                Toast.makeText(context, "Please enter a valid number of guests", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val fullDate = "$date $time"
                    val finalNotes = notes.ifEmpty { null }
                    BookingRepository.updateBookingDetails(b.id, b.listingId, fullDate, guests, finalNotes)
                    Toast.makeText(context, "Reservation updated", Toast.LENGTH_SHORT).show()
                    booking = booking?.copy(date = fullDate, guests = guests, notes = finalNotes)
                    bindData()
                    parentFragmentManager.setFragmentResult("booking_updated", Bundle())
                    d.dismiss()
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to update reservation", Toast.LENGTH_SHORT).show()
                }
            }
        }

        d.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_BOOKING = "arg_booking"
        private const val ARG_LISTING = "arg_listing"

        fun newInstance(booking: Booking, listing: Listing) = ReservationDetailFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_BOOKING, safeJson.encodeToString(booking))
                putString(ARG_LISTING, safeJson.encodeToString(listing))
            }
        }
    }
}
