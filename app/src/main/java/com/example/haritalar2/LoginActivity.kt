package com.example.haritalar2

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.example.haritalar2.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

//private const val TAG: String = "my_map"

class LoginActivity : AppCompatActivity() {
    //private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        //setContentView(R.layout.activity_login)

        binding.LoginBtn.setOnClickListener {
            val userCredentialList: ArrayList<String>? = getNEP()
            if (userCredentialList.isNullOrEmpty()){
                return@setOnClickListener
            }
            else{
                val email = userCredentialList[0]
                val password = userCredentialList[1]
                Sabit.auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                    if (task.isSuccessful){
                        Log.d(Sabit.TAG, "RegisterActivity: User signed in.")

                        startActivity(Intent(this, MainActivity::class.java))

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
        val currentUser: FirebaseUser? = Sabit.auth.currentUser
        updateUI(currentUser)

    }

    private fun updateUI(user: FirebaseUser?){
        if (user != null){
            Log.d(Sabit.TAG, "LoginActivity: user exists: " + user.uid)
            startActivity(Intent(applicationContext, MainActivity::class.java))
            finish()
        }
        else{
            Log.d(Sabit.TAG, "User is null")
        }
    }

    private fun getNEP(): ArrayList<String>?{

        val name = binding.loginEmail.text.toString()
        val password = binding.loginPassword.text.toString()

        val arrayList = arrayListOf(name, password)

        return if (name.isEmpty() && password.isEmpty()){
            binding.loginEmail.error = "Please fill all fields."
            binding.loginPassword.error = "!"
            null
        } else if(name.isEmpty() || password.isEmpty()){
            binding.loginEmail.error = "Please fill remaining empty fields."
            null
        } else{
            arrayList
        }
    }
}