package com.example.myapplication.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.security.MessageDigest

/**
 * 在线更新管理：清单获取 → 差异 → 下载 → 校验 → 版本切换
 */
object UpdateManager {
    private const val TIMEOUT = 8000

    suspend fun checkAndUpdate(manifestUrl: String, rootDir: File): Boolean = withContext(Dispatchers.IO) {
        val manifest = fetchManifest(manifestUrl) ?: return@withContext false
        val newVer = manifest.version
        val targetDir = File(rootDir, "assets/$newVer").apply { mkdirs() }

        // 下载 assets
        manifest.assets.forEach { asset ->
            val local = File(targetDir, asset.id)
            if (!local.exists() || sha256(local) != asset.sha256) {
                if (!download(asset.url, local)) return@withContext false
                if (sha256(local) != asset.sha256) return@withContext false
            }
        }
        // 下载 levels
        manifest.levels.forEach { level ->
            val local = File(targetDir, "${level.id}.json")
            if (!local.exists() || sha256(local) != level.sha256) {
                if (!download(level.url, local)) return@withContext false
                if (sha256(local) != level.sha256) return@withContext false
            }
        }
        // 写入版本索引
        File(rootDir, "current_version.txt").writeText(newVer)
        true
    }

    private fun fetchManifest(url: String): Manifest? {
        val json = httpGet(url) ?: return null
        val obj = JSONObject(json)
        val assets = obj.getJSONArray("assets")
            .let { 0 until it.length() }
            .map { idx ->
                val a = obj.getJSONArray("assets").getJSONObject(idx)
                Manifest.Asset(
                    id = a.getString("id"),
                    type = a.getString("type"),
                    url = a.getString("url"),
                    sha256 = a.getString("sha256"),
                    size = a.getLong("size")
                )
            }
        val levels = obj.getJSONArray("levels")
            .let { 0 until it.length() }
            .map { idx ->
                val l = obj.getJSONArray("levels").getJSONObject(idx)
                Manifest.LevelEntry(
                    id = l.getString("id"),
                    url = l.getString("url"),
                    sha256 = l.getString("sha256"),
                    size = l.getLong("size")
                )
            }
        return Manifest(
            version = obj.getString("version"),
            timestamp = obj.getLong("timestamp"),
            assets = assets,
            levels = levels
        )
    }

    private fun httpGet(url: String): String? {
        return runCatching {
            val conn = (java.net.URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = TIMEOUT
                readTimeout = TIMEOUT
                requestMethod = "GET"
                doInput = true
            }
            conn.inputStream.bufferedReader().use { it.readText() }.also { conn.disconnect() }
        }.getOrNull()
    }

    private fun download(url: String, outFile: File): Boolean {
        return runCatching {
            val conn = (java.net.URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = TIMEOUT
                readTimeout = TIMEOUT
                requestMethod = "GET"
                doInput = true
            }
            outFile.outputStream().use { fos ->
                conn.inputStream.copyTo(fos)
            }
            conn.disconnect()
        }.isSuccess
    }

    private fun sha256(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { fis ->
            val buf = ByteArray(8192)
            while (true) {
                val n = fis.read(buf)
                if (n <= 0) break
                md.update(buf, 0, n)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }
}