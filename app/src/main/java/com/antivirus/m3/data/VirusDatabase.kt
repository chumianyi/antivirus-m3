package com.antivirus.m3.data

import android.content.Context
import com.antivirus.m3.R
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class PermissionRule(
    val permission: String,
    val risk: Int,
    val reason: String
)

@Serializable
data class VirusDatabaseData(
    val version: String,
    val updateDate: String,
    val signatures: List<VirusSignature>,
    val permissionRules: List<PermissionRule>
)

object VirusDatabase {
    private var database: VirusDatabaseData? = null
    private val json = Json { ignoreUnknownKeys = true }

    fun load(context: Context): VirusDatabaseData {
        database?.let { return it }
        val inputStream = context.resources.openRawResource(R.raw.virus_db)
        val content = inputStream.bufferedReader().use { it.readText() }
        database = json.decodeFromString(VirusDatabaseData.serializer(), content)
        return database!!
    }

    fun getSignature(packageName: String): VirusSignature? {
        return database?.signatures?.find { it.packageName == packageName }
    }

    fun getPermissionRisk(permission: String): PermissionRule? {
        return database?.permissionRules?.find { it.permission == permission }
    }

    val signatureCount: Int get() = database?.signatures?.size ?: 0
    val permissionRuleCount: Int get() = database?.permissionRules?.size ?: 0
    val version: String get() = database?.version ?: "unknown"
}
