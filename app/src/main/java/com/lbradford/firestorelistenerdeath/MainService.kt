package com.lbradford.firestorelistenerdeath

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.util.concurrent.TimeUnit

class MainService : Service() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    //TODO: RX TIMER
    //TODO: Android TIMER

    // TIMER 1
    private val runnable: Runnable = object : Runnable {
        override fun run() {
            val time = Timestamp.now()

            handler.postDelayed(this, 20000)
            db.collection("writeStream").document().set(
                mapOf(
                    "time" to time,
                    "source" to "runnable"
                )
            ).addOnSuccessListener {
                db.collection("writeStreamSuccess").document().set(
                    mapOf(
                        "time" to time,
                        "timetaken" to Timestamp.now(),
                        "source" to "runnable"
                    )
                )
            }.addOnFailureListener {
                db.collection("writeStreamFailure").document().set(
                    mapOf(
                        "time" to time,
                        "timetaken" to Timestamp.now(),
                        "source" to "runnable"
                    )
                )
            }
        }
    }
    private val handler: Handler = Handler()

    // TIMER 2
    private lateinit var subscription: Disposable

    // TIMER 3
    // private lateinit var

    override fun onCreate() {
        super.onCreate()

        val notification = createForegroundNotification()
        startForeground(1, notification)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        FirebaseFirestore.setLoggingEnabled(true)

        auth.signInAnonymously()

        subscription = Observable.interval(10000, 10000, TimeUnit.MILLISECONDS)
            .subscribeOn(Schedulers.computation())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                val time = Timestamp.now()
                db.collection("writeStream").document().set(
                    mapOf(
                        "time" to time,
                        "source" to "rx"
                    )
                ).addOnSuccessListener {
                    db.collection("writeStreamSuccess").document().set(
                        mapOf(
                            "time" to time,
                            "timetaken" to Timestamp.now(),
                            "source" to "rx"
                        )
                    )
                }.addOnFailureListener {
                    db.collection("writeStreamFailure").document().set(
                        mapOf(
                            "time" to time,
                            "timetaken" to Timestamp.now(),
                            "source" to "rx"
                        )
                    )
                }
            }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // TIMER 1
        handler.postDelayed(runnable, 10000)

        // TIMER 2

        db.collection("readStream").document("8LrTS2ev2GfSKzRwVo8O")
            .addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
                if (firebaseFirestoreException != null) {
                    Log.v("MainService", "listen failed")
                }

                if (documentSnapshot!!.exists()) {

                    val string = documentSnapshot["hello"]
                    Log.v("MainService", "listen success: $string")
                }
            }

        return START_STICKY_COMPATIBILITY
    }

    override fun stopService(name: Intent?): Boolean {

        return super.stopService(name)
    }

    override fun onDestroy() {
        handler.removeCallbacks(runnable)
        subscription.dispose()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null;
    }

    override fun onUnbind(intent: Intent?): Boolean {
        return super.onUnbind(intent)
    }

    override fun onRebind(intent: Intent?) {
        super.onRebind(intent)
    }

    private fun createForegroundNotification(): Notification {
        val serviceChannel = NotificationChannel(
            "com.lbradford.firestoredeathlistenerchannel",
            "com.lbradford.firestoredeathlistenerchannel",
            NotificationManager.IMPORTANCE_DEFAULT
        )

        val manager = ContextCompat.getSystemService(this, NotificationManager::class.java)
        manager!!.createNotificationChannel(serviceChannel)

        return NotificationCompat.Builder(this, "com.lbradford.firestoredeathlistenerchannel")
            .setContentTitle("FirestoreDeathListener")
            .setContentText("w")
            .build()
    }
}