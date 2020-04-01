package com.lbradford.firestorelistenerdeath

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity;
import android.view.Menu
import android.view.MenuItem
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QueryDocumentSnapshot

import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*

class MainActivity : AppCompatActivity() {

    var mFirstPress = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()
        FirebaseFirestore.setLoggingEnabled(true)

        auth.signInAnonymously()

        fab.setOnClickListener { view ->
            Snackbar.make(view, "Attempting starting foreground service", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
            val manager = this.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            var serviceStarted: Boolean = false
            for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
                if ("MainService" == service.service.className) {
                    if (service.foreground) {
                        serviceStarted = true
                    }
                }
            }
            if (!serviceStarted) {
                this.startService(Intent(this, MainService::class.java))
            }
        }

        button.setOnClickListener { view ->
            if (mFirstPress) {
                db.collection("readStream").document("helloworld")
                    .addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
                        if (firebaseFirestoreException != null) {
                            Log.v("MainActivity", "listen failed")
                        }

                        if (documentSnapshot!!.exists()) {

                            val string = documentSnapshot["hello"]
                            Log.v("MainActivity", "listen success: $string")
                        }
                    }
                mFirstPress = false
            }

            Snackbar.make(view, "Attempting firestore button write", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()


            val time = Timestamp.now()

            db.collection("writeStream").document().set(
                mapOf(
                    "time" to time,
                    "source" to "click"
                )
            ).addOnSuccessListener {
                Log.v("MainActivity", "button write success")
                Snackbar.make(view, "success firestore click write", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
                db.collection("writeStreamSuccess").document().set(
                    mapOf(
                        "time" to time,
                        "timetaken" to Timestamp.now(),
                        "source" to "click"
                    )
                )
            }.addOnFailureListener {
                Log.v("MainActivity", "button write failure")
                db.collection("writeStreamFailure").document().set(
                    mapOf(
                        "time" to time,
                        "timetaken" to Timestamp.now(),
                        "source" to "click"
                    )
                )
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }
}
