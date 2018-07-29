package com.example.ben.open_study

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.preference.PreferenceManager
import android.support.v4.app.NotificationCompat
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
        //Check database, if notifications are active in the settings page
        Thread{//Open a new thread so it doesn't block the rest of the app, and allows this service to do the start sticky
            while(PreferenceManager.getDefaultSharedPreferences(this).getBoolean("notifications_new_message", false)){
                try {
                    Log.d("NotificationService: ", "Current Setting: " + PreferenceManager.getDefaultSharedPreferences(this).getBoolean("notifications_new_message", false))
                    retrieveData(4)
                    retrieveData(5)
                    retrieveData(6)
                    Thread.sleep(30000)//wait 30 seconds to search again, to save on power and data
                } catch (e: Exception) {
                    Log.e("Notification-Service:",e.message)
                }
            }
        }.start()

        Log.i("Notification-Service:", "Service running")
        return Service.START_STICKY
    }

    //Check the database for room information and return a list of the info
    private fun retrieveData(floor:Int){
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
                    checkForDifferences(roomData,floor)
                },
                Response.ErrorListener { Log.d("Volley HTTP Error: ","That didn't work!" ); responseJson = ""})

        // Add the request to the RequestQueue.
        queue.add(stringRequest)
    }

    private fun checkForDifferences(data:ArrayList<Room>,floor:Int){
        val PREFS_FILENAME = "com.ben.openstudy.login.prefs"
        val prefs = this.getSharedPreferences(PREFS_FILENAME, 0)
        val editor = prefs!!.edit()
        var previousData:String = ""
        when (floor){
            4 -> {
                previousData = prefs.getString("lastCheckedData4","")
            }
            5 -> {
                previousData = prefs.getString("lastCheckedData5","")
            }
            6 -> {
                previousData = prefs.getString("lastCheckedData6","")
            }
        }

        //Moshi allows for easy json string manipulation
        val moshi = Moshi.Builder()
                // ... add your own JsonAdapters and factories ...
                .add(MoshiJsonListAdaptersFactory())
                .add(KotlinJsonAdapterFactory())
                .build()
        val roomType: Type = Types.newParameterizedType(List::class.java, Room::class.java)
        val roomAdapter: JsonAdapter<ArrayList<Room>> = moshi.adapter(roomType)
        //Convert the saved string into an object for comparing
        var previousCheckedData:ArrayList<Room> = arrayListOf()
        if(!previousData.equals(""))
            previousCheckedData = roomAdapter.fromJson(previousData)!!

        //Convert the data object to a string that can be saved in preferences
        val currentCheckedDataStr:String = roomAdapter.toJson(data)
        //Save data string to local storage to compare next time
        editor.putString("lastCheckedData$floor", currentCheckedDataStr)
        editor.apply()

        //Compare the two objects for changes
        for (x in 0..data.size - 1){
            if(data.get(x).availability != previousCheckedData.get(x).availability && data.get(x).availability && !previousData.equals("")){
                //data changed and room is now available, send user notification
                val notificationManager: NotificationManager = getSystemService(
                        Context.NOTIFICATION_SERVICE) as NotificationManager

                Log.d("NotificationService:","Room is now available")
                val mBuilder: NotificationCompat.Builder = NotificationCompat.Builder(this,"com.example.ben.open_study")
                mBuilder.setDefaults(Notification.DEFAULT_ALL)
                mBuilder.setSmallIcon(R.drawable.open_study_logo_108_red_background)
                mBuilder.setContentTitle("Room Available")
                mBuilder.setContentText("Room " + data.get(x).name + " was recently opened.")
                mBuilder.setChannelId("com.example.ben.open_study")
                mBuilder.setPriority(Notification.PRIORITY_MAX)

                val notificationIntent:Intent = Intent(this,MainActivity::class.java)
                notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                val pendingIntent:PendingIntent = PendingIntent.getActivity(this,0,notificationIntent,PendingIntent.FLAG_UPDATE_CURRENT)
                mBuilder.setContentIntent(pendingIntent)

                notificationManager.notify(1,mBuilder.build())
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
