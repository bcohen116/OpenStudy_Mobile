package com.example.ben.open_study

import com.squareup.moshi.FromJson
import com.squareup.moshi.Json

class Room constructor(name:String,occupied:Boolean){
    @Json(name="number")
    val name:String

    @Json(name="occupied")
    var occupied:Boolean
//    var notes:String//notes was not implemented in the database
    init{
        this.name = name
        this.occupied = occupied
//        this.notes = notes//notes was not implemented in the database
    }

}