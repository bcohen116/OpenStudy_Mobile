package com.example.ben.open_study

import android.app.ActionBar
import android.content.Context
import android.graphics.drawable.Drawable
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.Gravity
import android.view.View
import java.util.*
import android.view.LayoutInflater
import android.widget.*
import kotlinx.android.synthetic.main.room_details_popup.view.*


class MainActivity : AppCompatActivity(),RecyclerViewAdapter.ItemClickListener {

    lateinit var adapter:RecyclerViewAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val floorPicker = findViewById<RadioGroup>(R.id.floorPicker);
        val roomList:RecyclerView = findViewById(R.id.roomList);

        roomList.layoutManager = GridLayoutManager(this,2);//2 column list
        roomList.setHasFixedSize(true);
        floorPicker.setOnCheckedChangeListener{ group, checkedId ->
            var roomData:ArrayList<Room>;
            val selected: RadioButton = findViewById(checkedId)
            for (x in 0..(floorPicker.childCount - 1)){
                val radioBtn:View = floorPicker.getChildAt(x)
                if(radioBtn is RadioButton)
                    radioBtn.setCompoundDrawables(null,null,null,null)
                    continue
            }
            val selectionIcon: Drawable = this.getDrawable(android.R.drawable.button_onoff_indicator_on)
            selected.setCompoundDrawablesWithIntrinsicBounds(null,null,null,selectionIcon)
            if (selected.id == R.id.radioButton){
                //Floor 4 was selected... retrieve info from database
                roomData = retrieveData(4)
            }
            else if (selected.id == R.id.radioButton2){
                //Floor 5 was selected... retrieve info from database
                roomData = retrieveData(5)
            }
            else{
                //Floor 6 was selected.. retrieve info from database
                roomData = retrieveData(6)
            }
            //Populate the list
            adapter = RecyclerViewAdapter(this,roomData)
            adapter.setClickListener(this)
            roomList.adapter = adapter
        }
    }

    private fun retrieveData(floor:Int): ArrayList<Room> {
        var data:ArrayList<Room> = ArrayList();
        //put database info request here

        //test Data
        data.add(Room("5467",false,"This is test data for room #1."))
        data.add(Room("5738",false,"This is test data for room #2."))
        data.add(Room("6473",true,"This is test data for room #3."))
        data.add(Room("6782",false,"This is test data for room #4."))
        return data;
    }

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
        val itemNotes:String = adapter.getItem(position).notes//Room Notes from database
        val roomName:String = popupView.name.text.toString()
        val availablility:String = popupView.availablility.text.toString()
        val notes:String = popupView.notes.text.toString()
        popupView.name.text = "%s %s".format(roomName, itemName)
        popupView.availablility.text = "%s %s".format(availablility, itemAvailable)
        popupView.notes.text = "%s %s".format(notes, itemNotes)
        if(itemAvailable)
            popupView.switch1.text = "Take Room"
        else
            popupView.switch1.text = "I'm done with Room"

        //Set popup properties
        popupWindow.elevation = 5.0f
        val closeButton: ImageButton = popupView.findViewById(R.id.ib_close)
        closeButton.setOnClickListener { v ->
            popupWindow.dismiss()
        }
        popupWindow.showAtLocation(findViewById(R.id.roomListLayout),Gravity.CENTER,0,0)
    }
}
