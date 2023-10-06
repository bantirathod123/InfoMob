package com.charyatani.InfoMob

import android.Manifest
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.charyatani.InfoMob.model.DeviceInfo
import com.google.firebase.FirebaseOptions
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var databaseReference: DatabaseReference
    private lateinit var firestore: FirebaseFirestore
    private lateinit var sharedPreferences: SharedPreferences

    private lateinit var timestampTextView: TextView
    private lateinit var captureCountTextView: TextView
    private lateinit var frequencyTextView: TextView
    private lateinit var connectivityTextView: TextView
    private lateinit var batteryChargingTextView: TextView
    private lateinit var batteryChargeTextView: TextView
    private lateinit var locationTextView: TextView
    private lateinit var latitudeTextView: TextView
    private lateinit var longitudeTextView: TextView
    private lateinit var manualRefreshButton: Button

    private var captureCount: Int = 0

    private lateinit var locationManager: LocationManager
    private val locationPermissionCode = 1

    private val locationListener: LocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            updateLocationUI(location.latitude, location.longitude)
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    }

    private val timer = Timer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initFirebase()
        initViews()
        initListeners()

        // Initialize locationManager here
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        // Initialize SharedPreferences for capture count
        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)

        // Reset captureCount to 0 when the app is launched
        captureCount = 0

        checkAndRequestLocationPermission()
        scheduleDataCapture()
    }

    private fun initFirebase() {
        val options = FirebaseOptions.Builder()
            .setApplicationId("1:500899905042:android:8d462e737f42d2134ae2cb")
            .setApiKey("AIzaSyCnF43agAU5ctbbpERUNoSsHp5UApgdeuA")
            .setProjectId("infomob-1b930")
            .setDatabaseUrl("https://infomob-1b930-default-rtdb.firebaseio.com")
            .build()

        // FirebaseApp.initializeApp(this, options)

        databaseReference = FirebaseDatabase.getInstance().reference.child("mobileData")
        firestore = FirebaseFirestore.getInstance()
    }

    private fun initViews() {
        timestampTextView = findViewById(R.id.timestampValue)
        captureCountTextView = findViewById(R.id.captureCountValue)
        frequencyTextView = findViewById(R.id.frequencyValue)
        connectivityTextView = findViewById(R.id.connectivityValue)
        batteryChargingTextView = findViewById(R.id.batteryChargingValue)
        batteryChargeTextView = findViewById(R.id.batteryChargeValue)
        locationTextView = findViewById(R.id.locationValue)
        latitudeTextView = findViewById(R.id.latitudeValue)
        longitudeTextView = findViewById(R.id.longitudeValue)
        manualRefreshButton = findViewById(R.id.manualRefreshButton)
    }

    private fun initListeners() {
        manualRefreshButton.setOnClickListener { captureData() }
        frequencyTextView.setOnClickListener { showFrequencyInputDialog() }
    }

    private fun checkAndRequestLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                locationPermissionCode
            )
        }
    }

    private fun scheduleDataCapture() {
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                captureData()
            }
        }, 0, 60000)
    }

    private fun updateLocationUI(latitude: Double, longitude: Double) {
        latitudeTextView.text = "Lat: $latitude"
        longitudeTextView.text = "Lon: $longitude"
    }

    private fun captureData() {
        val timestamp = Date().toString()
        val frequency = 15

        // Capture location information
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            runOnUiThread {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    0,
                    0f,
                    locationListener
                )
            }
        }

        val batteryLevel = getBatteryLevel()

        // Check if the battery level is below 20%
        if (batteryLevel < 20) {
            // Battery is low, send a notification
            sendLowBatteryNotification()
        }

        val deviceInfo = DeviceInfo(
            timestamp,
            captureCount,
            frequency,
            isConnected(),
            isCharging(),
            getBatteryLevel(),
        )

        databaseReference.setValue(deviceInfo)

        firestore.collection("mobileData")
            .document(timestamp)
            .set(deviceInfo)
            .addOnSuccessListener {
                // Data successfully stored in Firestore
            }
            .addOnFailureListener {
                // Handle failure to store data in Firestore
            }

        runOnUiThread {
            updateUI(deviceInfo)
            captureCount++
            captureCountTextView.text = "$captureCount"
            saveCaptureCount()
        }
    }

    private fun sendLowBatteryNotification() {
        // You can use Android's Notification system to send a notification here
        // Create a Notification and send it when the battery is low
        // Here's a basic example of how to create and show a notification:

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "low_battery_channel"
        val channelName = "Low Battery Channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_low_battery)
            .setContentTitle("Low Battery")
            .setContentText("Your battery is below 20%. Please charge your device.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        val notificationId = 1
        notificationManager.notify(notificationId, notificationBuilder.build())
    }


    private fun isConnected(): Boolean {
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo: NetworkInfo? = connectivityManager.activeNetworkInfo
        return networkInfo?.isConnected == true
    }

    private fun isCharging(): Boolean {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            applicationContext.registerReceiver(null, ifilter)
        }
        return batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) != 0
    }

    private fun getBatteryLevel(): Int {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            applicationContext.registerReceiver(null, ifilter)
        }
        return batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    }

    private fun showFrequencyInputDialog() {
        val inputDialog = AlertDialog.Builder(this)
        inputDialog.setTitle("Set Frequency (min)")

        val inputField = EditText(this)
        inputField.hint = "Enter frequency in minutes"
        inputDialog.setView(inputField)

        inputDialog.setPositiveButton("Set") { dialog, which ->
            val inputText = inputField.text.toString()
            val newFrequency = inputText.toIntOrNull()

            if (newFrequency != null) {
                frequencyTextView.text = "Frequency (min): $newFrequency"
                updateFirebaseFrequency(newFrequency)
            } else {
                Toast.makeText(
                    this,
                    "Invalid input. Please enter a valid number.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        inputDialog.setNegativeButton("Cancel") { dialog, which ->
            // User canceled the input
        }

        inputDialog.show()
    }

    private fun updateFirebaseFrequency(newFrequency: Int) {
        // Update the Firebase Realtime Database or Firestore with the new frequency value
        // You need to implement this part based on your Firebase structure
    }

    private fun saveCaptureCount() {
        sharedPreferences.edit().putInt("captureCount", captureCount).apply()
    }

    private fun loadCaptureCount() {
        captureCount = sharedPreferences.getInt("", 0)
    }

    private fun updateUI(data: DeviceInfo?) {
        if (data != null) {
            timestampTextView.text = "${data.timestamp}"
            captureCountTextView.text = "${data.captureCount}"
            frequencyTextView.text = "${data.frequency}"
            connectivityTextView.text = if (data.connectivity) "ON" else "OFF"
            batteryChargingTextView.text = if (data.batteryCharging) "ON" else "OFF"
            batteryChargeTextView.text = "${data.batteryCharge}%"
            latitudeTextView.text = "Lat: ${data.latitude}"
            longitudeTextView.text = "Lon: ${data.longitude}"
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        // Schedule a task to capture and update data every 1 minute (adjust the interval as needed)
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                captureData() // Capture data
            }
        }, 0, 60000) // Capture data every 1 minute (60000 milliseconds)
    }
}