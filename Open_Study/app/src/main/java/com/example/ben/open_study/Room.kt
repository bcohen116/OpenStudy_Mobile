package com.example.ben.open_study

class Room constructor(name:String,availability:Boolean,notes:String){
    val name:String
    val availability:Boolean
    val notes:String
    init{
        this.name = name
        this.availability = availability
        this.notes = notes
    }
}