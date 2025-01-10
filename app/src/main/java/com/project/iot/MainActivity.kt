package com.project.iot

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.database.*
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {

    // Menjalankan coroutine pada thread utama
    private val mainScope = CoroutineScope(Dispatchers.Main)

    // Mendeklarasikan Views untuk parkiran
    private lateinit var vBoxLeft: View
    private lateinit var vBoxRight: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Mendapatkan reference ke Views
        vBoxLeft = findViewById(R.id.v_box_left)
        vBoxRight = findViewById(R.id.v_box_right)

        // Tambahkan referensi ke Firebase Realtime Database
        val databaseReference: DatabaseReference = FirebaseDatabase.getInstance().getReference("sensorData")

        // Memulai coroutine untuk membaca data Firebase di background
        mainScope.launch {
            readDataFromFirebase(databaseReference)
        }
    }

    // Fungsi untuk membaca data dari Firebase
    private suspend fun readDataFromFirebase(databaseReference: DatabaseReference) {
        // Menggunakan coroutine untuk melakukan operasi Firebase di background
        withContext(Dispatchers.IO) {
            databaseReference.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // Mendapatkan nilai dari sensorData
                    var parkir1Count = 0
                    var parkir2Count = 0

                    // Iterasi semua anak dari sensorData
                    for (childSnapshot in snapshot.children) {
                        val key = childSnapshot.key // Mendapatkan nama node (A1_1, A1_2, dll.)
                        val value = childSnapshot.getValue(Int::class.java) // Mendapatkan nilai node (1 atau 0)

                        // Hitung node dengan nilai 0 untuk parkir 1
                        if (key in listOf("A1_1", "A1_2", "A1_3") && value == 0) {
                            parkir1Count++
                        }

                        // Hitung node dengan nilai 0 untuk parkir 2
                        if (key in listOf("A2_1", "A2_2", "A2_3") && value == 0) {
                            parkir2Count++
                        }

                        // Log hasil pembacaan
                        Log.d("Firebase", "Key: $key, Value: $value")
                    }

                    // Tentukan apakah parkir 1 terisi (semua node harus bernilai 0)
                    val parkir1Terisi = parkir1Count == 3

                    // Tentukan apakah parkir 2 terisi (semua node harus bernilai 0)
                    val parkir2Terisi = parkir2Count == 3

                    // Menentukan warna background parkir dan menampilkan ke UI
                    runOnUiThread {
                        if (parkir1Terisi) {
                            vBoxLeft.setBackgroundColor(resources.getColor(android.R.color.holo_red_light))
                        } else {
                            vBoxLeft.setBackgroundColor(resources.getColor(android.R.color.holo_green_light))
                        }

                        if (parkir2Terisi) {
                            vBoxRight.setBackgroundColor(resources.getColor(android.R.color.holo_red_light))
                        } else {
                            vBoxRight.setBackgroundColor(resources.getColor(android.R.color.holo_green_light))
                        }
                    }

                    // Menampilkan Toast di thread utama setelah data dibaca
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Data berhasil dibaca", Toast.LENGTH_SHORT).show()
                    }
                }


                override fun onCancelled(error: DatabaseError) {
                    // Handle jika ada error
                    Log.e("Firebase", "Gagal membaca data", error.toException())

                    // Menampilkan Toast di thread utama jika terjadi kesalahan
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Gagal membaca data", Toast.LENGTH_SHORT).show()
                    }
                }
            })
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Membatalkan semua coroutine saat activity dihancurkan
        mainScope.cancel()
    }
}
