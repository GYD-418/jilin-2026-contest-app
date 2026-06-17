package com.example.myapplication.ui.detail

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.R
import com.example.myapplication.data.network.HuaweiIotService
import com.example.myapplication.databinding.ActivityDeviceDetailBinding
import com.example.myapplication.ui.CredentialStore
import com.example.myapplication.ui.MainViewModel
import com.example.myapplication.ui.chart.ChartActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DeviceDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDeviceDetailBinding
    private lateinit var viewModel: MainViewModel
    private val gson = Gson()
    private val handler = Handler(Looper.getMainLooper())
    private val iotService = HuaweiIotService()
    private var deviceId: String = ""
    private var currentServiceId: String = ""
    private var lastPropsJson: String = ""
    private val pendingToggles = mutableMapOf<String, Boolean>()
    private var propertyAdapter: PropertyAdapter? = null

    private val refreshRunnable = object : Runnable {
        override fun run() {
            viewModel.loadDeviceShadow(deviceId)
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityDeviceDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.recyclerView.layoutManager = LinearLayoutManager(this)

        deviceId = intent.getStringExtra("device_id") ?: return finish()
        val deviceName = intent.getStringExtra("device_name") ?: ""
        val deviceStatus = intent.getStringExtra("device_status") ?: ""

        binding.textDeviceName.text = deviceName
        binding.textDeviceId.text = deviceId
        binding.textStatusBadge.text = when (deviceStatus) {
            "ONLINE" -> "在线"
            "OFFLINE" -> "离线"
            "INACTIVE" -> "未激活"
            else -> deviceStatus
        }

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        viewModel.selectedDeviceShadow.observe(this) { shadow ->
            if (shadow != null) {
                displayShadow(shadow)
                binding.swipeRefresh.isRefreshing = false
            }
        }

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadDeviceShadow(deviceId)
        }

        loadData()
    }

    private fun loadData() {
        viewModel.loadDeviceShadow(deviceId)
        handler.postDelayed(refreshRunnable, 1000)
    }

    private fun displayShadow(json: String) {
        binding.progressBar.visibility = android.view.View.GONE

        try {
            val mapType = object : TypeToken<Map<String, Any>>() {}.type
            val root: Map<String, Any> = gson.fromJson(json, mapType)

            val shadows = root["shadow"] as? List<*>
            val firstShadow = shadows?.firstOrNull() as? Map<*, *>
            val reported = firstShadow?.get("reported") as? Map<*, *>
            val properties = reported?.get("properties") as? Map<*, *>

            android.util.Log.d("IOT_DETAIL", "properties keys: ${properties?.keys}")

            val propsJson = gson.toJson(properties ?: emptyMap<String, Any>())
            if (propsJson != lastPropsJson) {
                pendingToggles.clear()
                lastPropsJson = propsJson
            }

            val eventTime = reported?.get("event_time") as? String ?: ""

            currentServiceId = (firstShadow?.get("service_id") as? String) ?: ""

            val timeStr = if (eventTime.isNotBlank()) "更新时间: $eventTime (每秒自动刷新)" else "等待数据上报..."
            binding.textUpdateTime.text = timeStr

            // 华为云上定义的属性白名单及显示顺序
            val allowedProperties = listOf(
                "temperature", "humidity", "distance", "waterLevel",
                "motorRpm", "lightDuty", "lightVoltage", "flowRate",
                "pumpOn", "buzzerOn"
            )
            
            // 智能过滤：只有当设备确实包含白名单中的属性时才过滤，否则显示全部
            val useWhitelist = properties?.keys?.any { it in allowedProperties } == true
            val propKeys = if (useWhitelist) {
                allowedProperties.filter { properties?.containsKey(it) == true }
            } else {
                properties?.keys?.toList() ?: emptyList()
            }
            
            // 需要显示为开关控制的属性（数字0/1也当作开关）
            val toggleProperties = setOf("lamp", "pumpOn", "buzzerOn")
            
            val propList = propKeys.map { k ->
                val v = properties!![k]!!
                val valueStr = formatValue(v)
                // 布尔类型 或 在开关白名单中的数字属性 显示为开关
                val isBool = v is Boolean || (k in toggleProperties && v is Number)
                val boolVal = pendingToggles[k] ?: when {
                    v is Boolean -> v
                    v is Number && k in toggleProperties -> v.toInt() == 1
                    else -> false
                }
                when (v) {
                    is Number -> PropertyHistoryStore.addPoint(deviceId, k.toString(), v.toFloat())
                    is Boolean -> PropertyHistoryStore.addPoint(deviceId, k.toString(), if (v) 1f else 0f)
                }
                Property(
                    key = k.toString(),
                    value = valueStr,
                    isBoolean = isBool,
                    boolValue = boolVal
                )
            } ?: emptyList()

            if (propertyAdapter == null) {
                propertyAdapter = PropertyAdapter(
                    initItems = propList,
                    onClick = { prop ->
                        val intent = android.content.Intent(this, ChartActivity::class.java).apply {
                            putExtra("device_id", deviceId)
                            putExtra("property_key", prop.key)
                        }
                        startActivity(intent)
                    },
                    onToggle = { prop, checked ->
                        pendingToggles[prop.key] = checked
                        sendToggleCommand(prop.key, checked)
                    }
                )
                binding.recyclerView.adapter = propertyAdapter
            } else {
                propertyAdapter?.updateItems(propList)
            }

            if (propList.isEmpty()) {
                Toast.makeText(this, "暂无数据", Toast.LENGTH_SHORT).show()
            }
        } catch (_: Exception) {}
    }

    private fun formatValue(value: Any?): String {
        return when (value) {
            is Double -> {
                if (value == value.toLong().toDouble()) value.toLong().toString()
                else String.format("%.2f", value)
            }
            is Number -> value.toString()
            is String -> value
            is Boolean -> value.toString()
            null -> "-"
            else -> value.toString()
        }
    }

    // === Command sending ===
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.detail_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_command) {
            showCommandDialog()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showCommandDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 20, 40, 0)
        }

        val serviceInput = EditText(this).apply {
            hint = "Service ID"
            setText(currentServiceId)
        }
        val commandInput = EditText(this).apply { hint = "命令名称" }
        val parasInput = EditText(this).apply {
            hint = "参数 (JSON, 可选)"
            setText("{}")
        }

        layout.addView(serviceInput)
        layout.addView(commandInput)
        layout.addView(parasInput)

        AlertDialog.Builder(this)
            .setTitle("下发命令")
            .setView(layout)
            .setPositiveButton("发送") { _, _ ->
                val serviceId = serviceInput.text.toString().trim()
                val cmdName = commandInput.text.toString().trim()
                val parasJson = parasInput.text.toString().trim()
                if (serviceId.isNotEmpty() && cmdName.isNotEmpty()) {
                    sendCommand(serviceId, cmdName, parasJson)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun sendCommand(serviceId: String, cmdName: String, parasJson: String) {
        val app = application
        val ak = CredentialStore.getAk(app)
        val sk = CredentialStore.getSk(app)
        val projectId = CredentialStore.getProjectId(app)
        val region = CredentialStore.getRegion(app)

        val paras: Map<String, Any> = try {
            val type = object : TypeToken<Map<String, Any>>() {}.type
            gson.fromJson(parasJson, type) ?: emptyMap()
        } catch (_: Exception) { emptyMap() }

        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                iotService.sendCommand(ak, sk, projectId, deviceId, serviceId, cmdName, paras, region)
            }
            result.fold(
                onSuccess = { Toast.makeText(this@DeviceDetailActivity, "命令发送成功", Toast.LENGTH_SHORT).show() },
                onFailure = { Toast.makeText(this@DeviceDetailActivity, "失败: ${it.message}", Toast.LENGTH_LONG).show() }
            )
        }
    }

    private fun sendToggleCommand(propKey: String, checked: Boolean) {
        val app = application
        val ak = CredentialStore.getAk(app)
        val sk = CredentialStore.getSk(app)
        val projectId = CredentialStore.getProjectId(app)
        val region = CredentialStore.getRegion(app)

        // Map property to command
        val cmdName = when (propKey) {
            "lamp" -> "lamp_control"
            else -> return
        }
        val parasKey = when (propKey) {
            "lamp" -> "lamp-stat"
            else -> return
        }
        // lamp-stat=0 means ON (checked), lamp-stat=1 means OFF (unchecked)
        val value = if (checked) 0 else 1

        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                iotService.sendCommand(ak, sk, projectId, deviceId, currentServiceId, cmdName, mapOf(parasKey to value), region)
            }
            result.fold(
                onSuccess = { Toast.makeText(this@DeviceDetailActivity, "命令已发送", Toast.LENGTH_SHORT).show() },
                onFailure = { Toast.makeText(this@DeviceDetailActivity, "失败: ${it.message}", Toast.LENGTH_LONG).show() }
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(refreshRunnable)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
