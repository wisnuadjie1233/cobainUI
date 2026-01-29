package com.example.cobainui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class DateItem(val date: Calendar, var isSelected: Boolean = false)

class DateAdapter(private val dates: List<DateItem>) : RecyclerView.Adapter<DateAdapter.DateViewHolder>() {

    private var selectedPosition = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DateViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_date, parent, false)
        return DateViewHolder(view)
    }

    override fun onBindViewHolder(holder: DateViewHolder, position: Int) {
        val dateItem = dates[position]
        holder.bind(dateItem)

        holder.itemView.setOnClickListener {
            if (selectedPosition != position) {
                val previousSelectedPosition = selectedPosition
                selectedPosition = position

                if (previousSelectedPosition != -1) {
                    dates[previousSelectedPosition].isSelected = false
                    notifyItemChanged(previousSelectedPosition)
                }

                dateItem.isSelected = true
                notifyItemChanged(position)
            }
        }
    }

    override fun getItemCount(): Int = dates.size

    class DateViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dayName: TextView = itemView.findViewById(R.id.day_name)
        private val dayNumber: TextView = itemView.findViewById(R.id.day_number)

        fun bind(dateItem: DateItem) {
            val dayNameFormat = SimpleDateFormat("EEE", Locale("id", "ID"))
            dayName.text = dayNameFormat.format(dateItem.date.time)

            dayNumber.text = dateItem.date.get(Calendar.DAY_OF_MONTH).toString()

            if (dateItem.isSelected) {
                dayNumber.setBackgroundResource(R.drawable.bg_date_selected)
                dayNumber.setTextColor(itemView.context.getColor(android.R.color.white))
            } else {
                dayNumber.setBackgroundResource(R.drawable.bg_date_unselected)
                dayNumber.setTextColor(itemView.context.getColor(android.R.color.white))
            }
        }
    }
}
