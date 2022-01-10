package com.example.haritalar2

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem

class ClusterMarker(
    lat: Double,
    lng: Double,
    title: String,
    snippet: String,
    var user: User
) : ClusterItem {

    private var position: LatLng = LatLng(lat, lng)
    private var title: String = title
    private var snippet: String = snippet
    //private val user: User = user

    override fun getPosition(): LatLng {
        return position
    }

    fun setPosition(position: LatLng){
        this.position = position
    }

    fun setTitle(title: String){
        this.title = title
    }

    fun setSnippet(snippet: String){
        this.snippet = snippet
    }

    override fun getTitle(): String {
        return title
    }

    override fun getSnippet(): String {
        return snippet
    }



}