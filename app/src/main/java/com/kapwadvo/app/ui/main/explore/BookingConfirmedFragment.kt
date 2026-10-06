package com.kapwadvo.app.ui.main.explore

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.kapwadvo.app.R
import com.kapwadvo.app.databinding.FragmentBookingConfirmedBinding

class BookingConfirmedFragment : Fragment() {

    private var _binding: FragmentBookingConfirmedBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val ARG_PLACE = "place_name"
        private const val ARG_DATE = "booking_date"

        fun newInstance(placeName: String, bookingDate: String): BookingConfirmedFragment {
            return BookingConfirmedFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PLACE, placeName)
                    putString(ARG_DATE, bookingDate)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBookingConfirmedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Populate info card
        val placeName = arguments?.getString(ARG_PLACE) ?: "—"
        val bookingDate = arguments?.getString(ARG_DATE) ?: "—"
        binding.tvBookingPlace.text = placeName
        binding.tvBookingDate.text = bookingDate

        // Entrance animation for the success icon
        val scaleAnim = AnimationUtils.loadAnimation(requireContext(), R.anim.scale_bounce_in)
        binding.frameIcon.startAnimation(scaleAnim)

        // "View My Booking" → navigate to MyReservationsFragment
        binding.btnViewBooking.setOnClickListener {
            // Replace the current fragment with MyReservationsFragment.
            // We do NOT add to back stack here, so the previous back stack entry (which added this confirmation page)
            // remains at the top. When the user presses back from MyReservations, it will pop that entry
            // and correctly return to ExploreFragment.
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, MyReservationsFragment())
                .commit()
        }

        // "Back to Explore" → pop the back-stack fragment (returns to Explore tab)
        binding.btnBackToExplore.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
