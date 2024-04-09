package com.example.taller2

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.database.Cursor
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.ContactsContract
import android.widget.ListView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class ContactosActivity : AppCompatActivity() {
    @SuppressLint("SetTextI18n")
    var mProjection: Array<String>? = null
    var mCursor: Cursor? = null
    var mContactsAdapter: ContactsAdapter? = null
    var mlista: ListView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contactos)

        // 1. Variables
        mlista = findViewById(R.id.listaContactos)
        // 2. Proyecciòn
        mProjection = arrayOf(ContactsContract.Profile._ID, ContactsContract.Profile.DISPLAY_NAME_PRIMARY)

        // 3. Adaptador
        mContactsAdapter = ContactsAdapter(this,null,0)
        mlista?.adapter = mContactsAdapter

        // 4. Pedir permiso (en una funciòn)
        when {
            ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED -> {
                // You can use the API that requires the permission.
                //val textV = findViewById<TextView>(R.id.textView3)
                //textV.text = "PERMISO CONCEDIDO"
                //textV.setTextColor(Color.GREEN)
                // 5. Cargar los contactos
                initView()
                Toast.makeText(this, "Gracias!", Toast.LENGTH_SHORT).show()
            }
            ActivityCompat.shouldShowRequestPermissionRationale(
                this, android.Manifest.permission.READ_CONTACTS) -> {
                requestPermissions(
                    arrayOf(android.Manifest.permission.READ_CONTACTS),
                    Datos.MY_PERMISSION_REQUEST_READ_CONTACTS)
            }
            else -> {
                // You can directly ask for the permission.
                requestPermissions(
                    arrayOf(android.Manifest.permission.READ_CONTACTS),
                    Datos.MY_PERMISSION_REQUEST_READ_CONTACTS)
            }
        }

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        //val textV = findViewById<TextView>(R.id.textView3)
        when (requestCode) {
            Datos.MY_PERMISSION_REQUEST_READ_CONTACTS -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // Permission is granted. Continue the action or workflow
                    // in your app.
                    //textV.text = "PERMISO CONCEDIDO"
                    //textV.setTextColor(Color.GREEN)
                    // 5. Cargar los contactos
                    initView()
                    Toast.makeText(this, "Gracias!", Toast.LENGTH_SHORT).show()
                } else {
                    // Explain to the user that the feature is unavailable
                    //textV.text = "PERMISO DENEGADO"
                    //textV.setTextColor(Color.RED)
                    Toast.makeText(this, "Funcionalidades limitadas!", Toast.LENGTH_SHORT).show()
                }
                return
            }
            else -> {
                // Ignore all other requests.
            }
        }
    }

    fun initView() {
        mCursor = contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI, mProjection, null, null, null
        )
        mContactsAdapter?.changeCursor(mCursor)
    }
}