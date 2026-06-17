package com.example.myapplication.ui.chart

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.databinding.ActivityChartBinding
import com.example.myapplication.ui.MainViewModel
import com.example.myapplication.ui.detail.PropertyHistoryStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class ChartActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChartBinding
    private val handler = Handler(Looper.getMainLooper())
    private val gson = Gson()
    private lateinit var viewModel: MainViewModel
    private lateinit var deviceId: String
    private lateinit var propertyKey: String
    private var chartReady = false

    private val refreshRunnable = object : Runnable {
        override fun run() {
            viewModel.loadDeviceShadow(deviceId)
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityChartBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        deviceId = intent.getStringExtra("device_id") ?: return finish()
        propertyKey = intent.getStringExtra("property_key") ?: return finish()
        supportActionBar?.title = propertyKey

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        viewModel.selectedDeviceShadow.observe(this) { shadow ->
            if (shadow != null) {
                processShadow(shadow)
            }
        }

        binding.webView.settings.javaScriptEnabled = true

        binding.webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                chartReady = true
                pushToChart()
            }
        }

        binding.webView.loadDataWithBaseURL(null, buildHtml(), "text/html", "UTF-8", null)

        viewModel.loadDeviceShadow(deviceId)
        handler.postDelayed(refreshRunnable, 1000)
    }

    private fun processShadow(json: String) {
        try {
            val mapType = object : TypeToken<Map<String, Any>>() {}.type
            val root: Map<String, Any> = gson.fromJson(json, mapType)
            val shadows = root["shadow"] as? List<*>
            val firstShadow = shadows?.firstOrNull() as? Map<*, *>
            val reported = firstShadow?.get("reported") as? Map<*, *>
            val properties = reported?.get("properties") as? Map<*, *>

            val value = properties?.get(propertyKey) ?: return
            val floatVal = when (value) {
                is Number -> value.toFloat()
                is Boolean -> if (value) 1f else 0f
                else -> return@processShadow
            }

            PropertyHistoryStore.addPoint(deviceId, propertyKey, floatVal)
            pushToChart()
        } catch (_: Exception) {}
    }

    private fun pushToChart() {
        if (!chartReady) return

        val points = PropertyHistoryStore.getHistory(deviceId, propertyKey)
        if (points.isEmpty()) return

        val labels = points.map { "\"${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(it.timestamp))}\"" }
        val vals = points.map { it.value.toString() }
        val min = (points.minOf { it.value } * 0.9f).toInt()
        val max = (points.maxOf { it.value } * 1.1f).toInt().coerceAtLeast(min + 1)

        binding.webView.evaluateJavascript(
            "push([${labels.joinToString(",")}], [${vals.joinToString(",")}], $min, $max)", null
        )
    }

    private fun buildHtml() = """
<!DOCTYPE html>
<html>
<head>
<meta name='viewport' content='width=device-width, initial-scale=1.0'>
<script src='https://cdn.jsdelivr.net/npm/chart.js@4.4.0/dist/chart.umd.min.js'></script>
<style>
*{margin:0;padding:0}
body{background:#1A1A2E;display:flex;align-items:center;justify-content:center;height:100vh;padding:8px}
#box{width:100%;max-width:600px}
</style>
</head>
<body>
<div id='box'><canvas id='c'></canvas></div>
<script>
var chart=new Chart(document.getElementById('c'),{
  type:'line',
  data:{labels:[],datasets:[{label:'$propertyKey',data:[],borderColor:'#00D4FF',backgroundColor:'rgba(0,212,255,0.15)',borderWidth:2,pointRadius:0,fill:true,tension:0.3}]},
  options:{
    responsive:true,maintainAspectRatio:true,animation:{duration:200},
    scales:{y:{ticks:{color:'#AAA'},grid:{color:'rgba(255,255,255,0.05)'}},x:{ticks:{color:'#AAA',maxTicksLimit:6},grid:{display:false}}},
    plugins:{legend:{labels:{color:'#AAA'}}}
  }
});
function push(l,v,min,max){chart.data.labels=l;chart.data.datasets[0].data=v;chart.options.scales.y.min=min;chart.options.scales.y.max=max;chart.update()}
</script>
</body>
</html>
""".trimIndent()

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(refreshRunnable)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
