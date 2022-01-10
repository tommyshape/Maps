package com.example.haritalar2

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.haritalar2.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var user_name_reference: DocumentReference
    val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)




        binding.LoginHereBtn.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        binding.RegisterBtn.setOnClickListener {
            val userCredentialList: ArrayList<String>? = getNEP()
            if (userCredentialList.isNullOrEmpty()){
                return@setOnClickListener
            }
            else{
                val name = userCredentialList[0]
                val email = userCredentialList[1]
                val password = userCredentialList[2]
                Sabit.auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                    if (task.isSuccessful){
                        Log.d(Sabit.TAG, "RegisterActivity: User created.")
                        val userID = Sabit.auth.currentUser?.uid.toString()
                        user_name_reference = db.collection("maps_users").document(userID)
                        /*val userHash = hashMapOf(
                            "name" to name,
                            "email" to email,
                            "password" to password,
                            "user_id" to userID
                        )*/
                        val user = User(name = name, email = email, password = password, user_id = userID)
                        user_name_reference.set(user).addOnSuccessListener {
                            Log.d(Sabit.TAG, "RegisterActivity: userHash saved to database.")
                            startActivity(Intent(this, MainActivity::class.java))
                        }.addOnFailureListener {
                            Log.d(Sabit.TAG, "RegisterActivity: userHash save failed.")
                        }
                    }
                    else{
                        Toast.makeText(this, "Error: " + task.exception, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

    }

    override fun onStart() {
        super.onStart()

        //UserClient.setUser(User("keko", "melo@gmail.com", "123456", "93r889r"))


        val currentUser: FirebaseUser? = Sabit.auth.currentUser
        updateUI(currentUser)

    }

    private fun updateUI(user: FirebaseUser?){
        if (user != null){
            Log.d(Sabit.TAG, "Register Activity: user exists: " + user.uid)
            startActivity(Intent(applicationContext, MainActivity::class.java))
            finish()
        }
        else{
            Log.d(Sabit.TAG, "Register Activity: User is null")
        }
    }

    private fun getNEP(): ArrayList<String>?{

        val name = binding.userName.text.toString()
        val email = binding.userEmail.text.toString()
        val password = binding.userPassword.text.toString()

        val arrayList = arrayListOf(name, email, password)

        return if (name.isEmpty() && email.isEmpty() && password.isEmpty()){
            binding.userName.error = "Please fill all fields."
            binding.userEmail.error = "!"
            binding.userPassword.error = "!"
            null
        } else if(name.isEmpty() || email.isEmpty() || password.isEmpty()){
            binding.userName.error = "Please fill remaining empty fields."
            null
        } else{
            arrayList
        }
    }


}