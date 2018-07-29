package com.example.ben.open_study

import com.squareup.moshi.FromJson
import com.squareup.moshi.Json

class Room constructor(name:String,availability:Boolean,notes:String){
    @Json(name="name")
    val name:String

    @Json(name="occupied")
    var availability:Boolean
    var notes:String
    init{
        this.name = name
        this.availability = availability
        this.notes = notes
    }

}