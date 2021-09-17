package com.artembark.weatherapp

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import java.util.*

class CitiesRecyclerAdapter(context: Context, private val cities: List<City>,private val cityClickListener: CityClickListener) :
    RecyclerView.Adapter<CitiesRecyclerAdapter.ViewHolder>() {

    private val inflater = LayoutInflater.from(context)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view: View = inflater.inflate(R.layout.city_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(cities[position])
        holder.itemView.setOnClickListener {
            cityClickListener.onCityClickListener(cities[position])
        }
    }

    override fun getItemCount(): Int {
        return cities.size
    }

    class ViewHolder constructor(view: View) : RecyclerView.ViewHolder(view) {
        val cityView: TextView = view.findViewById(R.id.item_city)

        fun bind(city: City) {
            cityView.text = city.name
        }


    }
}
