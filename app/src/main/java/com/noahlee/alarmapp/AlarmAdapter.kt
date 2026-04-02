package com.noahlee.alarmapp

import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AlarmAdapter(
    private val items: List<Alarm>,
    private val onLongClick: (Alarm) -> Unit,
) : RecyclerView.Adapter<AlarmAdapter.VH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_alarm, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val alarm = items[position]
        val ctx = holder.itemView.context
        holder.title.text = alarm.title.ifBlank { ctx.getString(R.string.app_name) }
        holder.date.text = DateFormat.getMediumDateFormat(ctx).format(alarm.triggerAtMillis)
        holder.time.text = DateFormat.getTimeFormat(ctx).format(alarm.triggerAtMillis)
        holder.remaining.text = RemainingFormatter.format(ctx, alarm.triggerAtMillis)
        holder.itemView.setOnLongClickListener {
            onLongClick(alarm)
            true
        }
    }

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.alarmTitle)
        val date: TextView = view.findViewById(R.id.alarmDate)
        val time: TextView = view.findViewById(R.id.alarmTime)
        val remaining: TextView = view.findViewById(R.id.alarmRemaining)
    }
}
