package com.example.ben.open_study

class Room constructor(name:String,availability:Boolean,notes:String){
    val name:String
    var availability:Boolean
    var notes:String
    init{
        this.name = name
        this.availability = availability
        this.notes = notes
    }
}