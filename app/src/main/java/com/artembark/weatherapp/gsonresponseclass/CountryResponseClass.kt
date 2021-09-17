package com.artembark.weatherapp.gsonresponseclass

data class CountryResponseClass(
    val response: Response
) {
    data class Response(
        val count: Int,
        val items: List<Item>
    ) {
        data class Item(
            val id: Int,
            val title: String
        )
    }
}