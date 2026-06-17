package com.example.myapplication.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        loadSavedCredentials()

        binding.btnSave.setOnClickListener {
            saveCredentials()
        }
    }

    private fun loadSavedCredentials() {
        val savedAk = CredentialStore.getAk(this)
        val savedSk = CredentialStore.getSk(this)
        val savedProjectId = CredentialStore.getProjectId(this)
        val savedRegion = CredentialStore.getRegion(this)

        binding.editAk.setText(savedAk.ifEmpty { "HPUA5SZXFWC9K1IXNLHT" })
        binding.editSk.setText(savedSk.ifEmpty { "LR3dklvavTQIHzHpOBjWFHA9Kne08Klqq6C94lqj" })
        binding.editProjectId.setText(savedProjectId.ifEmpty { "5a2a445956db4d3ba5812d1a1fb2889d" })
        binding.editRegion.setText(savedRegion.ifEmpty { "cn-north-4" })
    }

    private fun saveCredentials() {
        val ak = binding.editAk.text.toString().trim()
        val sk = binding.editSk.text.toString().trim()
        val projectId = binding.editProjectId.text.toString().trim()
        val region = binding.editRegion.text.toString().trim().ifEmpty { "cn-north-4" }

        if (ak.isEmpty() || sk.isEmpty() || projectId.isEmpty()) {
            Toast.makeText(this, "请填写所有必填项", Toast.LENGTH_SHORT).show()
            return
        }

        CredentialStore.save(this, ak, sk, projectId, region)
        Toast.makeText(this, "保存成功", Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
