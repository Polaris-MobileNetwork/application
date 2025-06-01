package com.iust.polaris.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.ConnectivityManager // Kept for potential future context
import android.os.Build
import android.os.IBinder
import android.telephony.CellIdentityGsm
import android.telephony.CellIdentityLte
import android.telephony.CellIdentityNr
import android.telephony.CellIdentityWcdma
import android.telephony.CellInfo
import android.telephony.CellInfoGsm
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellInfoWcdma
import android.telephony.CellSignalStrengthGsm
import android.telephony.CellSignalStrengthLte
import android.telephony.CellSignalStrengthNr
import android.telephony.CellSignalStrengthWcdma
import android.telephony.NetworkRegistrationInfo // For NR State constants
import android.telephony.ServiceState
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.iust.polaris.MainActivity
import com.iust.polaris.R
import com.iust.polaris.data.local.NetworkMetric
import com.iust.polaris.data.repository.NetworkMetricsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val NOTIFICATION_ID = 101
private const val NOTIFICATION_CHANNEL_ID = "PolarisNetworkMetricChannel"
private const val TAG = "NetworkMetricServiceLog"

@AndroidEntryPoint
class NetworkMetricService : Service() {

    @Inject
    lateinit var networkMetricsRepository: NetworkMetricsRepository

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private var collectionJob: Job? = null

    private lateinit var telephonyManager: TelephonyManager
    @Suppress("unused")
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var locationManager: LocationManager

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service onCreate()")
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        createNotificationChannel()
        Log.d(TAG, "System services initialized.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "Service onStartCommand received. Action: ${intent?.action}")
        when (intent?.action) {
            ACTION_START_COLLECTION -> {
                Log.i(TAG, "ACTION_START_COLLECTION received.")
                startForegroundServiceWithNotification()
                startMetricCollection()
            }
            ACTION_STOP_COLLECTION -> {
                Log.i(TAG, "ACTION_STOP_COLLECTION received.")
                stopMetricCollection()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                Log.i(TAG, "Service stopSelf() called.")
            }
            else -> {
                Log.w(TAG, "Unknown or null action received in onStartCommand: ${intent?.action}")
            }
        }
        return START_NOT_STICKY
    }

    private fun startForegroundServiceWithNotification() {
        Log.d(TAG, "startForegroundServiceWithNotification() called")
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntentFlags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, pendingIntentFlags)
        val notificationIcon = R.drawable.ic_network_notification
        Log.d(TAG, "Building notification...")
        val notification: Notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Polaris Cellular Monitor")
            .setContentText("Collecting mobile network data...")
            .setSmallIcon(notificationIcon)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        try {
            Log.d(TAG, "Calling startForeground with ID: $NOTIFICATION_ID")
            startForeground(NOTIFICATION_ID, notification)
            Log.i(TAG, "startForeground() called successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Error calling startForeground()", e)
        }
    }

    private fun createNotificationChannel() {
        val channelName = "Cellular Metric Collection"
        val channelDescription = "Channel for Polaris mobile network data collection service"
        val importance = NotificationManager.IMPORTANCE_LOW
        val serviceChannel = NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, importance).apply {
            description = channelDescription
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(serviceChannel)
        Log.d(TAG, "Notification channel '$NOTIFICATION_CHANNEL_ID' created or already exists.")
    }

    private fun startMetricCollection() {
        if (collectionJob?.isActive == true) {
            Log.d(TAG, "Metric collection job is already active.")
            return
        }
        Log.i(TAG, "Starting metric collection loop (cellular focus).")
        collectionJob = serviceScope.launch {
            while (isActive) {
                Log.d(TAG, "Collection loop: gathering cellular metrics.")
                gatherAndStoreCellularMetricData()
                delay(COLLECTION_INTERVAL_MS)
            }
            Log.i(TAG, "Metric collection loop ended (isActive is false).")
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun gatherAndStoreCellularMetricData() {
        Log.d(TAG, "gatherAndStoreCellularMetricData() called")

        var currentLatitude: Double? = null
        var currentLongitude: Double? = null
        if (hasLocationPermission()) {
            try {
                val location: Location? = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                location?.let {
                    currentLatitude = it.latitude
                    currentLongitude = it.longitude
                } ?: Log.w(TAG, "Location data not available (getLastKnownLocation returned null).")
            } catch (e: SecurityException) { Log.e(TAG, "SecurityException getting location", e)
            } catch (e: Exception) { Log.e(TAG, "Error getting location", e) }
        } else {
            Log.w(TAG, "Location permission not granted. Location data will be null.")
        }

        val cellularNetworkTypeString = getCellularNetworkTypeString()
        Log.d(TAG, "Cellular Network Type: $cellularNetworkTypeString")

        var primarySignalStrength = -999
        var cellIdString: String? = null
        var plmnIdString: String? = null
        var lacInt: Int? = null
        var tacInt: Int? = null
        var rsrp: Int? = null
        var rsrq: Int? = null
        var rscp: Int? = null
        var ecno: Double? = null
        var rxlev: Int? = null
        var arfcnInt: Int? = null
        var bandString: String? = null

        try {
            val serviceState = telephonyManager.serviceState
            if (serviceState?.state != ServiceState.STATE_IN_SERVICE && serviceState?.state != ServiceState.STATE_EMERGENCY_ONLY) {
                Log.w(TAG, "Cellular service not in service. State: ${serviceState?.state}. Cell info might be limited or stale.")
            }

            val cellInfoList: List<CellInfo>? = telephonyManager.allCellInfo
            if (cellInfoList.isNullOrEmpty()) {
                Log.w(TAG, "TelephonyManager.allCellInfo returned null or empty list.")
            } else {
                val registeredCellInfo = cellInfoList.firstOrNull { it.isRegistered }
                if (registeredCellInfo != null) {
                    Log.d(TAG, "Processing registered cell: ${registeredCellInfo::class.java.simpleName}")
                    plmnIdString = getPlmnIdFromCellInfo(registeredCellInfo)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        when (registeredCellInfo) {
                            is CellInfoLte -> {
                                val ci = registeredCellInfo.cellIdentity
                                val cs = registeredCellInfo.cellSignalStrength
                                primarySignalStrength = cs.dbm
                                rsrp = cs.rsrp.takeUnlessInvalid()
                                rsrq = cs.rsrq.takeUnlessInvalid()
                                cellIdString = ci.ci.takeUnlessInvalid()?.toString()
                                tacInt = ci.tac.takeUnlessInvalid()
                                arfcnInt = ci.earfcn.takeUnlessInvalid()
                                bandString = ci.bands.joinToString().takeIf { it.isNotEmpty() }
                            }
                            is CellInfoGsm -> {
                                val ci = registeredCellInfo.cellIdentity
                                val cs = registeredCellInfo.cellSignalStrength
                                primarySignalStrength = cs.dbm
                                rxlev = cs.asuLevel.let { if (it == 99 || it < 0) null else -113 + (2 * it) }
                                cellIdString = ci.cid.takeUnlessInvalid()?.toString()
                                lacInt = ci.lac.takeUnlessInvalid()
                                arfcnInt = ci.arfcn.takeUnlessInvalid()
                            }
                            is CellInfoWcdma -> {
                                val ci = registeredCellInfo.cellIdentity
                                val cs = registeredCellInfo.cellSignalStrength
                                primarySignalStrength = cs.dbm
                                rscp = cs.asuLevel.let { if (it == 99 || it < -5) null else -116 + it }
                                cellIdString = ci.cid.takeUnlessInvalid()?.toString()
                                lacInt = ci.lac.takeUnlessInvalid()
                                arfcnInt = ci.uarfcn.takeUnlessInvalid()
                            }
                            is CellInfoNr -> {
                                val ci = registeredCellInfo.cellIdentity as CellIdentityNr
                                val cs = registeredCellInfo.cellSignalStrength as CellSignalStrengthNr
                                rsrp = (cs.csiRsrp.takeUnlessInvalid() ?: cs.ssRsrp.takeUnlessInvalid())
                                rsrq = (cs.csiRsrq.takeUnlessInvalid() ?: cs.ssRsrq.takeUnlessInvalid())
                                primarySignalStrength = rsrp ?: primarySignalStrength
                                cellIdString = ci.nci.takeUnlessInvalidL()?.toString()
                                tacInt = ci.tac.takeUnlessInvalid()
                                arfcnInt = ci.nrarfcn.takeUnlessInvalid()
                                bandString = ci.bands.joinToString().takeIf { it.isNotEmpty() }
                            }
                            else -> Log.w(TAG, "Unhandled registered cell type: ${registeredCellInfo::class.java.simpleName}")
                        }
                    } else {
                        when (registeredCellInfo) {
                            is CellInfoLte -> {
                                val ci = registeredCellInfo.cellIdentity
                                val cs = registeredCellInfo.cellSignalStrength
                                primarySignalStrength = cs.dbm
                                rsrp = cs.rsrp.takeUnlessInvalid()
                                rsrq = cs.rsrq.takeUnlessInvalid()
                                cellIdString = ci.ci.takeUnlessInvalid()?.toString()
                                tacInt = ci.tac.takeUnlessInvalid()
                                arfcnInt = ci.earfcn.takeUnlessInvalid()
                                bandString = null
                            }
                            is CellInfoGsm -> {
                                val ci = registeredCellInfo.cellIdentity
                                val cs = registeredCellInfo.cellSignalStrength
                                primarySignalStrength = cs.dbm
                                rxlev = cs.asuLevel.let { if (it == 99 || it < 0) null else -113 + (2 * it) }
                                cellIdString = ci.cid.takeUnlessInvalid()?.toString()
                                lacInt = ci.lac.takeUnlessInvalid()
                                arfcnInt = ci.arfcn.takeUnlessInvalid()
                            }
                            is CellInfoWcdma -> {
                                val ci = registeredCellInfo.cellIdentity
                                val cs = registeredCellInfo.cellSignalStrength
                                primarySignalStrength = cs.dbm
                                rscp = cs.asuLevel.let { if (it == 99 || it < -5) null else -116 + it }
                                cellIdString = ci.cid.takeUnlessInvalid()?.toString()
                                lacInt = ci.lac.takeUnlessInvalid()
                                arfcnInt = ci.uarfcn.takeUnlessInvalid()
                            }
                            else -> Log.w(TAG, "Unhandled registered cell type: ${registeredCellInfo::class.java.simpleName}")
                        }
                    }
                    Log.d(TAG, "Registered Cell Parsed: Type=$cellularNetworkTypeString, Signal=$primarySignalStrength, CellID=$cellIdString, PLMN=$plmnIdString, LAC=$lacInt, TAC=$tacInt, RSRP=$rsrp, RSRQ=$rsrq")
                } else {
                    Log.w(TAG, "No registered cell found in cellInfoList.")
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException getting Telephony info", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting Telephony info", e)
        }

        val metric = NetworkMetric(
            timestamp = System.currentTimeMillis(),
            networkType = cellularNetworkTypeString,
            signalStrength = primarySignalStrength,
            latitude = currentLatitude,
            longitude = currentLongitude,
            cellId = cellIdString,
            plmnId = plmnIdString,
            lac = lacInt,
            tac = tacInt,
            rac = null,
            arfcn = arfcnInt,
            frequencyBand = bandString,
            actualFrequencyMhz = null,
            rsrp = rsrp,
            rsrq = rsrq,
            rscp = rscp,
            ecno = ecno,
            rxlev = rxlev,
            isUploaded = false
        )

        try {
            networkMetricsRepository.insertMetric(metric)
            Log.i(TAG, "Successfully inserted metric: Type=${metric.networkType}, Signal=${metric.signalStrength}")
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting metric into database", e)
        }
    }

    private fun getPlmnIdFromCellInfo(cellInfo: CellInfo): String? {
        // mccString/mncString available from API 28 (P)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return when (cellInfo) {
                is CellInfoGsm -> cellInfo.cellIdentity.mccString?.let { mcc -> cellInfo.cellIdentity.mncString?.let { mnc -> "$mcc-$mnc" } }
                is CellInfoLte -> cellInfo.cellIdentity.mccString?.let { mcc -> cellInfo.cellIdentity.mncString?.let { mnc -> "$mcc-$mnc" } }
                is CellInfoWcdma -> cellInfo.cellIdentity.mccString?.let { mcc -> cellInfo.cellIdentity.mncString?.let { mnc -> "$mcc-$mnc" } }
                is CellInfoNr ->
                    (cellInfo.cellIdentity as? CellIdentityNr)?.mccString?.let { mcc -> (cellInfo.cellIdentity as? CellIdentityNr)?.mncString?.let { mnc -> "$mcc-$mnc" } }
                else -> null
            }
        } else {
            return when (cellInfo) {
                is CellInfoGsm -> cellInfo.cellIdentity.mccString?.let { mcc -> cellInfo.cellIdentity.mncString?.let { mnc -> "$mcc-$mnc" } }
                is CellInfoLte -> cellInfo.cellIdentity.mccString?.let { mcc -> cellInfo.cellIdentity.mncString?.let { mnc -> "$mcc-$mnc" } }
                is CellInfoWcdma -> cellInfo.cellIdentity.mccString?.let { mcc -> cellInfo.cellIdentity.mncString?.let { mnc -> "$mcc-$mnc" } }
                else -> null
            }
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasReadPhoneStatePermission(): Boolean {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    private fun getCellularNetworkTypeString(): String {
        if (telephonyManager.simState != TelephonyManager.SIM_STATE_READY) {
            Log.w(TAG, "SIM not ready or absent. State: ${telephonyManager.simState}")
            return "NO_SIM_OR_DISABLED"
        }
        val serviceState = telephonyManager.serviceState
        if (serviceState == null) {
            Log.w(TAG, "ServiceState is null.")
            return "SERVICE_STATE_NULL"
        }
        // Use getter methods for voice and data registration state

//        if (serviceState.state != ServiceState.STATE_IN_SERVICE && serviceState.state != ServiceState.STATE_EMERGENCY_ONLY) {
//            Log.w(TAG, "Not in service. Voice state: ${serviceState.voiceRegState}, Data state: ${serviceState.dataRegState}")
//            return "NO_SERVICE (State: ${serviceState.state})"
//        }

        var networkType = TelephonyManager.NETWORK_TYPE_UNKNOWN
        if (hasReadPhoneStatePermission()) {
            try {
                networkType = telephonyManager.dataNetworkType
                Log.d(TAG, "Using dataNetworkType: $networkType (with READ_PHONE_STATE)")
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException getting dataNetworkType, falling back.", e)
                networkType = telephonyManager.voiceNetworkType
                Log.d(TAG, "Fell back to voiceNetworkType: $networkType")
            }
        } else if (hasLocationPermission() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // API 31
            networkType = telephonyManager.voiceNetworkType
            Log.w(TAG, "READ_PHONE_STATE missing, using voiceNetworkType as fallback on API 31+: $networkType")
        } else {
            Log.w(TAG, "Insufficient permissions for detailed network type.")
            return "CELLULAR (PermIssue)"
        }

        return when (networkType) {
            TelephonyManager.NETWORK_TYPE_LTE -> "LTE"
//                {
//                // Check for 5G NSA connection (LTE anchor with NR secondary)
//                // ServiceState.getNetworkRegistrationInfoList() is API 30
//                // ServiceState.nrState is API 30
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//                    var isNsa = false
//                    // Check NR state directly from ServiceState if available (API 30+)
//                    if (serviceState.nrState == TelephonyManager.NETWORK_REGISTRATION_INFO_NR_STATE_CONNECTED_ENDC ||
//                        serviceState.nrState == TelephonyManager.NETWORK_REGISTRATION_INFO_NR_STATE_CONNECTED_SA_MODE // Though SA mode would usually report NETWORK_TYPE_NR
//                    ) {
//                        isNsa = true
//                    }
//                    // More robust check using NetworkRegistrationInfo list
//                    telephonyManager.getNetworkRegistrationInfoList(ServiceState.DOMAIN_PS, null)
//                        .filter { it.accessNetworkTechnology == TelephonyManager.NETWORK_TYPE_NR && it.isRegistered }
//                        .forEach { _ -> isNsa = true }
//
//                    if (isNsa) "5G_NSA" else "LTE"
//                } else {
//                    "LTE" // No NR state info available below API 30
//                }
//            }
            TelephonyManager.NETWORK_TYPE_NR -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) "5G_SA" else "UNKNOWN_CELL_TYPE"
            TelephonyManager.NETWORK_TYPE_HSPAP -> "HSPA+"
            TelephonyManager.NETWORK_TYPE_UMTS -> "UMTS"
            TelephonyManager.NETWORK_TYPE_EDGE -> "EDGE"
            TelephonyManager.NETWORK_TYPE_GPRS -> "GPRS"
            TelephonyManager.NETWORK_TYPE_GSM -> "GSM"
            TelephonyManager.NETWORK_TYPE_IWLAN -> "IWLAN"
            TelephonyManager.NETWORK_TYPE_UNKNOWN -> if (serviceState.state == ServiceState.STATE_IN_SERVICE) "CELLULAR_IN_SERVICE" else "UNKNOWN_CELL_TYPE"
            else -> "CELLULAR_CODE_${networkType}"
        }
    }

    private fun stopMetricCollection() {
        Log.i(TAG, "Stopping metric collection job.")
        collectionJob?.cancel()
        collectionJob = null
        Log.d(TAG, "Metric collection job cancelled and set to null.")
    }

    override fun onBind(intent: Intent): IBinder? {
        Log.d(TAG, "Service onBind()")
        return null
    }

    override fun onDestroy() {
        Log.i(TAG, "Service onDestroy()")
        stopMetricCollection()
        serviceJob.cancel()
        Log.d(TAG, "Service scope cancelled.")
        super.onDestroy()
    }

    companion object {
        const val ACTION_START_COLLECTION = "com.iust.polaris.service.action.START_COLLECTION"
        const val ACTION_STOP_COLLECTION = "com.iust.polaris.service.action.STOP_COLLECTION"
        private const val COLLECTION_INTERVAL_MS = 15000L

        fun startService(context: Context) {
            Log.i(TAG, "Companion: Attempting to start service.")
            val startIntent = Intent(context, NetworkMetricService::class.java).apply {
                action = ACTION_START_COLLECTION
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(startIntent)
            } else {
                context.startService(startIntent)
            }
            Log.d(TAG, "Companion: startForegroundService/startService called.")
        }

        fun stopService(context: Context) {
            Log.i(TAG, "Companion: Attempting to stop service.")
            val stopIntent = Intent(context, NetworkMetricService::class.java).apply {
                action = ACTION_STOP_COLLECTION
            }
            context.startService(stopIntent)
            Log.d(TAG, "Companion: startService(stopIntent) called.")
        }
    }
}

// Helper extension functions to handle invalid API return values
private fun Int.takeUnlessInvalid(): Int? = if (this == Int.MAX_VALUE) null else this
private fun Long.takeUnlessInvalidL(): Long? = if (this == Long.MAX_VALUE) null else this
