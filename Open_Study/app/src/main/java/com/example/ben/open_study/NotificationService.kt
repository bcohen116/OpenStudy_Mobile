package com.example.ben.open_study

import android.app.IntentService
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.preference.PreferenceManager
import android.util.Log
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.lang.reflect.Type

//Service that runs in the background to check for database changes. Notifies the user when a room becomes available
class NotificationService : Service() {

    override fun onCreate() {
        Log.i("Notification-Service:", "Service onCreate")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        Log.i("Notification-Service:", "Service onStartCommand " + startId)
        var totalRoomData:ArrayList<Room> = arrayListOf()
        //Check database, if notifications are active in the settings page
        while(PreferenceManager.getDefaultSharedPreferences(this).getBoolean("notifications_new_message", false)){
            try {
                Log.d("NotificationService: ", "Current Setting: " + PreferenceManager.getDefaultSharedPreferences(this).getBoolean("notifications_new_message", false))
                val fourData = retrieveData(4)
                val fiveData = retrieveData(5)
                val sixData = retrieveData(6)
                totalRoomData.addAll(fourData)
                totalRoomData.addAll(fiveData)
                totalRoomData.addAll(sixData)

                checkForDifferences(totalRoomData)
                Thread.sleep(30000)//wait 30 seconds to search again, to save on power and data
            } catch (e: Exception) {
                Log.e("Notification-Service:",e.message)
            }
        }
        Log.i("Notification-Service:", "Service running")
        return Service.START_STICKY
    }

    //Check the database for room information and return a list of the info
    private fun retrieveData(floor:Int):ArrayList<Room>{
        // Instantiate the RequestQueue.
        val queue = Volley.newRequestQueue(this)
        val url = "http://www.openstudyuc.xyz/api/floor/" + floor
        lateinit var responseJson:String
        val moshi = Moshi.Builder()
                // ... add your own JsonAdapters and factories ...
                .add(MoshiJsonListAdaptersFactory())
                .add(KotlinJsonAdapterFactory())
                .build()
        val roomType: Type = Types.newParameterizedType(List::class.java, Room::class.java)
        val roomAdapter: JsonAdapter<ArrayList<Room>> = moshi.adapter(roomType)
        var roomData: ArrayList<Room> = arrayListOf()

        // Request a string response from the provided URL.
        val stringRequest = StringRequest(Request.Method.GET, url,
                Response.Listener<String> { response ->
                    // get the object result from the database
                    responseJson = response
                    roomData = roomAdapter.fromJson(responseJson)!!
                },
                Response.ErrorListener { Log.d("Volley HTTP Error: ","That didn't work!" ); responseJson = ""})

        // Add the request to the RequestQueue.
        queue.add(stringRequest)


        return roomData
    }

    private fun checkForDifferences(data:ArrayList<Room>){
        val PREFS_FILENAME = "com.ben.openstudy.login.prefs"
        val prefs = this.getSharedPreferences(PREFS_FILENAME, 0)
        val editor = prefs!!.edit()
        val previousData = prefs.getString("lastCheckedData","")


        //Moshi allows for easy json string manipulation
        val moshi = Moshi.Builder()
                // ... add your own JsonAdapters and factories ...
                .add(MoshiJsonListAdaptersFactory())
                .add(KotlinJsonAdapterFactory())
                .build()
        val roomType: Type = Types.newParameterizedType(List::class.java, Room::class.java)
        val roomAdapter: JsonAdapter<ArrayList<Room>> = moshi.adapter(roomType)
        //Convert the saved string into an object for comparing
        val previousCheckedData:ArrayList<Room> = roomAdapter.fromJson(previousData)!!

        //Convert the data object to a string that can be saved in preferences
        val currentCheckedDataStr:String = roomAdapter.toJson(data)
        //Save data string to local storage to compare next time
        editor.putString("lastCheckedData", currentCheckedDataStr)
        editor.apply()

        //Compare the two objects for changes
        for (x in 0..data.size){
            if(data.get(x).availability != previousCheckedData.get(x).availability && data.get(x).availability){
                //data changed and room is now available, send user notification
            }
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        Log.i("Notification-Service:", "Service onBind")
        return null
    }

    override fun onDestroy() {
        Log.i("Notification-Service:", "Service onDestroy")
    }
}
