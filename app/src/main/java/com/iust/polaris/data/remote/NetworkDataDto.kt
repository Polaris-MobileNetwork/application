package com.iust.polaris.data.remote

import com.iust.polaris.data.local.NetworkMetric
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The top-level object for the sync request body.
 */
@Serializable
data class SyncRequestDto(
    val measurements: List<MeasurementDto>
)

/**
 * Represents a single measurement object in the JSON array.
 * The @SerialName annotation ensures the Kotlin property name maps to the correct JSON key.
 */
@Serializable
data class MeasurementDto(
    @SerialName("timeStamp") val timeStamp: Long,
    @SerialName("latitude") val latitude: Double?,
    @SerialName("longitude") val longitude: Double?,
    @SerialName("networkType") val networkType: String,
    @SerialName("plmnId") val plmnId: String?,
    @SerialName("lac") val lac: Int?,
    @SerialName("tac") val tac: Int?,
    @SerialName("rac") val rac: Int?,
    @SerialName("cellId") val cellId: String?,
    @SerialName("arfcn") val arfcn: Int?,
    @SerialName("frequencyBand") val frequencyBand: String?,
    @SerialName("actualFrequencyMhz") val actualFrequencyMhz: Double?,
    @SerialName("signalStrength") val signalStrength: Int,
    @SerialName("rsrp") val rsrp: Int?,
    @SerialName("rsrq") val rsrq: Int?,
    @SerialName("rscp") val rscp: Int?,
    @SerialName("rxlev") val rxlev: Int?,
    @SerialName("ecno") val ecno: Double?
)

/**
 * An extension function to easily convert a list of our local NetworkMetric database entities
 * into a list of MeasurementDto objects suitable for sending to the server.
 */
fun List<NetworkMetric>.toDto(): List<MeasurementDto> {
    return this.map { metric ->
        MeasurementDto(
            timeStamp = metric.timestamp,
            latitude = metric.latitude,
            longitude = metric.longitude,
            networkType = metric.networkType,
            plmnId = metric.plmnId,
            lac = metric.lac,
            tac = metric.tac,
            rac = metric.rac,
            cellId = metric.cellId,
            arfcn = metric.arfcn,
            frequencyBand = metric.frequencyBand,
            actualFrequencyMhz = metric.actualFrequencyMhz,
            signalStrength = metric.signalStrength,
            rsrp = metric.rsrp,
            rsrq = metric.rsrq,
            rscp = metric.rscp,
            rxlev = metric.rxlev,
            ecno = metric.ecno
        )
    }
}
