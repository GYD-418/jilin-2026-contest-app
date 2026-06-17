package com.example.myapplication.data.network

import com.example.myapplication.data.model.DeviceListResponse
import com.example.myapplication.data.model.IotDevice
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class HuaweiIotService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    suspend fun listDevices(
        ak: String, sk: String, projectId: String, region: String = "cn-north-4"
    ): Result<List<IotDevice>> = withContext(Dispatchers.IO) {
        try {
            val url = "https://iotda.$region.myhuaweicloud.com/v5/iot/$projectId/devices"
            val signed = HuaweiSigner.sign(method = "GET", url = url, ak = ak, sk = sk, projectId = projectId)

            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", signed.authorization)
                .addHeader("X-Sdk-Date", signed.xSdkDate)
                .addHeader("Host", signed.host)
                .addHeader("X-Project-Id", projectId)
                .addHeader("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(IOException("HTTP ${response.code}: $body"))
            }

            val listResponse = gson.fromJson(body, DeviceListResponse::class.java)
            val devices = listResponse.devices.map { raw ->
                IotDevice(
                    deviceId = raw.device_id,
                    name = raw.device_name,
                    status = raw.status,
                    lastOnlineTime = raw.last_online_time ?: ""
                )
            }
            Result.success(devices)
        } catch (e: IOException) {
            Result.failure(e)
        }
    }

    suspend fun getDeviceShadow(
        ak: String, sk: String, projectId: String, deviceId: String, region: String = "cn-north-4"
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url = "https://iotda.$region.myhuaweicloud.com/v5/iot/$projectId/devices/$deviceId/shadow"
            val signed = HuaweiSigner.sign(method = "GET", url = url, ak = ak, sk = sk, projectId = projectId)

            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", signed.authorization)
                .addHeader("X-Sdk-Date", signed.xSdkDate)
                .addHeader("Host", signed.host)
                .addHeader("X-Project-Id", projectId)
                .addHeader("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(IOException("HTTP ${response.code}: $body"))
            }
            Result.success(body)
        } catch (e: IOException) {
            Result.failure(e)
        }
    }

    suspend fun sendCommand(
        ak: String, sk: String, projectId: String, deviceId: String,
        serviceId: String, commandName: String, paras: Map<String, Any> = emptyMap(),
        region: String = "cn-north-4"
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url = "https://iotda.$region.myhuaweicloud.com/v5/iot/$projectId/devices/$deviceId/commands"
            val bodyJson = gson.toJson(mapOf(
                "service_id" to serviceId, "command_name" to commandName, "paras" to paras
            ))
            val signed = HuaweiSigner.sign(method = "POST", url = url, ak = ak, sk = sk, projectId = projectId, body = bodyJson)

            val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Authorization", signed.authorization)
            conn.setRequestProperty("X-Sdk-Date", signed.xSdkDate)
            conn.setRequestProperty("Host", signed.host)
            conn.setRequestProperty("X-Project-Id", projectId)
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.connectTimeout = 15000
            conn.readTimeout = 15000

            conn.outputStream.use { it.write(bodyJson.toByteArray(Charsets.UTF_8)) }

            val respCode = conn.responseCode
            val respBody = if (respCode in 200..299) {
                conn.inputStream.bufferedReader().readText()
            } else {
                conn.errorStream?.bufferedReader()?.readText() ?: ""
            }

            if (respCode !in 200..299) {
                return@withContext Result.failure(IOException("HTTP $respCode: $respBody"))
            }
            Result.success(respBody)
        } catch (e: IOException) {
            Result.failure(e)
        }
    }
}
