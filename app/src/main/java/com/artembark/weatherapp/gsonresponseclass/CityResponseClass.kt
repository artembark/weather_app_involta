package com.artembark.weatherapp.gsonresponseclass

data class CityResponseClass(
    val response: Response
) {
    data class Response(
        val count: Int,
        val items: List<Item>
    ) {
        data class Item(
            val area: String,
            val id: Int,
            val region: String,
            val title: String
        )
    }
}