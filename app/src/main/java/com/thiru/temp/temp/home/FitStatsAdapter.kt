package com.thiru.temp.temp.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.thiru.temp.temp.R
import com.thiru.temp.temp.model.FitActivity
import kotlinx.android.synthetic.main.fit_stats_row.view.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class FitStatsAdapter : ListAdapter<FitActivity, FitStatsAdapter.ViewHolder>(
    DIFF
) {

    private var maxDuration = 0.0

    override fun submitList(list: List<FitActivity>?) {
        list?.forEach {
            maxDuration = Math.max(maxDuration, it.distanceMeters)
        }
        super.submitList(list)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(
                parent.context
            ).inflate(R.layout.fit_stats_row, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), maxDuration.toInt())
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<FitActivity>() {
            override fun areItemsTheSame(oldItem: FitActivity, newItem: FitActivity): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: FitActivity, newItem: FitActivity): Boolean {
                return oldItem == newItem
            }

        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        fun bind(activity: FitActivity, max: Int) {
            val context = itemView.context
            val calendar = Calendar.getInstance().apply { timeInMillis = activity.date }
            val day = calendar.getDisplayName(
                Calendar.DAY_OF_WEEK,
                Calendar.LONG,
                Locale.getDefault()
            )
            val monthFormatter = SimpleDateFormat("MM")
            itemView.statsRowTitle.text = context.getString(
                R.string.stat_date,
                day,
                calendar.get(Calendar.DAY_OF_MONTH),
                monthFormatter.format(calendar.time).toInt()
            )

            val minutes = TimeUnit.MILLISECONDS.toMinutes(activity.durationMs)
            val km = String.format("%.2f", activity.distanceMeters / 1000)
            val duration = context.getString(R.string.stat_duration, minutes)
            val distance = context.getString(R.string.stat_distance, km)
            itemView.statsRowContent.apply {
                text = duration
                append("\n")
                append(distance)
            }

            itemView.statsRowProgress.max = max
            itemView.statsRowProgress.progress = activity.distanceMeters.toInt()
        }
    }
}