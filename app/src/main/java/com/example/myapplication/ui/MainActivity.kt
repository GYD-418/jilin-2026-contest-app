package com.example.myapplication.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.data.model.IotDevice
import com.example.myapplication.databinding.ActivityMainBinding
import com.example.myapplication.ui.detail.DeviceDetailActivity

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    private lateinit var adapter: IotDeviceAdapter
    private val handler = Handler(Looper.getMainLooper())

    private val autoRefresh = object : Runnable {
        override fun run() {
            if (viewModel.hasCredentials()) viewModel.refresh()
            handler.postDelayed(this, 5000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        setupRecyclerView()
        observeViewModel()
        setupSwipeRefresh()
    }

    override fun onResume() {
        super.onResume()
        if (viewModel.hasCredentials() && !viewModel.hasLoadedOnce) {
            viewModel.refresh()
        }
        handler.postDelayed(autoRefresh, 5000)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(autoRefresh)
    }

    private fun setupRecyclerView() {
        adapter = IotDeviceAdapter { device -> onDeviceClick(device) }
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    private fun observeViewModel() {
        viewModel.devices.observe(this) { devices ->
            adapter.submitList(devices)
            binding.textEmpty.text = when {
                !viewModel.hasCredentials() -> "请点击右上角配置华为云凭证"
                viewModel.isLoading.value == true -> "加载中..."
                devices.isEmpty() -> "暂无设备，下拉刷新"
                else -> ""
            }
            binding.textEmpty.visibility =
                if (devices.isEmpty()) android.view.View.VISIBLE
                else android.view.View.GONE
        }

        viewModel.isLoading.observe(this) { loading ->
            binding.progressBar.visibility =
                if (loading) android.view.View.VISIBLE
                else android.view.View.GONE
            binding.swipeRefresh.isRefreshing = loading
            // Update empty text when loading state changes
            if (!loading) {
                val devices = viewModel.devices.value
                binding.textEmpty.text = when {
                    !viewModel.hasCredentials() -> "请点击右上角配置华为云凭证"
                    devices.isNullOrEmpty() -> "暂无设备，下拉刷新"
                    else -> ""
                }
            }
        }

        viewModel.errorMessage.observe(this) { error ->
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }

    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refresh()
        }
    }

    private fun onDeviceClick(device: IotDevice) {
        val intent = Intent(this, DeviceDetailActivity::class.java).apply {
            putExtra("device_id", device.deviceId)
            putExtra("device_name", device.name)
            putExtra("device_status", device.status)
        }
        startActivity(intent)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(com.example.myapplication.R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            com.example.myapplication.R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
