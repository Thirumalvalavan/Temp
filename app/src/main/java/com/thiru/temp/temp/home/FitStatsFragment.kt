package com.thiru.temp.temp.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.thiru.temp.temp.R
import com.thiru.temp.temp.model.FitRepository
import kotlinx.android.synthetic.main.fit_stats_fragment.*
import java.util.concurrent.TimeUnit

/**
 * Fragment to show the last users activities and stats.
 *
 * It observes the total count stats and the last activities updating the view upon changes
 */
class FitStatsFragment : Fragment() {

    lateinit var actionsCallback: FitStatsActions

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fit_stats_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = FitStatsAdapter()
        statsList.adapter = adapter

        val repository = FitRepository.getInstance(requireContext())
        repository.getStats().observe(viewLifecycleOwner, Observer { fitStats ->
            statsActivityCount.text = getString(
                R.string.stats_total_count,
                fitStats.totalCount
            )
            statsDistanceCount.text = getString(
                R.string.stats_total_distance,
                fitStats.totalDistanceMeters.toInt()
            )
            val durationInMin = TimeUnit.MILLISECONDS.toMinutes(fitStats.totalDurationMs)
            statsDurationCount.text = getString(R.string.stats_total_duration, durationInMin)
        })

        repository.getLastActivities(10).observe(viewLifecycleOwner, Observer {
            adapter.submitList(it)
            statsList.smoothScrollToPosition(0)
        })

        statsStartButton.setOnClickListener {
            actionsCallback.onStartActivity()
        }
    }

    interface FitStatsActions {
        fun onStartActivity()
    }
}