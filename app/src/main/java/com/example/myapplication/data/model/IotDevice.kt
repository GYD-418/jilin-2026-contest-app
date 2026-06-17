package com.example.myapplication.data.model

data class IotDevice(
    val deviceId: String,
    val name: String,
    val status: String,
    val lastOnlineTime: String = ""
)

data class DeviceListResponse(
    val devices: List<DeviceRaw>
)

data class DeviceRaw(
    val device_id: String,
    val device_name: String,
    val status: String,
    val last_online_time: String?
)

data class DeviceShadowResponse(
    val device_id: String,
    val shadow: List<ShadowData>
)

data class ShadowData(
    val service_id: String,
    val desired: DesiredData?,
    val reported: ReportedData?
)

data class DesiredData(
    val properties: Any?,
    val event_time: String?
)

data class ReportedData(
    val properties: Any?,
    val event_time: String?
)

data class IamTokenResponse(
    val token: TokenData
)

data class TokenData(
    val expires_at: String
)
