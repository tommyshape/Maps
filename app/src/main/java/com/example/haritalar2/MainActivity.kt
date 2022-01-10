package com.example.haritalar2

import android.Manifest
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.app.ActivityManager
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.IntentSender.SendIntentException
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.haritalar2.service.LocationService
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.lang.RuntimeException

private const val TAG: String = "my_map"
private val LOCATION_PERMISSION_CODE = 1

class MainActivity : AppCompatActivity() {

    lateinit var mFusedLocationProviderClient: FusedLocationProviderClient
    lateinit var logoutButton: FloatingActionButton
    lateinit var showInfoButton: Button
    private val db = Firebase.firestore
    lateinit var mUser: User
    lateinit var mGeoPoint: GeoPoint
    lateinit var context: Context

    lateinit var mUserLocationList: ArrayList<UserLocation>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        logoutButton = findViewById(R.id.logout_button)
        showInfoButton = findViewById(R.id.showInfoButton)

        logoutButton.setOnClickListener {
            Sabit.auth.signOut()
            startActivity(Intent(this, RegisterActivity::class.java))
            finish()
        }

        getAllUsersLocation()
        checkPermission()

    }

    fun getAllUsersLocation(){
        mUserLocationList = ArrayList()
        val usersRef: CollectionReference = db.collection("user_locations")
        usersRef.addSnapshotListener { value, error ->
            value?.forEach {
                val userLocation: UserLocation = it.toObject(UserLocation::class.java)
                mUserLocationList.add(userLocation)
            }
            UserClient.setList(mUserLocationList)
        }
    }

    private fun checkPermission(){
        if (ContextCompat.checkSelfPermission(this,
                ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "You have already granted location permission!", Toast.LENGTH_SHORT).show()
            enableGps("checkPermission")



        } else {
            requestLocationPermission()
        }
    }

    private fun enableGps(place: String) {
        //to enable gps at the launch of application
        Log.d(TAG, "enableGps from $place")
        val locationRequest: LocationRequest = LocationRequest.create()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)

        val result: Task<LocationSettingsResponse> =
            LocationServices.getSettingsClient(this).checkLocationSettings(builder.build())


        result.addOnCompleteListener { task ->
            try {
                val response = task.getResult(ApiException::class.java)
                // All location settings are satisfied. The client can initialize location
                // requests here.

                //getLastKnownLocation()//bu calisiyor
                Handler(Looper.getMainLooper()).postDelayed({
                    // Your Code
                    //getLastKnownLocation()
                    getUserDetails()
                }, 3000)


            } catch (exception: ApiException) {
                when (exception.statusCode) {
                    LocationSettingsStatusCodes.RESOLUTION_REQUIRED ->
                        // Location settings are not satisfied. But could be fixed by showing the
                        // user a dialog.
                        try {
                            // Cast to a resolvable exception.
                            val resolvable = exception as ResolvableApiException
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            resolvable.startResolutionForResult(
                                this,
                                LocationRequest.PRIORITY_HIGH_ACCURACY
                            )


                        } catch (e: SendIntentException) {
                            // Ignore the error.
                        } catch (e: ClassCastException) {
                            // Ignore, should be an impossible error.
                        }
                    LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                    }
                }
            }
        }
    }

    private fun requestLocationPermission(){
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, ACCESS_FINE_LOCATION)) {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Permission needed")
            builder.setMessage("This permission is needed to locate your device location")
            builder.setPositiveButton("Ok"){_,_ ->
                ActivityCompat.requestPermissions(this,
                    arrayOf(ACCESS_FINE_LOCATION), LOCATION_PERMISSION_CODE)
            }
            builder.setNegativeButton("Cancel"){_,_ -> }
            builder.create().show()
        }
        else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_CODE
            )
        }
    }

    private fun startLocationService(){
        if (!isLocationServiceRunning()){
            val serviceIntent: Intent = Intent(this, LocationService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O){
                Log.d(TAG, "startLocatationService: 26 or higher")
                this.startForegroundService(serviceIntent)
            }
            else{
                Log.d(TAG, "startLocatationService: lower than 26")
                startService(serviceIntent)
            }
        }
    }

    private fun isLocationServiceRunning(): Boolean {
        val manager: ActivityManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        for (service: ActivityManager.RunningServiceInfo in manager.getRunningServices(Integer.MAX_VALUE)){
            if ("com.example.haritalar2.service.LocationService" == service.service.className){
                Log.d(TAG, "isLocationServiceRunning: location service is already running")
                return true
            }
        }
        Log.d(TAG, "isLocationServiceRunning: location service is not running")
        return false
    }




    //GPS acildiktan sonra
    private fun getUserDetails(){//1

            Log.d(TAG, "getUserDetails: getUserDetails calisti")
            val userRef: DocumentReference = db.collection("maps_users").document(Sabit.auth.uid.toString())

            userRef.get(Source.CACHE).addOnCompleteListener { task ->
                if (task.isSuccessful){
                    Log.d(TAG, "getUserDetails: Successfully got the user details")
                    val user: User? = task.result.toObject(User::class.java)

                    if (user != null) {
                        Log.d(TAG, "getUserDetails: >>>" + user.email)
                        mUser = user
                        //mUserLocation?.user = mUser

                        UserClient.setUser(mUser)

                        getLastKnownLocation()

                        startLocationService()
                    }
                }
            }

    }

    private fun getLastKnownLocation(){//2
        Log.d(TAG, "getLastKnownLocation: called.")

        if (ActivityCompat.checkSelfPermission(
                this,
                ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            return
        }
        mFusedLocationProviderClient.lastLocation.addOnSuccessListener { location: Location ->
            val geoPoint = GeoPoint(location.latitude, location.longitude)
            Log.d(TAG, "\nFusedLocation latitude: " + geoPoint.latitude + "\nFusedLocation longitude: " + geoPoint.longitude)

            //mUserLocation?.geo_point = geoPoint
            mGeoPoint = geoPoint

            //Sanirim bu singleton oluyor.
            Log.d(TAG, "LOLOLOLOLOLOLOLOL: ${UserClient.getUser()?.name}")
            //Log.d(TAG, "getLastKnownLocation: " + mUserLocation?.geo_point!!.longitude.toString())
            saveUserLocation()

        }

    }

    private fun saveUserLocation(){//3
        Log.d(TAG, "saveUserLocation: called")
        // if (mUserLocation != null){
            Log.d(TAG, "saveUserLocation: saveUserLocation Calisti")
            val locationRef: DocumentReference = db.collection("user_locations").document(Sabit.auth.uid.toString())
            locationRef.set(UserLocation(geo_point = mGeoPoint, timestamp = null, user = mUser)).addOnCompleteListener { task ->
                if (task.isSuccessful){
                    //Log.d(TAG, "saveUserLocation: Inserted user location into database\nlatitude: " +
                    //mUserLocation!!.geo_point!!.latitude + "\nlongitude: " + mUserLocation!!.geo_point!!.longitude)
                }
            }



        //}
       /* else{
            Log.d(TAG, "saveUserLocation: mUserLocation is null")
        }*/
    }







    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_CODE)  {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "enableGPS -- Permission GRANTED 1", Toast.LENGTH_SHORT).show()
                enableGps("onRequestPermissionResult")

                //burada fused location
                //getLastKnownLocation()

            } else {
                Toast.makeText(this, "Permission DENIED 1", Toast.LENGTH_SHORT).show()
            }
        }
    }





    //Gps acma ekrani
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            LocationRequest.PRIORITY_HIGH_ACCURACY -> when (resultCode) {
                RESULT_OK ->                 // All required changes were successfully made
                {Log.d(TAG, "onActivityResult: GPS Enabled by user")


                    Handler(Looper.getMainLooper()).postDelayed({
                        // Your Code
                        //getLastKnownLocation()
                        getUserDetails()
                    }, 2000)



                //getLastKnownLocation()//bu calismiyor

                }
                RESULT_CANCELED ->                 // The user was asked to change settings, but chose not to
                    Log.d(TAG, "onActivityResult: User rejected GPS request")
                else -> {
                }
            }
        }

    }



    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }




}