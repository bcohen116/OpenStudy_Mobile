package com.example.ben.open_study

import com.squareup.moshi.*
import java.lang.reflect.Type

class MoshiJsonListAdaptersFactory : JsonAdapter.Factory {
    override fun create(type: Type, annotations: MutableSet<out Annotation>, moshi: Moshi): JsonAdapter<*>? {
        return when (type) {
            Types.newParameterizedType(ArrayList::class.java, Room::class.java) ->
                moshi.adapter<ArrayList<Room>>(type)
            else -> null
        }
    }

//    @FromJson fun fromJson(jsonReader: JsonReader, room:String):Room{
//        return
//    }
}