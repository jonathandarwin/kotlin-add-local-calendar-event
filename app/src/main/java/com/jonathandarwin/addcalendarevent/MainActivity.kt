package com.jonathandarwin.addcalendarevent

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.CalendarContract
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.jonathandarwin.addcalendarevent.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.*
import java.util.jar.Manifest

class MainActivity : AppCompatActivity() {

    private lateinit var binding : ActivityMainBinding
    private val preference by lazy {
        getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)
    }

    private val WRITE_CALENDAR_REQUEST = 123
    private val SHARED_PREF_NAME = "AddEventCalendar"
    private val SHARED_PREF_COLUMN = "eventIdList"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        binding.etDatetime.setText(getCurrentDateTime())
        binding.etEventTitle.setText("Sample Title")
        binding.etEventDescription.setText("Sample Description")

        binding.btnSave.setOnClickListener {
            val message = validateInput()
            if(message != "") Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            else{
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.WRITE_CALENDAR,
                    android.Manifest.permission.READ_CALENDAR),
                    WRITE_CALENDAR_REQUEST)
            }
        }

        binding.btnDeleteAll.setOnClickListener {
            val eventIdList = preference.getString(SHARED_PREF_COLUMN, "") ?: ""
            eventIdList.split(";").map {
                if(!it.isNullOrEmpty()){
                    val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, it.toLong())
                    contentResolver.delete(uri, null, null)
                }
            }
            Toast.makeText(this, "Delete Success", Toast.LENGTH_SHORT).show()
        }
    }

    private fun validateInput() : String {
        return if(binding.etDatetime.text.toString().trim() == "" || binding.etEventTitle.text.toString().trim() == "" ||
            binding.etEventDescription.text.toString().trim() == "")
            "Please input all the field"
        else ""
    }

    private fun getTimeInMillis(raw : String) : Long {
        val input = SimpleDateFormat("yyyy-MM-dd HH:mm")
        val date = input.parse(raw)

        return date.time
    }

    private fun getCurrentDateTime() : String {
        val calendar = Calendar.getInstance()
        val output = SimpleDateFormat("yyyy-MM-dd HH:mm")
        return output.format(calendar.time)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode) {
            WRITE_CALENDAR_REQUEST -> {
                if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    setCalendar()
                }
            }
        }
    }

    private fun setCalendar() {
        val values = ContentValues()

        val dtStart = getTimeInMillis(binding.etDatetime.text.toString().trim())
        values.put(CalendarContract.Events.DTSTART, dtStart)
        values.put(CalendarContract.Events.TITLE, binding.etEventTitle.text.toString().trim())
        values.put(CalendarContract.Events.DESCRIPTION, binding.etEventDescription.text.toString().trim())
        values.put(CalendarContract.Events.DTEND, dtStart + 60*60*1000)
        // should have either DTEND or DURATION

        val timezone = TimeZone.getDefault()
        values.put(CalendarContract.Events.EVENT_TIMEZONE, timezone.id)

        values.put(CalendarContract.Events.CALENDAR_ID, getCalendarId())
        var uri = contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
        val id = uri?.lastPathSegment ?: ""
        saveEventId(id)

        Toast.makeText(this, "Event saved.", Toast.LENGTH_SHORT).show()
    }

    private fun getCalendarId() : Long? {
        val projection = arrayOf(CalendarContract.Calendars._ID, CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)

        var calCursor = contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            CalendarContract.Calendars.VISIBLE + " = 1 AND " + CalendarContract.Calendars.IS_PRIMARY + "=1",
            null,
            CalendarContract.Calendars._ID + " ASC"
        )

        if (calCursor != null && calCursor.count <= 0) {
            calCursor = contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                CalendarContract.Calendars.VISIBLE + " = 1",
                null,
                CalendarContract.Calendars._ID + " ASC"
            )
        }

        if (calCursor != null) {
            if (calCursor.moveToFirst()) {
                val calName: String
                val calID: String
                val nameCol = calCursor.getColumnIndex(projection[1])
                val idCol = calCursor.getColumnIndex(projection[0])

                calName = calCursor.getString(nameCol)
                calID = calCursor.getString(idCol)

                calCursor.close()
                return calID.toLong()
            }
        }
        return null
    }

    private fun saveEventId(id : String){
        var eventIdList = preference.getString(SHARED_PREF_COLUMN, "")
        eventIdList += "${id};"
        with(preference.edit()){
            putString(SHARED_PREF_COLUMN, eventIdList)
            commit()
        }
    }
}