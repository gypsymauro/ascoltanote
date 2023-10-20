package org.glopestopper.ascoltanote

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.core.app.ActivityCompat
import kotlin.math.log2
import kotlin.math.roundToInt

import org.jtransforms.fft.FloatFFT_1D
import kotlin.math.absoluteValue


class MainActivity : AppCompatActivity() {

        private val REQUEST_RECORD_AUDIO_PERMISSION = 200
        private var permissionToRecordAccepted = false
        private var audioRecord: AudioRecord? = null
        private var isRecording = false
        private lateinit var noteTextView: TextView
        private lateinit var startButton: Button
        private lateinit var stopButton: Button

        @SuppressLint("MissingInflatedId")
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_main)

            noteTextView = findViewById(R.id.noteTextView)
            startButton = findViewById(R.id.startButton)
            stopButton = findViewById(R.id.stopButton)

            startButton.setOnClickListener {
                startRecording()
            }

            stopButton.setOnClickListener {
                stopRecording()
            }

            // Richiedi i permessi per registrare audio
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                REQUEST_RECORD_AUDIO_PERMISSION
            )
        }

    private fun findFundamentalFrequency(buffer: ShortArray): Double {
        val audioData = buffer.map { it.toFloat() }.toFloatArray()
        val fft = FloatFFT_1D(audioData.size.toLong())
        fft.realForward(audioData)

        val amplitudes = audioData.map { it.absoluteValue }
        val peakIndex = amplitudes.indices.maxByOrNull { amplitudes[it] } ?: 0
        val frequency = peakIndex.toDouble() * SAMPLE_RATE.toDouble() / audioData.size.toDouble()
        return frequency
    }


        private fun startRecording() {
            if (permissionToRecordAccepted) {
                val bufferSize = AudioRecord.getMinBufferSize(
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.RECORD_AUDIO
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return
                }
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize
                )

                audioRecord?.startRecording()
                isRecording = true

                Thread {
                    val buffer = ShortArray(bufferSize)
                    while (isRecording) {
                        audioRecord?.read(buffer, 0, bufferSize)
                        val frequency = findFundamentalFrequency(buffer)
                        val note = getNoteFromFrequency(frequency)

                        runOnUiThread {

                            if(note != "Unknown") {
                                noteTextView.text = note
                            }
                        }
                    }
                }.start()
            }
        }

        private fun stopRecording() {
            isRecording = false
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
        }


        private fun getNoteFromFrequency(frequency: Double): String {

            if (frequency > 0) {
                Log.e("test", frequency.toString())
                Log.e("test", (12 * log2(frequency / A4_FREQUENCY) + 57).toString())
            }
            val noteIndex = (12 * log2(frequency / A4_FREQUENCY) + 57).roundToInt()



            if (noteIndex in 0 until NOTES.size){
                Log.e("test",NOTES[noteIndex])
            }
            return if (noteIndex in 0 until NOTES.size) {
                NOTES[noteIndex] + " - " + frequency.toString()

            } else {
                "Unknown"
            }
        }

        override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<String>,
            grantResults: IntArray
        ) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            when (requestCode) {
                REQUEST_RECORD_AUDIO_PERMISSION -> {
                    permissionToRecordAccepted =
                        grantResults[0] == PackageManager.PERMISSION_GRANTED
                }
            }
            if (!permissionToRecordAccepted) {
                finish()
            }
        }

        companion object {
            private const val SAMPLE_RATE = 44100
            private const val A4_FREQUENCY = 440.0

            private val NOTES = arrayOf(
                "C", "C#", "D", "D#", "E", "F",
                "F#", "G", "G#", "A", "A#", "B",
                "C1", "C1#", "D1", "D1#", "E1", "F1",
                "F1#", "G1", "G1#", "A1", "A1#", "B1",
                "C2", "C2#", "D2", "D2#", "E2", "F2",
                "F2#", "G2", "G2#", "A2", "A2#", "B2",
                "C3", "C3#", "D3", "D3#", "E3", "F3",
                "F3#", "G3", "G3#", "A3", "A3#", "B3",
                "C4", "C4#", "D4", "D4#", "E4", "F4",
                "F4#", "G4", "G4#", "A4", "A4#", "B4"


                )
        }
}