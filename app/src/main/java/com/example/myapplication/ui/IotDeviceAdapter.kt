package com.example.myapplication.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.data.model.IotDevice
import com.example.myapplication.databinding.ItemDeviceBinding

class IotDeviceAdapter(
    private val onItemClick: (IotDevice) -> Unit
) : ListAdapter<IotDevice, IotDeviceAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDeviceBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemDeviceBinding,
        private val onItemClick: (IotDevice) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(device: IotDevice) {
            binding.textDeviceName.text = device.name
            binding.textDeviceId.text = device.deviceId
            binding.textStatus.text = when (device.status) {
                "ONLINE" -> "在线"
                "OFFLINE" -> "离线"
                "INACTIVE" -> "未激活"
                "FROZEN" -> "已冻结"
                else -> device.status
            }
            binding.root.setOnClickListener { onItemClick(device) }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<IotDevice>() {
        override fun areItemsTheSame(oldItem: IotDevice, newItem: IotDevice) =
            oldItem.deviceId == newItem.deviceId

        override fun areContentsTheSame(oldItem: IotDevice, newItem: IotDevice) =
            oldItem == newItem
    }
}
