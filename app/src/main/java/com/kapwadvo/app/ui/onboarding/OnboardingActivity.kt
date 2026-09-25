package com.kapwadvo.app.ui.onboarding

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.kapwadvo.app.R
import com.kapwadvo.app.databinding.ActivityOnboardingBinding
import com.kapwadvo.app.databinding.ItemOnboardingPageBinding
import com.kapwadvo.app.ui.main.MainActivity

class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding

    private val pages = listOf(
        OnboardingPage(
            imageRes    = R.mipmap.ic_launcher,
            title       = "Welcome to Kapwa DVO!",
            description = "Your personal guide to discovering the hidden gems and beloved destinations of Davao City."
        ),
        OnboardingPage(
            imageRes    = android.R.drawable.ic_menu_compass,
            title       = "Explore Destinations",
            description = "Browse tourist spots, local businesses, hidden gems, food places, and accommodations all in one place."
        ),
        OnboardingPage(
            imageRes    = android.R.drawable.ic_menu_mapmode,
            title       = "Find Places on the Map",
            description = "Use the interactive map to search nearby spots and navigate to your next adventure."
        ),
        OnboardingPage(
            imageRes    = android.R.drawable.ic_menu_save,
            title       = "Save Your Favourites",
            description = "Bookmark spots you love and revisit them anytime from your Saved tab."
        ),
        OnboardingPage(
            imageRes    = android.R.drawable.ic_menu_agenda,
            title       = "Book with Ease",
            description = "Request bookings directly through the app and track all your reservations in one place."
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        androidx.core.view.WindowCompat.getInsetsController(window, binding.root)
            .isAppearanceLightStatusBars = true

        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        setupViewPager()
        setupDots()
        setupButtons()
    }

    private fun setupViewPager() {
        binding.viewPager.adapter = OnboardingPagerAdapter(pages)
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateDots(position)
                updateButtons(position)
            }
        })
    }

    private fun setupDots() {
        repeat(pages.size) {
            val dot = ImageView(this).apply {
                setImageResource(R.drawable.dot_inactive)
                val params = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { lp -> lp.setMargins(6, 0, 6, 0) }
                layoutParams = params
            }
            binding.dotsLayout.addView(dot)
        }
        updateDots(0)
    }

    private fun updateDots(selectedPos: Int) {
        for (i in 0 until binding.dotsLayout.childCount) {
            val dot = binding.dotsLayout.getChildAt(i) as ImageView
            dot.setImageResource(
                if (i == selectedPos) R.drawable.dot_active else R.drawable.dot_inactive
            )
        }
    }

    private fun setupButtons() {
        updateButtons(0)
        binding.btnNext.setOnClickListener {
            val current = binding.viewPager.currentItem
            if (current < pages.lastIndex) {
                binding.viewPager.currentItem = current + 1
            } else {
                finishOnboarding()
            }
        }
        binding.btnSkip.setOnClickListener { finishOnboarding() }
    }

    private fun updateButtons(position: Int) {
        if (position == pages.lastIndex) {
            binding.btnNext.text = "Get Started"
            binding.btnSkip.visibility = View.GONE
        } else {
            binding.btnNext.text = "Next"
            binding.btnSkip.visibility = View.VISIBLE
        }
    }

    private fun finishOnboarding() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}

data class OnboardingPage(val imageRes: Int, val title: String, val description: String)

class OnboardingPagerAdapter(private val pages: List<OnboardingPage>) :
    RecyclerView.Adapter<OnboardingPagerAdapter.PageViewHolder>() {

    inner class PageViewHolder(val binding: ItemOnboardingPageBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): PageViewHolder {
        val binding = ItemOnboardingPageBinding.inflate(
            android.view.LayoutInflater.from(parent.context), parent, false
        )
        return PageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        val page = pages[position]
        holder.binding.ivOnboardingImage.setImageResource(page.imageRes)
        holder.binding.tvOnboardingTitle.text = page.title
        holder.binding.tvOnboardingDesc.text = page.description
    }

    override fun getItemCount() = pages.size
}
