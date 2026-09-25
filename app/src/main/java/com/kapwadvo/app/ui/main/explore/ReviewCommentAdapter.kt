package com.kapwadvo.app.ui.main.explore

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kapwadvo.app.data.models.CommentWithAuthor
import com.kapwadvo.app.databinding.ItemReviewCommentBinding

class ReviewCommentAdapter(
    private val isAdmin: Boolean = false,
    private val onDeleteClick: (CommentWithAuthor) -> Unit = {}
) : RecyclerView.Adapter<ReviewCommentAdapter.ViewHolder>() {

    private val items = mutableListOf<CommentWithAuthor>()

    fun submitList(list: List<CommentWithAuthor>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    inner class ViewHolder(private val b: ItemReviewCommentBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(c: CommentWithAuthor) {
            b.tvInitials.text = c.authorInitials
            b.tvAuthorName.text = c.authorName
            b.tvDate.text = c.createdAt?.take(10) ?: ""
            b.tvContent.text = c.content

            // Admin delete
            if (isAdmin) {
                b.btnDelete.visibility = View.VISIBLE
                b.btnDelete.setOnClickListener { onDeleteClick(c) }
            } else {
                b.btnDelete.visibility = View.GONE
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemReviewCommentBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(items[position])
    override fun getItemCount() = items.size
}
