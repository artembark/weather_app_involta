package com.artembark.weatherapp

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.artembark.weatherapp.gsonresponseclass.CityResponseClass
import com.artembark.weatherapp.gsonresponseclass.CountryResponseClass
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import com.google.gson.Gson


class CitiesRecyclerViewActivity : AppCompatActivity(), CityClickListener {
    var countryCode = 0
    var citiesArray: MutableList<City> = ArrayList()
    var countriesArray: MutableList<Country> = ArrayList()
    var citiesList: List<String> = ArrayList()
    private lateinit var countryName: String
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cities_recycler_view)

        val autoCompleteTextView = findViewById<View>(R.id.countries_ac_text_view) as AutoCompleteTextView

        //получаем данные из shared preferences
        sharedPreferences = getSharedPreferences("settings", MODE_PRIVATE)
        countryName = sharedPreferences.getString("countryName", "").toString()
        autoCompleteTextView.setText(countryName)

        val jsonString = sharedPreferences.getString("citiesArray", "")

        //тут скорее всего можно было по-другому
        if (jsonString != "") {
            citiesList = jsonString!!.split(";")
            for (i in citiesList) citiesArray.add(City(i))
            initRecycler()
        }

        //при запуске активити сделал фокус на корневой linearLayout, поэтому при клике на textview
        //фокус меняется и срабатывает listener
        autoCompleteTextView.setOnFocusChangeListener { view, b ->
            if (b && countriesArray.isEmpty()) {
                //страны и города не меняются постоянно, но было интересно реализовать запросами
                val url =
                    "https://api.vk.com/method/database.getCountries?lang=0&need_all=1&count=300&v=5.126&access_token=key"

                //запрос JSON с помощью Fuel
                url.httpGet().responseString { _, _, result ->
                    when (result) {
                        is Result.Failure -> {
                            Toast.makeText(
                                this,
                                result.getException().message.toString(),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        is Result.Success -> {
                            runOnUiThread {
                                //преоборазуем по шаблону полученный JSON
                                val response = Gson().fromJson(
                                    result.get(),
                                    CountryResponseClass::class.java
                                )
                                //перебираем все item-ы и добавляем их поле title и id в list со странами
                                for (i in response.response.items) {
                                    countriesArray.add(Country(i.title, i.id))
                                }

                                //инициализируем адаптер для автокомплита
                                val adapter = ArrayAdapter(
                                    this,
                                    android.R.layout.simple_dropdown_item_1line, countriesArray
                                )
                                //назначаем автокомплиту адаптер
                                autoCompleteTextView.setAdapter(adapter)
                            }
                        }
                    }
                }
            }
        }

        //обрабатываем клик на элемент автокомплита, вычисляя id страны и передавая его в следующий запрос городов
        autoCompleteTextView.setOnItemClickListener { adapterView, _, i, _ ->
            citiesArray.clear()
            hideSoftKeyboard(this)
            val selectedCountry = adapterView.getItemAtPosition(i)

            for (country in countriesArray) {
                if (country.name == selectedCountry.toString()) {
                    countryName = country.name
                    countryCode = country.id
                    break
                }
            }

            val url =
                "https://api.vk.com/method/database.getCities?lang=0&country_id=$countryCode&need_all=0&count=300&v=5.126&access_token=key"

            //запрос JSON с помощью Fuel
            url.httpGet().responseString { _, _, result ->
                when (result) {
                    is Result.Failure -> {
                        Toast.makeText(
                            this,
                            result.getException().message.toString(),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    is Result.Success -> {
                        runOnUiThread {
                            //преоборазуем по шаблону полученный JSON
                            val response = Gson().fromJson(
                                result.get(),
                                CityResponseClass::class.java
                            )
                            //перебираем все item-ы и добавляем их поле title в list с городами
                            for (i in response.response.items) {
                                citiesArray.add(City(i.title))
                            }
                            //инициализируем recyclerView
                            initRecycler()
                        }
                    }
                }
            }
        }

        //обработчик кнопки включения геолокации
        findViewById<Button>(R.id.button_recycler_gps).setOnClickListener {
            val intentToMain = Intent()
            intentToMain.putExtra("cityName", "GPS")
            setResult(Activity.RESULT_OK, intentToMain)
            finish()
        }
    }

    //инициализация recyclerView
    private fun initRecycler() {
        val recyclerView: RecyclerView = findViewById(R.id.recycler_view)
        val adapter = CitiesRecyclerAdapter(this, citiesArray, this)
        recyclerView.adapter = adapter
    }

    //переопределение функции нажтия на элемент recyclerView
    override fun onCityClickListener(city: City) {
        val intent = Intent()
        intent.putExtra("cityName", city.name)
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    //сохраняем данные для последующей работы
    override fun onPause() {
        val editor = sharedPreferences.edit()
        editor.putString("countryName", countryName)
        val jsonString = citiesArray.joinToString(separator = ";")
        editor.putString("citiesArray", jsonString)
        editor.apply()

        super.onPause()
    }
    //для того, чтобы спрятать клавиатуру
    fun hideSoftKeyboard(activity: Activity) {
        val inputMethodManager = activity.getSystemService(
            INPUT_METHOD_SERVICE
        ) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(
            activity.currentFocus!!.windowToken, 0
        )
    }
}