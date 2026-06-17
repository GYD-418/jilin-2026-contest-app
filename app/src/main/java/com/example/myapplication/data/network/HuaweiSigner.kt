package com.example.myapplication.data.network

import java.net.URI
import java.net.URLEncoder
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

data class SignedRequest(
    val authorization: String,
    val xSdkDate: String,
    val host: String
)

object HuaweiSigner {

    private const val ALGORITHM = "SDK-HMAC-SHA256"
    private const val EMPTY_BODY_SHA256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"

    fun sign(method: String, url: String, ak: String, sk: String, projectId: String, body: String = ""): SignedRequest {
        val uri = URI.create(url)
        val host = uri.host

        val timestamp = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date())

        // Canonical URI: URL-encode each segment + trailing /
        val canonicalUri = uri.rawPath.split("/").joinToString("/") { urlEncode(it) } + "/"

        // Body hash
        val bodyHash = if (body.isEmpty()) EMPTY_BODY_SHA256
        else sha256Hex(body)

        // Headers to sign
        val signedHeaders = mutableMapOf(
            "content-type" to "application/json",
            "host" to host,
            "x-project-id" to projectId,
            "x-sdk-date" to timestamp
        )
        val sortedKeys = signedHeaders.keys.sorted()
        val signedHeadersStr = sortedKeys.joinToString(";")
        val canonicalHeaders = sortedKeys.joinToString("\n") { "$it:${signedHeaders[it]}" } + "\n"

        // Build canonical request
        val canonicalRequest = "$method\n$canonicalUri\n\n$canonicalHeaders\n$signedHeadersStr\n$bodyHash"
        val hashedCanonicalRequest = sha256Hex(canonicalRequest)

        // String to sign
        val stringToSign = "$ALGORITHM\n$timestamp\n$hashedCanonicalRequest"

        // Signature: single HMAC-SHA256(SK, stringToSign)
        val signature = hmacSha256Hex(sk.toByteArray(Charsets.UTF_8), stringToSign.toByteArray(Charsets.UTF_8))

        val authorization = "$ALGORITHM Access=$ak, SignedHeaders=$signedHeadersStr, Signature=$signature"

        return SignedRequest(authorization = authorization, xSdkDate = timestamp, host = host)
    }

    private fun sha256Hex(data: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return bytesToHex(digest.digest(data.toByteArray(Charsets.UTF_8)))
    }

    private fun hmacSha256Hex(key: ByteArray, data: ByteArray): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return bytesToHex(mac.doFinal(data))
    }

    private fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun urlEncode(s: String): String {
        return URLEncoder.encode(s, "UTF-8")
            .replace("+", "%20")
            .replace("*", "%2A")
            .replace("%7E", "~")
    }
}
