package com.example.myapplication.ui.detail

object PropertyHistoryStore {
    private const val MAX_POINTS = 60
    private val history = mutableMapOf<String, MutableList<DataPoint>>()

    data class DataPoint(
        val timestamp: Long,
        val value: Float
    )

    fun addPoint(deviceId: String, key: String, value: Float) {
        val fullKey = "${deviceId}_$key"
        val list = history.getOrPut(fullKey) { mutableListOf() }
        list.add(DataPoint(System.currentTimeMillis(), value))
        if (list.size > MAX_POINTS) list.removeAt(0)
    }

    fun getHistory(deviceId: String, key: String): List<DataPoint> {
        val fullKey = "${deviceId}_$key"
        return history[fullKey]?.toList() ?: emptyList()
    }
}
