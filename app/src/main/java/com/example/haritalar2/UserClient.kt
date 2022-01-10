package com.example.haritalar2



object UserClient {

    private var user: User? = null

    var arrayList: ArrayList<UserLocation> = ArrayList()

    fun getList(): ArrayList<UserLocation> {
        return arrayList
    }
    fun setList(arrayList: ArrayList<UserLocation>){
        this.arrayList = arrayList
    }

    fun getUser(): User? {
        return user
    }
    fun setUser(user: User?){
        this.user = user
    }

}