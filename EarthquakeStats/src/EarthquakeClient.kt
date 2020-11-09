package com.github.fstien

import com.github.fstien.ktor.header.forwarding.HeaderForwardingClient
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.apache.*
import io.ktor.client.features.json.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

class EarthquakeClient {
    private val client = HttpClient(Apache) {
        install(JsonFeature) {
            serializer = JacksonSerializer {}
        }

        install(HeaderForwardingClient)
    }

    private suspend fun getEarthquakes(): List<Earthquake> {
        val call: HttpStatement = client.get("http://earthquake-adaptor:8080/earthquakes")

        val earthquakeResponse: List<Earthquake> = call.execute {
            when(it.status) {
                HttpStatusCode.OK -> it.receive()
                else -> throw Exception("Error response received from earquakes.usgs ${it.status}")
            }
        }

        return earthquakeResponse
    }

    suspend fun getLatest(): Earthquake {
        val earthquakes = getEarthquakes()
        val latest = earthquakes.first()
        return latest
    }

    suspend fun getBiggest(): Earthquake {
        val earthquakes = getEarthquakes()
        val biggest = earthquakes.sortedBy { it.magnitude }.last()
        return biggest
    }

    suspend fun getBiggerThan(threshold: Double): List<Earthquake> {
        val earthquakes = getEarthquakes()
        val biggerThan = earthquakes.filter { it.magnitude > threshold }
        return biggerThan
    }
}

data class Earthquake(
    val location: String,
    val magnitude: Double,
    val timeGMT: String
)