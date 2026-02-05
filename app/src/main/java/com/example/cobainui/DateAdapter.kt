package com.example.cobainui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Locale

class DateAdapter(private val dates: List<DateItem>) : RecyclerView.Adapter<DateAdapter.DateViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DateViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_date, parent, false)
        return DateViewHolder(view)
    }

    override fun onBindViewHolder(holder: DateViewHolder, position: Int) {
        val dateItem = dates[position]
        holder.bind(dateItem)

        // Set a data di HomeActivity
        holder.itemView.isSelected = dateItem.isSelected
    }

    override fun getItemCount(): Int = dates.size

    inner class DateViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dayNameText: TextView = itemView.findViewById(R.id.day_name_text)
        private val dayNumberText: TextView = itemView.findViewById(R.id.day_number_text)

        fun bind(dateItem: DateItem) {
            val dayNameFormat = SimpleDateFormat("EEE", Locale("id", "ID"))
            val dayNumberFormat = SimpleDateFormat("d", Locale.getDefault())

            dayNameText.text = dayNameFormat.format(dateItem.date.time)
            dayNumberText.text = dayNumberFormat.format(dateItem.date.time)
        }
    }
}
