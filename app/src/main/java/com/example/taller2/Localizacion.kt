package com.example.taller2

import org.json.JSONException
import org.json.JSONObject
import java.util.Date

class Localizacion(var latitud: Double, var longitud: Double, var fecha: String){
    fun toJSON(): JSONObject {
        val obj = JSONObject()
        try {
            obj.put("latitud", latitud)
            obj.put("longitud", longitud)
            obj.put("date", fecha)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return obj
    }
}
