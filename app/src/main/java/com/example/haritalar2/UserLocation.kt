package com.example.haritalar2

import android.os.Parcel
import android.os.Parcelable
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ServerTimestamp
import java.util.*

class UserLocation(var geo_point: GeoPoint? = null, @ServerTimestamp var  timestamp: Date? = null, var user: User? = null): Parcelable{
    //constructor() : this(null, null,null)
    constructor(parcel: Parcel) : this(

    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {

    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<UserLocation> {
        override fun createFromParcel(parcel: Parcel): UserLocation {
            return UserLocation(parcel)
        }

        override fun newArray(size: Int): Array<UserLocation?> {
            return arrayOfNulls(size)
        }
    }
}