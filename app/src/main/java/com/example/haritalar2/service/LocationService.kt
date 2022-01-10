package com.example.haritalar2.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.example.haritalar2.*
import com.example.haritalar2.R
import com.google.android.gms.location.*
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase


class LocationService: Service() {

    lateinit var mFusedLocationProviderClient: FusedLocationProviderClient
    val UPDATE_INTERVAL: Long = 9000
    val FASTEST_INTERVAL: Long = 8000

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }


    override fun onCreate() {
        super.onCreate()
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        if (Build.VERSION.SDK_INT >= 26){
            val channelID = "my_channel_26"
            val channel = NotificationChannel(channelID, "My Channel", NotificationManager.IMPORTANCE_DEFAULT)
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
            val builder = NotificationCompat.Builder(this, channelID)
                .setContentTitle("")
                .setContentText("")
                .setSmallIcon(R.drawable.ic_map).build()
            startForeground(1, builder)//ONLY FOR SERVICES THAT USE GPS
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(Sabit.TAG, "onStartCommand: called")
        getLocation()
        return START_NOT_STICKY//RUNS AS LONG AS GET LOCATION RUNS

    }
    private fun getLocation(){
        //LocationRequest is used for retrieving the location at an interval
        val locationRequestHighAccuracy = LocationRequest.create()
        locationRequestHighAccuracy.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequestHighAccuracy.interval = UPDATE_INTERVAL
        locationRequestHighAccuracy.fastestInterval = FASTEST_INTERVAL

        //if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            Log.d(Sabit.TAG, "getLocation: stopping getLocation")
            stopSelf()
            return
        }
        Log.d(Sabit.TAG, "getLocation: getting location information")
        mFusedLocationProviderClient.requestLocationUpdates(
            locationRequestHighAccuracy,
            object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    //super.onLocationResult(p0)
                    Log.d(Sabit.TAG, "onLocationResult: get location result")
                    val location: Location = locationResult.lastLocation

                    val user : User? = UserClient.getUser()
                    val geoPoint = GeoPoint(location.latitude, location.longitude)
                    val userLocation = UserLocation(geoPoint, null, user)
                    saveUserLocation(userLocation)

                }
            }, Looper.myLooper()
        )


    }
    private fun saveUserLocation(userLocation: UserLocation){

        try {
            val db = Firebase.firestore
            val locationRef: DocumentReference = db.collection("user_locations")
                .document(Sabit.auth.uid!!)

            locationRef.set(userLocation).addOnCompleteListener {
                if (it.isSuccessful){
                    Log.d(Sabit.TAG, "saveUserLocation: saved to database\n" +
                            "${userLocation.geo_point!!.latitude}\n${userLocation.geo_point!!.longitude}")

                }
            }

        }catch (e: NullPointerException){
            Log.d(Sabit.TAG, "saveUserLocation: user is null")
            stopSelf()
        }

    }
}