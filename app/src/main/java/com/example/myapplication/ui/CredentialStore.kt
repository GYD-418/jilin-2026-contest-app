package com.example.myapplication.ui

import android.content.Context
import android.content.SharedPreferences

object CredentialStore {

    private const val PREFS_NAME = "huawei_creds"
    private const val KEY_AK = "ak"
    private const val KEY_SK = "sk"
    private const val KEY_PROJECT_ID = "project_id"
    private const val KEY_REGION = "region"

    fun get(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun save(context: Context, ak: String, sk: String, projectId: String, region: String) {
        get(context).edit().apply {
            putString(KEY_AK, ak)
            putString(KEY_SK, sk)
            putString(KEY_PROJECT_ID, projectId)
            putString(KEY_REGION, region)
            apply()
        }
    }

    fun getAk(context: Context): String = get(context).getString(KEY_AK, "") ?: ""
    fun getSk(context: Context): String = get(context).getString(KEY_SK, "") ?: ""
    fun getProjectId(context: Context): String = get(context).getString(KEY_PROJECT_ID, "") ?: ""
    fun getRegion(context: Context): String = get(context).getString(KEY_REGION, "cn-north-4") ?: "cn-north-4"
    fun hasCredentials(context: Context): Boolean = getAk(context).isNotEmpty() && getProjectId(context).isNotEmpty()
}
