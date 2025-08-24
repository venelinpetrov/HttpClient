package com.vpe
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.nio.file.Files
import java.nio.file.Paths
/*

$cartId = d8c3d96a-577f-4d50-98e1-4252c9a74412
POST http://localhost:8080/carts/{{cartId}}/items
Content-Type: application/json
{
    "productId": 2
}

*/
fun main() {
    val filePath = "./request.txt"
    val lines = Files.readAllLines(Paths.get(filePath))
    val variables = mutableMapOf<String, String>()
    var method = ""
    var url = ""
    val headers = mutableMapOf<String, String>()
    val bodyBuilder = StringBuilder()

    var parsingHeaders = false
    var parsingBody = false

    for (line in lines) {
        when {
            // Variables
            line.startsWith("$") -> {
                val (name, value) = line.substring(1).split("=", limit = 2)
                variables[name.trim()] = value.trim()
            }
            // URL
            line.matches(Regex("^(GET|POST|PUT|PATCH|DELETE) .*")) -> {
                val parts = line.split(" ", limit = 2)
                method = parts[0]
                url = parts[1].substitute(variables)
                parsingHeaders = true
            }
            // Headers
            parsingHeaders && line.isNotBlank() -> {
                try {
                    val (headerName, headerValue) = line.split(":", limit = 2)
                    headers[headerName.trim()] = headerValue.substitute(variables).trim()
                } catch (e: Exception) {
                    parsingHeaders = false
                    parsingBody = true
                    bodyBuilder.appendLine(line)
                }
            }
            parsingHeaders && line.isBlank() -> {
                parsingHeaders = false
                parsingBody = true
            }
            parsingBody -> {
                bodyBuilder.appendLine(line)
            }
        }
    }

    val client = OkHttpClient()
    val body = if (bodyBuilder.isNotEmpty())
        bodyBuilder.toString().toRequestBody((headers["Content-Type"] ?: "application/json").toMediaType())
    else null

    val requestBuilder = Request.Builder().url(url)
    headers.forEach { (k, v) -> requestBuilder.addHeader(k, v) }

    when (method) {
        "POST" -> requestBuilder.post(body!!)
        "PUT" -> requestBuilder.put(body!!)
        "PATCH" -> requestBuilder.patch(body!!)
        "DELETE" -> requestBuilder.delete(body!!)
        "GET" -> requestBuilder.get()
    }

    val response = client.newCall(requestBuilder.build()).execute()
    println("Status: ${response.code}")
    println("Body: ${response.body}")
}

fun String.substitute(vars: Map<String, String>): String =
    vars.entries.fold(this) { acc, (name, value) ->
        acc.replace("{{${name}}}", value)
    }
