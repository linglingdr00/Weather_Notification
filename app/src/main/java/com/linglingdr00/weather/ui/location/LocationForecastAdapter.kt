package com.linglingdr00.weather.ui.location

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.linglingdr00.weather.databinding.ListForecastItemBinding
import com.linglingdr00.weather.ui.forecast.ForecastItem

class LocationForecastAdapter: ListAdapter<ForecastItem, LocationForecastAdapter.LocationForecastViewHolder>(DiffCallback) {

    companion object DiffCallback : DiffUtil.ItemCallback<ForecastItem>() {
        override fun areItemsTheSame(oldItem: ForecastItem, newItem: ForecastItem): Boolean {
            return oldItem.locationText == newItem.locationText
        }

        override fun areContentsTheSame(oldItem: ForecastItem, newItem: ForecastItem): Boolean {
            return oldItem.locationText == newItem.locationText
        }
    }

    class LocationForecastViewHolder(private var binding: ListForecastItemBinding):
        RecyclerView.ViewHolder(binding.root) {
        fun bind(forecastItem: ForecastItem) {
            //binding.viewModel = viewModel
            binding.forecastItem = forecastItem
            binding.executePendingBindings()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocationForecastViewHolder {
        val itemView = ListForecastItemBinding.inflate(LayoutInflater.from(parent.context))
        return LocationForecastViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: LocationForecastViewHolder, position: Int) {
        // 取得與目前 RecyclerView 位置相關的 object
        val item = getItem(position)
        // 傳遞至 ForecastViewHolder 中的 bind() 方法
        holder.bind(item)
    }

}