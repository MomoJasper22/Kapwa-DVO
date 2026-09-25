package com.kapwadvo.app.ui.main.explore

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kapwadvo.app.data.models.ReviewWithAuthor
import com.kapwadvo.app.databinding.ItemReviewBinding

class ReviewAdapter(
    private val isAdmin: Boolean = false,
    private val onReplyClick: (ReviewWithAuthor) -> Unit = {},
    private val onDeleteClick: (ReviewWithAuthor) -> Unit = {}
) : RecyclerView.Adapter<ReviewAdapter.ViewHolder>() {

    private val items = mutableListOf<ReviewWithAuthor>()

    fun submitList(list: List<ReviewWithAuthor>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    inner class ViewHolder(private val b: ItemReviewBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(r: ReviewWithAuthor) {
            b.tvInitials.text = r.authorInitials
            b.tvAuthorName.text = r.authorName
            b.tvDate.text = r.createdAt?.take(10) ?: ""
            b.ratingDisplay.rating = r.rating.toFloat()
            if (!r.comment.isNullOrBlank()) {
                b.tvComment.text = r.comment
                b.tvComment.visibility = View.VISIBLE
            } else {
                b.tvComment.visibility = View.GONE
            }
            b.btnReply.setOnClickListener { onReplyClick(r) }

            // Admin delete
            if (isAdmin) {
                b.btnDelete.visibility = View.VISIBLE
                b.btnDelete.setOnClickListener { onDeleteClick(r) }
            } else {
                b.btnDelete.visibility = View.GONE
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemReviewBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(items[position])
    override fun getItemCount() = items.size
}
