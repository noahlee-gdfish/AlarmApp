package com.noahlee.alarmapp

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.util.Calendar
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private val store by lazy { AlarmStore(this) }
    private val alarms = mutableListOf<Alarm>()
    private lateinit var listView: RecyclerView
    private lateinit var emptyView: TextView
    private var adapter: AlarmAdapter? = null

    private val tickHandler = Handler(Looper.getMainLooper())
    private val tickRunnable = object : Runnable {
        override fun run() {
            adapter?.notifyDataSetChanged()
            tickHandler.postDelayed(this, 1000L)
        }
    }

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* optional follow-up */ }

    private val alarmsChangedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            reloadAlarms()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        listView = findViewById(R.id.alarmList)
        emptyView = findViewById(R.id.emptyView)
        listView.layoutManager = LinearLayoutManager(this)

        findViewById<ExtendedFloatingActionButton>(R.id.fabAdd).setOnClickListener {
            showAddAlarmDialog()
        }

        reloadAlarms()
        tickHandler.post(tickRunnable)
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter(AlarmReceiver.ACTION_ALARMS_CHANGED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(alarmsChangedReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(alarmsChangedReceiver, filter)
        }
    }

    override fun onStop() {
        unregisterReceiver(alarmsChangedReceiver)
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        reloadAlarms()
    }

    override fun onDestroy() {
        tickHandler.removeCallbacks(tickRunnable)
        super.onDestroy()
    }

    private fun reloadAlarms() {
        alarms.clear()
        alarms.addAll(store.load())
        adapter = AlarmAdapter(alarms) { alarm -> showDeleteDialog(alarm) }
        listView.adapter = adapter
        val empty = alarms.isEmpty()
        emptyView.visibility = if (empty) android.view.View.VISIBLE else android.view.View.GONE
        listView.visibility = if (empty) android.view.View.GONE else android.view.View.VISIBLE
    }

    private fun showDeleteDialog(alarm: Alarm) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.delete_alarm_title)
            .setMessage(R.string.delete_alarm_message)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete) { _, _ ->
                AlarmScheduler.cancel(this, alarm.id)
                alarms.removeAll { it.id == alarm.id }
                store.save(alarms)
                reloadAlarms()
            }
            .show()
    }

    private fun showAddAlarmDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_add_alarm, null, false)
        val titleInput = view.findViewById<TextInputEditText>(R.id.titleInput)
        val labelDate = view.findViewById<TextView>(R.id.labelDate)
        val labelTime = view.findViewById<TextView>(R.id.labelTime)
        val btnDate = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnPickDate)
        val btnTime = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnPickTime)

        val draft = Calendar.getInstance().apply {
            add(Calendar.HOUR_OF_DAY, 1)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        fun refreshLabels() {
            labelDate.text = android.text.format.DateFormat.getMediumDateFormat(this).format(draft.timeInMillis)
            labelTime.text = android.text.format.DateFormat.getTimeFormat(this).format(draft.timeInMillis)
        }
        refreshLabels()

        btnDate.setOnClickListener {
            val picker = MaterialDatePicker.Builder.datePicker()
                .setSelection(draft.timeInMillis)
                .setTitleText(R.string.pick_date)
                .build()
            picker.addOnPositiveButtonClickListener { millis ->
                val keep = Calendar.getInstance().apply { timeInMillis = millis }
                draft.set(Calendar.YEAR, keep.get(Calendar.YEAR))
                draft.set(Calendar.MONTH, keep.get(Calendar.MONTH))
                draft.set(Calendar.DAY_OF_MONTH, keep.get(Calendar.DAY_OF_MONTH))
                refreshLabels()
            }
            picker.show(supportFragmentManager, "date")
        }

        btnTime.setOnClickListener {
            val picker = MaterialTimePicker.Builder()
                .setTimeFormat(
                    if (android.text.format.DateFormat.is24HourFormat(this)) {
                        TimeFormat.CLOCK_24H
                    } else {
                        TimeFormat.CLOCK_12H
                    },
                )
                .setHour(draft.get(Calendar.HOUR_OF_DAY))
                .setMinute(draft.get(Calendar.MINUTE))
                .setTitleText(R.string.pick_time)
                .build()
            picker.addOnPositiveButtonClickListener {
                draft.set(Calendar.HOUR_OF_DAY, picker.hour)
                draft.set(Calendar.MINUTE, picker.minute)
                draft.set(Calendar.SECOND, 0)
                draft.set(Calendar.MILLISECOND, 0)
                refreshLabels()
            }
            picker.show(supportFragmentManager, "time")
        }

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.add_alarm)
            .setView(view)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.save, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val title = titleInput.text?.toString()?.trim().orEmpty()
                val at = draft.timeInMillis
                if (at <= System.currentTimeMillis()) {
                    Toast.makeText(this, R.string.past_time_error, Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val alarm = Alarm(
                    id = UUID.randomUUID().mostSignificantBits and Long.MAX_VALUE,
                    title = title.ifBlank { getString(R.string.alarm_title_hint) },
                    triggerAtMillis = at,
                )
                alarms.add(alarm)
                alarms.sortBy { it.triggerAtMillis }
                store.save(alarms)
                AlarmScheduler.schedule(this, alarm)
                reloadAlarms()
                dialog.dismiss()
            }
        }
        dialog.show()
    }
}
