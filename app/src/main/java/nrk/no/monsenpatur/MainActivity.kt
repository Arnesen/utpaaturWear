package nrk.no.monsenpatur

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.wearable.activity.WearableActivity
import com.google.android.gms.location.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.*
import kotlin.concurrent.fixedRateTimer


class MainActivity : WearableActivity(), SensorEventListener {


    var client = OkHttpClient()

    private lateinit var fusedLocationClient: FusedLocationProviderClient


    private var lat: Double? = null
    private var long: Double? = null

    private  var locationCallback: LocationCallback? = null


    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
    }

    private fun hasGps(): Boolean =
            packageManager.hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS)


    override fun onSensorChanged(event: SensorEvent) {
        when {
            event.sensor.type == Sensor.TYPE_HEART_RATE -> {
                val msg = "" + event.values[0].toInt()
                heartrate.text = msg

            }
            event.sensor.type == Sensor.TYPE_STEP_COUNTER -> {
                val msg = "" + event.values[0].toInt()
                steps.text = msg
            }
            event.sensor.type == Sensor.TYPE_STEP_DETECTOR -> {
                // do something when we detect a step
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            50 -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    setupSensors()
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    finish()
                }
                return
            }

        // Add other 'when' lines to check for other
        // permissions this app might request.
            else -> {
                // Ignore all other requests.
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fixedRateTimer(startAt = Date(), period = 5000, action = {

            var url = "http://137.117.158.207/log?pulse=" + heartrate.text

            if (steps.text.isNotEmpty()) {
                url = "$url&steps=${steps.text}"
            }

            if (lat != null) {
                url = "$url&lat=$lat"
            }
            if (long != null) {
                url = "$url&lng=$long"
            }

            val request = Request.Builder()
                    .url(url)
                    .build()
            async(CommonPool) {
                client.newCall(request).execute()
            }

        })
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.BODY_SENSORS, Manifest.permission.ACCESS_FINE_LOCATION),
                    50)
        } else {
            setupSensors()
        }


        // Enables Always-on
        setAmbientEnabled()
    }

    private fun setupSensors() {
        val mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val mHeartRateSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)
        val mStepCountSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        val mStepDetectSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)

        mSensorManager.registerListener(this, mHeartRateSensor, SensorManager.SENSOR_DELAY_NORMAL)
        mSensorManager.registerListener(this, mStepCountSensor, SensorManager.SENSOR_DELAY_NORMAL)
        mSensorManager.registerListener(this, mStepDetectSensor, SensorManager.SENSOR_DELAY_NORMAL)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED && hasGps()) {

//            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
//                lat = location?.latitude
//                long = location?.longitude
//                locationText.text = "$lat : $long"
//            }
            locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult?) {
                    locationResult ?: return
                    for (location in locationResult.locations) {
                        // Update UI with location data
                        // ...

                        lat = location?.latitude
                        long = location?.longitude
                        locationText.text = "$lat : $long"
                    }
                }
            }
            fusedLocationClient.requestLocationUpdates(LocationRequest(),
                    locationCallback,
                    null /* Looper */)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}
