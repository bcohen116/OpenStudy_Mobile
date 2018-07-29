package com.example.ben.open_study

import com.squareup.moshi.FromJson
import com.squareup.moshi.Json

class Room constructor(name:String,availability:Boolean){
    @Json(name="number")
    val name:String

    @Json(name="occupied")
    var availability:Boolean
//    var notes:String//notes was not implemented in the database
    init{
        this.name = name
        this.availability = availability
//        this.notes = notes//notes was not implemented in the database
    }

}