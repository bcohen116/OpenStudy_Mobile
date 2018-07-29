package com.example.ben.open_study

import android.Manifest
import android.app.ActionBar
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.location.*
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.LayoutInflater
import android.widget.*
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.location.*
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types.newParameterizedType
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.android.synthetic.main.room_details_popup.view.*
import java.lang.reflect.Type
import kotlin.collections.ArrayList


class MainActivity : AppCompatActivity(),RecyclerViewAdapter.ItemClickListener {

    lateinit var adapter:RecyclerViewAdapter
    lateinit var geofencingClient: GeofencingClient
    lateinit var geofences:ArrayList<Geofence>
    lateinit var floorPicker:RadioGroup
    lateinit var roomList:RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //Initialize variables
        val PREFS_FILENAME = "com.ben.openstudy.login.prefs"
        val prefs = this.getSharedPreferences(PREFS_FILENAME, 0)

        floorPicker = findViewById<RadioGroup>(R.id.floorPicker);
        roomList = findViewById(R.id.roomList);

        //Initialize the action bar at the top of the screen
        val toolBar:android.support.v7.widget.Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolBar)

        //The Following Block is for resetting the shared preferences, hid it inside the logo
        // (long press the logo to access the login screen on next app launch)
        val logo:ImageView = findViewById(R.id.logo)
        logo.setOnLongClickListener {
            val editor = prefs!!.edit()
            editor.putBoolean("previous_login", false)
            editor.apply()
            return@setOnLongClickListener true
        }

        checkLogin()//Check if the User has logged in before

        //Init the location services
        geofencingClient = LocationServices.getGeofencingClient(this)
        ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 100)
        geofences = createGeofence()


        //Create the Room List
        roomList.layoutManager = GridLayoutManager(this,2);//2 column list
        roomList.setHasFixedSize(true);
        //Change the room list when changing floors
        floorPicker.setOnCheckedChangeListener{ group, checkedId ->
            val selected: RadioButton = findViewById(checkedId)
            for (x in 0..(floorPicker.childCount - 1)){
                val radioBtn:View = floorPicker.getChildAt(x)
                if(radioBtn is RadioButton)
                    radioBtn.setCompoundDrawables(null,null,null,null)
                    continue
            }
            val selectionIcon: Drawable = this.getDrawable(android.R.drawable.button_onoff_indicator_on)
            selected.setCompoundDrawablesWithIntrinsicBounds(null,null,null,selectionIcon)
            checkCurrentFloor(selected)//finds room data from database and refreshes the room list
        }
        floorPicker.check(R.id.radioButton)//Default to the first Floor of the library

        //Start service to check for room data changes and send notifications
        val serviceIntent:Intent = Intent(this,NotificationService::class.java)
        startService(serviceIntent)


        //Settings hamburger button click listener
        val settingsBtn:ImageButton = findViewById(R.id.settingsBtn)
        settingsBtn.setOnClickListener {
            val settingIntent:Intent = Intent(this,SettingsActivity::class.java)
            startActivity(settingIntent)
        }
    }
    private fun refreshRoomList(roomData:ArrayList<Room>){
        //Populate the list
        adapter = RecyclerViewAdapter(this,roomData)
        adapter.setClickListener(this)
        roomList.adapter = adapter
    }

    private fun checkCurrentFloor(selected:RadioButton){
        if (selected.id == R.id.radioButton){
            //Floor 4 was selected... retrieve info from database
            retrieveData(4)
        }
        else if (selected.id == R.id.radioButton2){
            //Floor 5 was selected... retrieve info from database
            retrieveData(5)
        }
        else{
            //Floor 6 was selected.. retrieve info from database
            retrieveData(6)
        }
    }

    //Check if the User has logged in before, make them log in if they have not.
    private fun checkLogin(){
        val PREFS_FILENAME = "com.ben.openstudy.login.prefs"
        val prefs = this.getSharedPreferences(PREFS_FILENAME, 0)
        val loggedInBefore = prefs.getBoolean("previous_login",false)

        if (!loggedInBefore){
            val intent:Intent = Intent(this,LoginActivity::class.java)
            startActivity(intent)
        }
    }

    //Retrieve the room data from the database to populate the room list and popups
    private fun retrieveData(floor:Int){
        volleyHttpGet(floor)!!;
    }

    private fun volleyHttpGet(floor: Int){
        // Instantiate the RequestQueue.
        val queue = Volley.newRequestQueue(this)
        val url = "http://www.openstudyuc.xyz/api/floor/" + floor
        lateinit var responseJson:String
        //Initialize moshi variables which will allow for easy conversion to and from json
        val moshi = Moshi.Builder()
                // ... add your own JsonAdapters and factories ...
                .add(MoshiJsonListAdaptersFactory())
                .add(KotlinJsonAdapterFactory())
                .build()
        val roomType: Type = newParameterizedType(List::class.java,Room::class.java)
        val roomAdapter: JsonAdapter<ArrayList<Room>> = moshi.adapter(roomType)
        var roomData: ArrayList<Room> = arrayListOf()

        // Request a string response from the provided URL.
        val stringRequest = StringRequest(Request.Method.GET, url,
                Response.Listener<String> { response ->
                    // get the object result from the database
                    responseJson = response
                    roomData = roomAdapter.fromJson(responseJson)!!//convert the database get request into a usable Kotlin object
                    //Refresh the room list
                    refreshRoomList(roomData)
                },
                Response.ErrorListener { Log.d("Volley HTTP Error: ","That didn't work!" ); responseJson = ""})

        // Add the request to the RequestQueue.
        queue.add(stringRequest)
    }
    //change a room availability
    private fun volleyHttpPut(room:Room){
        // Instantiate the RequestQueue.
        val queue = Volley.newRequestQueue(this)
        val url = "http://www.openstudyuc.xyz/api/room/" + room.name + "/" + room.availability
        lateinit var responseJson:String

        // Request a string response from the provided URL.
        val stringRequest = StringRequest(Request.Method.GET, url,
                Response.Listener<String> { response ->
                    // get the object result from the database
                    responseJson = response
                },
                Response.ErrorListener { Log.d("Volley HTTP Error: ","That didn't work!" ); responseJson = ""})
        // Add the request to the RequestQueue.
        queue.add(stringRequest)
    }

    //Put info in the popup window after clicking on a room
    override fun onItemClick(view: View, position: Int) {
        Log.i("Info: ", adapter.getItem(position).name + " was clicked...")


        //Load in popup window
        val inflater = this.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupView:View = inflater.inflate(R.layout.room_details_popup,null)
        val popupWindow = PopupWindow(
                popupView,
                ActionBar.LayoutParams.WRAP_CONTENT,
                ActionBar.LayoutParams.WRAP_CONTENT
        )

        //Add in data from database to popup
        val itemName:String = adapter.getItem(position).name//Room Name from database
        val itemAvailable:Boolean = adapter.getItem(position).availability//Availablility from database
//        val itemNotes:String = adapter.getItem(position).notes//Room Notes from database
        val roomName:String = popupView.name.text.toString()
        val availablility:String = popupView.availablility.text.toString()
//        val notes:String = popupView.notes.text.toString()//notes was not implemented in the database
        popupView.name.text = "%s %s".format(roomName, itemName)
        popupView.availablility.text = "%s %s".format(availablility, itemAvailable)
//        popupView.notes.text = "%s %s".format(notes, itemNotes)//notes was not implemented in the database
        if(itemAvailable)
            popupView.switch1.text = "Take Room"
        else
            popupView.switch1.text = "I'm done with Room"

        //Set popup properties
        popupWindow.elevation = 5.0f
        val closeButton: ImageButton = popupView.findViewById(R.id.ib_close)
        closeButton.setOnClickListener { v ->
            popupWindow.dismiss()
            val roomData:ArrayList<Room>;
            val selected:RadioButton = findViewById(floorPicker.checkedRadioButtonId)
            checkCurrentFloor(selected)//retrieves the room data from the database and refreshes the list
        }

        //When the take room switch is flipped, this will be triggered
        val roomSwitch:Switch = popupView.findViewById(R.id.switch1)
        roomSwitch.setOnClickListener {
            if(checkLocation()){
                modifyRoom(adapter.getItem(position),popupView)
            }
        }

        popupWindow.showAtLocation(findViewById(R.id.roomListLayout),Gravity.CENTER,0,0)
    }

    private fun checkLocation() : Boolean{
        //Check if the user allowed location permission and has it turned on
        if (ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            if(checkLocationSetting()){
                //turn on location tracking to see if the user is close to the library in order to allow them to make changes to a room
                val result: com.google.android.gms.tasks.Task<Void>? = geofencingClient?.addGeofences(getGeofencingRequest(), geofencePendingIntent)?.run {
                    addOnSuccessListener {
                        // Geofences added
                        // ...
                        Log.d("GeofenceClient: ", "Geofence added...")

                    }
                    addOnFailureListener {
                        // Failed to add geofences
                        // ...
                        Log.e("GeofenceClient: ", "Geofence FAILED to add...")
                    }
                }
                while(result?.isComplete != true){
                    //This is bad practice, but I need to wait for the geofencing to finish in order for the following to run correctly
                }
                if (result?.isSuccessful == true){
                    Log.d("GeofenceClient: ", "Location Found.....")
                    return true
                }
                else{
                    Log.e("GeofenceClient: ", "Location Lost...")
                    return false
                }
            }
            else{
                buildAlertMessageNoGps()
            }
        }
        else{
            ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 100)
        }
        //Close the location tracking to save battery, in this case, something failed so it should stop searching
        geofencingClient?.removeGeofences(geofencePendingIntent)?.run {
            addOnSuccessListener {
                // Geofences removed
                // ...
                Log.d("GeofenceClient: ", "Geofence Closed...")
            }
            addOnFailureListener {
                // Failed to remove geofences
                // ...
                Log.e("GeofenceClient: ", "Geofence FAILED to close...")
            }
        }
        return false
    }
    //Check if the location setting on the phone is turned on
    private fun checkLocationSetting(): Boolean{
        lateinit var locationManager: LocationManager
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }
    //change the availability of a room
    private fun modifyRoom(room:Room,popupView:View){
        room.availability = !room.availability
        val availTxt:TextView = popupView.findViewById(R.id.availablility)
        availTxt.text = "Available: " + room.availability
        volleyHttpPut(room)
        if(room.availability)
            popupView.switch1.text = "Take Room"
        else
            popupView.switch1.text = "I'm done with Room"
        //Close the location tracking to save battery
        geofencingClient?.removeGeofences(geofencePendingIntent)?.run {
            addOnSuccessListener {
                // Geofences removed
                // ...
                Log.d("GeofenceClient: ", "Geofence Closed...")
            }
            addOnFailureListener {
                // Failed to remove geofences
                // ...
                Log.e("GeofenceClient: ", "Geofence FAILED to close...")
            }
        }
    }
    //create the marker for the library location
    private fun createGeofence():ArrayList<Geofence>{
        var geofenceList:ArrayList<Geofence> = arrayListOf()
        geofenceList.add(Geofence.Builder()
                // Set the request ID of the geofence. This is a string to identify this
                // geofence.
                .setRequestId("Langsam")

                // Set the circular region of this geofence. (latitude, longitude, radius in meters)
                .setCircularRegion(39.135266,-84.515419,1000f)

                // Set the expiration duration of the geofence. This geofence gets automatically
                // removed after this period of time.
                .setExpirationDuration(Geofence.NEVER_EXPIRE)

                // Set the transition types of interest. Alerts are only generated for these
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_DWELL)
                .setLoiteringDelay(10000)//in milliseconds

                // Create the geofence.
                .build())
        return geofenceList
    }
    //one of multiple methods used to retreive user location
    private fun getGeofencingRequest(): GeofencingRequest {
        return GeofencingRequest.Builder().apply {
            setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            addGeofences(geofences)
        }.build()
    }
    //one of multiple methods used to retreive user location
    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(this, GeofenceTransitionsIntentService::class.java)
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
        // addGeofences() and removeGeofences().
        PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    //Takes the user to the settings page if they did not have location turned on
    private fun buildAlertMessageNoGps() {
        val  builder:AlertDialog.Builder = AlertDialog.Builder(this);
        builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes") { dialog, button ->
                    startActivity(Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }
                .setNegativeButton("No") { dialog, button ->
                    checkLocation()
                    dialog.cancel()
                }
        val alert:AlertDialog = builder.create()
        alert.show();
    }
    //used for location notifications
    companion object {
        fun makeNotificationIntent(geofenceService: Context): Intent {
            return Intent(geofenceService, MainActivity::class.java)
        }
    }

}
