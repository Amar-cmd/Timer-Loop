package com.heeraya.timerloop

import android.content.Context
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class RingtoneWrapper(val ringtone: Ringtone, val uri: Uri)

class RingtoneAdapter(
    private val ringtoneList: List<RingtoneWrapper>,
    private val context: Context,
    private val onRingtoneSelected: (RingtoneWrapper) -> Unit
) : RecyclerView.Adapter<RingtoneAdapter.RingtoneViewHolder>() {

    var currentRingtone: RingtoneWrapper? = null

    private var selectedIndex = -1
    private val sharedPreferences = context.getSharedPreferences("ringtone_prefs", Context.MODE_PRIVATE)
    private val editor = sharedPreferences.edit()

    init {
        // Load selected index from shared preferences
        selectedIndex = sharedPreferences.getInt("selected_ringtone_index", -1)
    }

    inner class RingtoneViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ringtoneName: TextView = itemView.findViewById(R.id.ringtone_name)
        val radioButton: RadioButton = itemView.findViewById(R.id.ringtone_radio_button)

        init {
            itemView.setOnClickListener {
                selectedIndex = adapterPosition
                notifyDataSetChanged()

                // Save selected ringtone URI to shared preferences
                val selectedRingtone = ringtoneList[selectedIndex]
                editor.putString("selected_ringtone_uri", selectedRingtone.uri.toString()).apply()

                onRingtoneSelected(selectedRingtone)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RingtoneViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.ringtone_item, parent, false)
        return RingtoneViewHolder(view)
    }

    override fun onBindViewHolder(holder: RingtoneViewHolder, position: Int) {
        val ringtone = ringtoneList[position]
        holder.ringtoneName.text = ringtone.ringtone.getTitle(holder.itemView.context)
        holder.radioButton.isChecked = position == selectedIndex
    }

    override fun getItemCount() = ringtoneList.size

    fun stopCurrentRingtone() {
        currentRingtone?.ringtone?.stop()
    }
}
