package com.koordy.app.ui.home

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.koordy.app.R
import com.koordy.app.databinding.ItemCalendarDayBinding

data class CalendarDay(
    val dayNum: Int,             // 0 = cellule vide (padding)
    val isCurrentMonth: Boolean,
    val isToday: Boolean,
    val eventTypes: List<String> // pour les points colorés
)

class CalendarDayAdapter(
    private var days: List<CalendarDay>,
    private var selectedDay: Int,
    private val onDayClick: (Int) -> Unit
) : RecyclerView.Adapter<CalendarDayAdapter.VH>() {

    inner class VH(val binding: ItemCalendarDayBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemCalendarDayBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = days.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val day = days[position]
        val b = holder.binding
        val ctx = b.root.context

        // Cellule vide (padding avant le 1er jour)
        if (day.dayNum == 0) {
            b.tvDayNum.text = ""
            b.tvDayNum.background = null
            b.dotsContainer.removeAllViews()
            b.root.isClickable = false
            return
        }

        b.tvDayNum.text = day.dayNum.toString()

        val isSelected = day.isCurrentMonth && day.dayNum == selectedDay

        // Apparence du chiffre selon l'état
        when {
            isSelected -> {
                b.tvDayNum.setBackgroundResource(R.drawable.bg_calendar_day_selected)
                b.tvDayNum.setTextColor(Color.WHITE)
            }
            day.isToday -> {
                b.tvDayNum.setBackgroundResource(R.drawable.bg_calendar_today)
                b.tvDayNum.setTextColor(ContextCompat.getColor(ctx, R.color.brand))
            }
            else -> {
                b.tvDayNum.background = null
                b.tvDayNum.setTextColor(
                    if (day.isCurrentMonth)
                        ContextCompat.getColor(ctx, R.color.text)
                    else
                        ContextCompat.getColor(ctx, R.color.muted)
                )
            }
        }

        // Clic uniquement sur les jours du mois courant
        b.root.isClickable = day.isCurrentMonth
        if (day.isCurrentMonth) {
            b.root.setOnClickListener { onDayClick(day.dayNum) }
        } else {
            b.root.setOnClickListener(null)
        }

        // Points colorés (max 3) pour les types d'événements
        b.dotsContainer.removeAllViews()
        val density = ctx.resources.displayMetrics.density
        val dotSizePx = (5 * density).toInt()
        val dotMarginPx = (2 * density).toInt()

        day.eventTypes.take(3).forEachIndexed { index, type ->
            val dot = View(ctx)
            val params = LinearLayout.LayoutParams(dotSizePx, dotSizePx)
            if (index > 0) params.marginStart = dotMarginPx
            dot.layoutParams = params

            val drawable = GradientDrawable()
            drawable.shape = GradientDrawable.OVAL
            drawable.setColor(eventColor(type))
            dot.background = drawable

            b.dotsContainer.addView(dot)
        }
    }

    fun update(newDays: List<CalendarDay>, newSelectedDay: Int) {
        days = newDays
        selectedDay = newSelectedDay
        notifyDataSetChanged()
    }

    fun setSelectedDay(day: Int) {
        selectedDay = day
        notifyDataSetChanged()
    }

    private fun eventColor(type: String): Int = when (type.uppercase()) {
        "MATCH"        -> Color.parseColor("#FF6B6B")
        "ENTRAINEMENT" -> Color.parseColor("#6CCFFF")
        "REUNION"      -> Color.parseColor("#A8FF60")
        else           -> Color.parseColor("#9AA3B2")
    }
}
