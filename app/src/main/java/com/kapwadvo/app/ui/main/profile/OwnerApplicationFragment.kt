package com.kapwadvo.app.ui.main.profile

import androidx.appcompat.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope

import com.kapwadvo.app.UserSession
import com.kapwadvo.app.data.CategoryManager
import com.kapwadvo.app.data.models.OwnerApplicationInsert
import com.kapwadvo.app.data.repository.ApplicationRepository
import com.kapwadvo.app.databinding.FragmentOwnerApplicationBinding
import kotlinx.coroutines.launch

class OwnerApplicationFragment : Fragment() {

    private var _binding: FragmentOwnerApplicationBinding? = null
    private val binding get() = _binding!!
    private var selectedCategories = mutableSetOf<String>()
    private var othersCustomText: String = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentOwnerApplicationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupCategoryDropdown()
        binding.btnSubmit.setOnClickListener { validateAndSubmit() }
        binding.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
    }

    private fun setupCategoryDropdown() {
        binding.etCategoryDropdown.setOnClickListener {
            showCategorySelectionDialog()
        }
    }

    private fun showCategorySelectionDialog() {
        val allOptions = CategoryManager.getCategories() + CategoryManager.OTHERS
        val items = allOptions.toTypedArray()
        val checkedItems = items.map { it in selectedCategories }.toBooleanArray()

        AlertDialog.Builder(requireContext())
            .setTitle("Select Categories")
            .setMultiChoiceItems(items, checkedItems) { dialog, which, isChecked ->
                if (isChecked) {
                    selectedCategories.add(items[which])
                    if (items[which] == CategoryManager.OTHERS) {
                        val input = android.widget.EditText(requireContext()).apply {
                            hint = "Specify 'Others'"
                            setText(othersCustomText)
                            setPadding(48, 24, 48, 24)
                        }
                        AlertDialog.Builder(requireContext())
                            .setTitle("Specify Others")
                            .setView(input)
                            .setPositiveButton("OK") { _, _ ->
                                othersCustomText = input.text.toString().trim()
                                updateCategoryDropdownText()
                            }
                            .setNegativeButton("Cancel") { _, _ ->
                                (dialog as AlertDialog).listView.setItemChecked(which, false)
                                selectedCategories.remove(items[which])
                                othersCustomText = ""
                            }
                            .setCancelable(false)
                            .show()
                    }
                } else {
                    selectedCategories.remove(items[which])
                    if (items[which] == CategoryManager.OTHERS) {
                        othersCustomText = ""
                        updateCategoryDropdownText()
                    }
                }
            }
            .setPositiveButton("OK") { _, _ ->
                updateCategoryDropdownText()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateCategoryDropdownText() {
        val list = selectedCategories.toMutableList()
        if (CategoryManager.OTHERS in list && othersCustomText.isNotEmpty()) {
            list.remove(CategoryManager.OTHERS)
            list.add("Others ($othersCustomText)")
        }
        binding.etCategoryDropdown.setText(list.joinToString(", "))
    }

    private fun getSelectedCategories(): String {
        return CategoryManager.encode(selectedCategories, othersCustomText)
    }

    private fun validateAndSubmit() {
        val businessName = binding.etBusinessName.text.toString().trim()
        val contactInfo = binding.etContactInfo.text.toString().trim()
        val address = binding.etAddress.text.toString().trim()
        val permit = binding.etPermit.text.toString().trim().takeIf { it.isNotEmpty() }
        val reason = binding.etReason.text.toString().trim().takeIf { it.isNotEmpty() }
        val categoryEncoded = getSelectedCategories()

        // Validate at least one category selected
        val anySelected = selectedCategories.isNotEmpty()

        if (businessName.isEmpty() || !anySelected || contactInfo.isEmpty() || address.isEmpty()) {
            Toast.makeText(context, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        // If "Others" is checked, require the custom text
        if (CategoryManager.OTHERS in selectedCategories && othersCustomText.isEmpty()) {
            Toast.makeText(context, "Please specify what \"Others\" means", Toast.LENGTH_SHORT).show()
            return
        }

        val uid = UserSession.userId
        if (uid == null) {
            Toast.makeText(context, "You must be logged in to apply", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnSubmit.isEnabled = false

        val application = OwnerApplicationInsert(
            userId = uid,
            businessName = businessName,
            category = categoryEncoded,
            contactInfo = contactInfo,
            businessAddress = address,
            permitNumber = permit,
            reason = reason
        )

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                ApplicationRepository.submitApplication(application)
                
                // Update the local cache so ProfileFragment knows it's pending without network
                val prefs = requireContext().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                prefs.edit().putString("app_status_$uid", "pending").apply()
                
                showSuccessDialog()
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Toast.makeText(context, "Failed to submit application. Please try again", Toast.LENGTH_LONG).show()
                binding.btnSubmit.isEnabled = true
            }
        }
    }

    private fun showSuccessDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Application Received")
            .setMessage("Your application is now under review. Please allow 1-2 business days for our team to verify your business details. You can track the status in your Profile.")
            .setPositiveButton("Got it") { dialog, _ ->
                dialog.dismiss()
                parentFragmentManager.popBackStack()
            }
            .setCancelable(false)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
