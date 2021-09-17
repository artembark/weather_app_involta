package com.artembark.weatherapp

data class Country(val name: String, val id: Int) {
    override fun toString(): String {
        return this.name
    }
}