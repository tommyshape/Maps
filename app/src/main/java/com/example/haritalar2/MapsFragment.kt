package com.example.haritalar2


import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.ActivityNavigator
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.firebase.firestore.DocumentReference
import com.google.maps.GeoApiContext
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.model.DirectionsResult

import com.google.maps.DirectionsApiRequest
import com.google.maps.PendingResult
import com.google.maps.android.clustering.ClusterItem
import com.google.maps.internal.PolylineEncoding
import java.lang.RuntimeException
import javax.security.auth.callback.Callback


class MapsFragment : Fragment() {
    //private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val TAG: String = "my_map"
    private val location_update_interval: Long = 9000

    lateinit var myLatLng: LatLng
    private lateinit var mGoogleMap: GoogleMap

    // Declare a variable for the cluster manager.
    private lateinit var clusterManager: ClusterManager<ClusterMarker>
    private lateinit var userLocationList: ArrayList<UserLocation>
    private lateinit var mClusterMarkers: ArrayList<ClusterMarker>
    private lateinit var mHandler: Handler
    private lateinit var mRunnable: Runnable
    private var mGeoApiContext: GeoApiContext? = null
    var mDirectionsResult: DirectionsResult? = null
    var polyLineCalled = false
    var polylinetodelete: Polyline? = null




    @SuppressLint("MissingPermission", "PotentialBehaviorOverride")
    private val callback = OnMapReadyCallback { googleMap ->
        /**
         * Manipulates the map once available.
         * This callback is triggered when the map is ready to be used.
         * This is where we can add markers or lines, add listeners or move the camera.
         * In this case, we just add a marker near Sydney, Australia.
         * If Google Play services is not installed on the device, the user will be prompted to
         * install it inside the SupportMapFragment. This method will only be triggered once the
         * user has installed Google Play services and returned to the app.
         */


        mGoogleMap = googleMap

        userLocationList = ArrayList()
        mClusterMarkers = ArrayList()


        if (mGeoApiContext == null){
            mGeoApiContext = GeoApiContext.Builder()
                .apiKey(getString(R.string.google_maps_key))
                .build()
        }

        mHandler = Handler(Looper.getMainLooper())//bundan emin degilim
        myLatLng = LatLng(41.004134, 39.713800)

        val trabzon = LatLng(41.004134, 39.713800)
        googleMap.addMarker(MarkerOptions().position(trabzon).title("Melih").snippet("melih@g.com")/*.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_map))*/)

        if (UserClient.getList().isNotEmpty()){
            Log.d(TAG, "mUserLocationList is not empty.")
            Log.d(TAG, UserClient.getList().first().user?.email!!)
            UserClient.getList().forEach {
                userLocationList.add(it)
                if (it.user!!.user_id == Sabit.auth.uid){
                    val latitude = it.geo_point!!.latitude
                    val longtitude = it.geo_point!!.longitude
                    myLatLng = LatLng(latitude, longtitude)
                }
                Log.d(TAG, "Benim adresim: " + it.user!!.name + it.geo_point)
            }

        }else{
            Log.d(TAG, "mUserLocationList is empty.")
        }

        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myLatLng, 10F))
        if (ActivityCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return@OnMapReadyCallback
        }
        googleMap.isMyLocationEnabled = true

        setUpClusterer()
    }





    //kodumun dunyasi

    private fun setUpClusterer() {
        // Position the map.
        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myLatLng/*LatLng(41.004134, 39.713800)*/, 10f))

        // Initialize the manager with the context and the map.
        // (Activity extends context, so we can pass 'this' in the constructor.)
        clusterManager = ClusterManager(context, mGoogleMap)

        // Point the map's listeners at the listeners implemented by the cluster
        // manager.
        mGoogleMap.setOnInfoWindowClickListener(clusterManager)

        //mGoogleMap.setOnCameraIdleListener(clusterManager)
        //mGoogleMap.setOnMarkerClickListener(clusterManager)
        clusterManager.setOnClusterItemClickListener(object : ClusterManager.OnClusterItemClickListener<ClusterMarker>{
            override fun onClusterItemClick(item: ClusterMarker?): Boolean {
                Toast.makeText(requireActivity(), "Item Click", Toast.LENGTH_SHORT).show()
                return false
            }

        })

        clusterManager.setOnClusterItemInfoWindowClickListener(object : ClusterManager.OnClusterItemInfoWindowClickListener<ClusterMarker>{
            override fun onClusterItemInfoWindowClick(item: ClusterMarker?) {
                Toast.makeText(requireActivity(), "Window Click" + item!!.title, Toast.LENGTH_SHORT).show()
                windowClick(item)
            }

        })

        //  mGoogleMap.setOnInfoWindowClickListener(this)

        // Add cluster items (markers) to the cluster manager.
        addItems()
    }

    private fun addItems() {

       /* // Set some lat/lng coordinates to start with.
        var lat = 51.5145160
        var lng = -0.1270060

        // Add ten cluster items in close proximity, for purposes of this example.
        for (i in 0..9) {
            val offset = i / 60.0
            lat += offset
            lng += offset
            val offsetItem =
                ClusterMarker(lat, lng, "Title $i", "Snippet $i")

            clusterManager.addItem(offsetItem)
        }*/

        userLocationList.forEach {

            if (it.user!!.user_id != Sabit.auth.uid){

                val lat = it.geo_point!!.latitude
                val lng = it.geo_point!!.longitude
                val title = it.user!!.name
                val snippet = it.user!!.email
                val user: User = it.user as User
                val offsetItem = ClusterMarker(lat, lng, title, snippet, user)

                clusterManager.addItem(offsetItem)
                //clusterManager.clearItems()
                mClusterMarkers.add(offsetItem)
                clusterManager.cluster()

            }
        }
    }


    //kodumun dunyasi bitis


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        //mUserLocations2 = arguments?.getParcelableArrayList<UserLocation>("keko") as ArrayList<UserLocation>

        val button = requireActivity().findViewById<Button>(R.id.showInfoButton)
        button.setOnClickListener {
            showInfoClick()
        }
        return inflater.inflate(R.layout.fragment_maps, container, false)
    }

    private fun showInfoClick(){

        if (mDirectionsResult == null){
            Toast.makeText(requireContext(), "please select destination", Toast.LENGTH_SHORT).show()
            return
        }else{
            val builder = AlertDialog.Builder(requireContext())

            builder.setPositiveButton("Yes"){_,_ ->
                //Code here


            }
            builder.setNegativeButton("No"){_,_ -> }
            builder.setTitle("Information")
            builder.setIcon(R.drawable.ic_info)
            /*Log.d(TAG, "onResult: routes: " + result.routes[0].toString())
            Log.d(TAG, "onResult: routes: " + result.routes[0].legs[0].duration)
            Log.d(TAG, "onResult: routes: " + result.routes[0].legs[0].distance)
            Log.d(TAG, "onResult: routes: " + result.geocodedWaypoints[0].toString())
            Log.d(TAG, "onResult: end address " + result.routes[0].legs[0].endAddress)*/

            var geowaypoint = ""

            mDirectionsResult!!.geocodedWaypoints[0].types.forEach {
                geowaypoint += "$it, "
            }

            builder.setMessage("Duration: " + mDirectionsResult!!.routes[0].legs[0].duration + "\n" +
                    "Distance: " + mDirectionsResult!!.routes[0].legs[0].distance + "\n" +
                    "Address: " + mDirectionsResult!!.routes[0].legs[0].endAddress + "\n" + "Geocode: " +
                    geowaypoint)
            builder.create().show()
        }


    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(callback)
    }

    override fun onResume() {
        super.onResume()
        startUserLocationsRunnable()
    }

    private fun startUserLocationsRunnable(){
        Log.d(TAG, "startUserLocationsRunnable: Starting user locations runnable")
        /*mHandler.postDelayed({
            retrieveUserLocations()
            mHandler.postDelayed(mRunnable, location_update_interval)
        }, location_update_interval)*/
        val handler = Handler()
        handler.postDelayed(object : Runnable {
            override fun run() {
                // TODO - Here is my logic
                retrieveUserLocations()
                // Repeat again after 8 seconds
                handler.postDelayed(this, location_update_interval)
            }
        }, location_update_interval)


    }



    private fun retrieveUserLocations(){
        Log.d(TAG, "Deneme amaclidir")

        try {

            mClusterMarkers.forEach { clusterMark ->

                val userLocationRef: DocumentReference = Sabit.db.collection("user_locations").document(clusterMark.user.user_id)

                userLocationRef.get().addOnCompleteListener{ mAddress ->
                    if (mAddress.isSuccessful){
                        //bir kullanicinin guncellenmis verisi
                        val updatedUserLocation = mAddress.result.toObject(UserLocation::class.java)

                        //guncellenmis veriyi kullanarak haritadaki marker yerini guncelleme
                        mClusterMarkers.forEach {
                            if (it.user.user_id == updatedUserLocation!!.user!!.user_id){
                                val updatedLatLng = LatLng(updatedUserLocation.geo_point!!.latitude, updatedUserLocation.geo_point!!.longitude)
                                //clusterManager.removeItem(it)
                                it.position = updatedLatLng
                                Log.d(TAG, "retrieveUserLocations: adres yenilendi mi")
                                clusterManager.updateItem(it)
                                clusterManager.cluster()


                            }
                        }

                    }
                }

            }
        }catch (e: Exception){

        }


    }

    private fun windowClick(item: ClusterMarker?){
        val builder = AlertDialog.Builder(requireContext())

        builder.setPositiveButton("Yes"){_,_ ->
            //Code here
            calculateDirections(item!!)
            Toast.makeText(requireContext(),
                "Yes",
                Toast.LENGTH_SHORT).show()
        }
        builder.setNegativeButton("No"){_,_ -> }
        builder.setTitle("Routes")
        builder.setIcon(R.drawable.ic_map)
        builder.setMessage("Go to " + item!!.title + "?")
        builder.create().show()
    }

    private fun calculateDirections(marker: ClusterMarker) {
        Log.d(TAG, "calculateDirections: calculating directions.")
        val destination = com.google.maps.model.LatLng(
            marker.position.latitude,
            marker.position.longitude
        )
        val directions = DirectionsApiRequest(mGeoApiContext)
        directions.alternatives(false)
        directions.origin(
            com.google.maps.model.LatLng(
                myLatLng.latitude,
                myLatLng.longitude
            )

        )
        Log.d(TAG, "calculateDirections: destination: $destination")

        directions.destination(destination).setCallback(object :
            PendingResult.Callback<DirectionsResult> {
            override fun onResult(result: DirectionsResult?) {
                if (result != null) {
                    Log.d(TAG, "onResult: routes: " + result.routes[0].toString())
                    Log.d(TAG, "onResult: routes: " + result.routes[0].legs[0].duration)
                    Log.d(TAG, "onResult: routes: " + result.routes[0].legs[0].distance)
                    Log.d(TAG, "onResult: routes: " + result.geocodedWaypoints[0].toString())
                    Log.d(TAG, "onResult: end address " + result.routes[0].legs[0].endAddress)

                    mDirectionsResult = result

                    addPolylinesToMap(result)

                }

            }

            override fun onFailure(e: Throwable?) {
                Log.e(TAG, "onFailure: " + e!!.message)
            }

        })

        /*directions.destination(destination).setCallback(object : Callback<DirectionsResult?>() {
            fun onResult(result: DirectionsResult) {
                Log.d(TAG, "onResult: routes: " + result.routes[0].toString())
                Log.d(TAG, "onResult: geocodedWayPoints: " + result.geocodedWaypoints[0].toString())
            }

            fun onFailure(e: Throwable) {
                Log.e(TAG, "onFailure: " + e.message)
            }
        })*/
    }

    private fun addPolylinesToMap(result: DirectionsResult) {

        Handler(Looper.getMainLooper()).post {//main thread
            Log.d(TAG, "run: result routes: " + result.routes.size)
            for (route in result.routes) {
                Log.d(TAG, "run: leg: " + route.legs[0].toString())
                val decodedPath = PolylineEncoding.decode(route.overviewPolyline.encodedPath)
                val newDecodedPath: MutableList<LatLng> =
                    ArrayList()

                // This loops through all the LatLng coordinates of ONE polyline.
                for (latLng in decodedPath) {

                //Log.d(TAG, "run: latlng: " + latLng.toString());
                    newDecodedPath.add(
                        LatLng(
                            latLng.lat,
                            latLng.lng
                        )
                    )
                }

                if (polylinetodelete != null){
                    polylinetodelete!!.remove()
                }

                //yeni polyline
                val polyline = mGoogleMap.addPolyline(PolylineOptions().addAll(newDecodedPath))

                polylinetodelete = polyline

                polyline.color = ContextCompat.getColor(requireActivity(), android.R.color.holo_blue_bright)
                polyline.isClickable = true
            }
        }
    }




}