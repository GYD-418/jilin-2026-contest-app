package com.example.myapplication.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.IotDevice
import com.example.myapplication.data.network.HuaweiIotService
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val iotService = HuaweiIotService()
    private val gson = Gson()

    private val _devices = MutableLiveData<List<IotDevice>>()
    val devices: LiveData<List<IotDevice>> = _devices

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _selectedDeviceShadow = MutableLiveData<String?>()
    val selectedDeviceShadow: LiveData<String?> = _selectedDeviceShadow

    var hasLoadedOnce = false
        private set

    fun hasCredentials(): Boolean = CredentialStore.hasCredentials(getApplication())

    fun refresh() {
        val app = getApplication<Application>()
        val ak = CredentialStore.getAk(app)
        val sk = CredentialStore.getSk(app)
        val projectId = CredentialStore.getProjectId(app)
        val region = CredentialStore.getRegion(app)

        Log.d("IOT_APP", "refresh called, ak=${ak.take(4)}..., projectId=$projectId")

        if (ak.isEmpty() || projectId.isEmpty()) {
            Log.d("IOT_APP", "credentials missing, aborting")
            _errorMessage.value = "请先配置华为云 AK/SK/项目ID"
            return
        }

        _isLoading.value = true
        _errorMessage.value = null

        Log.d("IOT_APP", "calling listDevices...")
        viewModelScope.launch {
            try {
                val result = iotService.listDevices(ak, sk, projectId, region)
                Log.d("IOT_APP", "listDevices result: ${result.isSuccess}, ${result.getOrNull()?.size ?: -1} devices")
                result.fold(
                    onSuccess = { devices ->
                        _devices.value = devices
                    },
                    onFailure = { e ->
                        Log.e("IOT_APP", "listDevices failed: ${e.message}")
                        _errorMessage.value = e.message ?: "查询设备失败"
                    }
                )
            } catch (e: Exception) {
                Log.e("IOT_APP", "unexpected error: ${e.message}", e)
                _errorMessage.value = e.message ?: "同步失败"
            } finally {
                _isLoading.value = false
                hasLoadedOnce = true
                Log.d("IOT_APP", "refresh finished, isLoading=false")
            }
        }
    }

    fun loadDeviceShadow(deviceId: String) {
        val app = getApplication<Application>()
        val ak = CredentialStore.getAk(app)
        val sk = CredentialStore.getSk(app)
        val projectId = CredentialStore.getProjectId(app)
        val region = CredentialStore.getRegion(app)

        viewModelScope.launch {
            try {
                iotService.getDeviceShadow(ak, sk, projectId, deviceId, region).fold(
                    onSuccess = { shadowJson ->
                        _selectedDeviceShadow.value = formatShadow(shadowJson)
                    },
                    onFailure = { e ->
                        _errorMessage.value = e.message
                    }
                )
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    private fun formatShadow(json: String): String {
        return try {
            val mapType = object : TypeToken<Map<String, Any>>() {}.type
            val map: Map<String, Any> = gson.fromJson(json, mapType)
            gson.newBuilder().setPrettyPrinting().create().toJson(map)
        } catch (e: Exception) {
            json
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
