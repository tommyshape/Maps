package com.example.haritalar2

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

object Sabit {

    const val TAG: String = "my_map"
    var auth: FirebaseAuth = Firebase.auth
    val db = Firebase.firestore

}