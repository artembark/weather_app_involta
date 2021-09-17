package com.artembark.weatherapp

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.artembark.weatherapp.gsonresponseclass.OpenWeatherResponseClass
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {
    private lateinit var cityNameTextView: TextView
    private lateinit var conditionImageView: ImageView
    private lateinit var tempTextView: TextView
    private lateinit var feelsLikeTempTextView: TextView
    private lateinit var windSpeedTextView: TextView
    private lateinit var humidityTextView: TextView
    private lateinit var pressureTextView: TextView
    private lateinit var localTimeTextView: TextView
    private lateinit var sharedPreferences: SharedPreferences

    private val citiesRequestCode = 0
    private val locationPermissionCode = 2

    private var locationHelper: GPSHelper? = null
    private var locationCallback: LocationCallback? = null

    //флаги для переключения режимов GPS/список
    private var requestingLocationUpdates = true
    private var cityName = "GPS"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //получаем данные из shared preferences
        sharedPreferences = getSharedPreferences("settings", MODE_PRIVATE)
        cityName = sharedPreferences.getString("cityName", "GPS").toString()
        requestingLocationUpdates = sharedPreferences.getBoolean("requestingLocationUpdates", true)

        cityNameTextView = findViewById(R.id.city_name_text_view)
        conditionImageView = findViewById(R.id.image_view_condition)

        tempTextView = findViewById(R.id.temp_text_view)
        feelsLikeTempTextView = findViewById(R.id.feels_like_temp_text_view)
        windSpeedTextView = findViewById(R.id.wind_speed_text_view)
        humidityTextView = findViewById(R.id.humidity_text_view)
        pressureTextView = findViewById(R.id.pressure_text_view)
        localTimeTextView = findViewById(R.id.local_time_text_view)

        locationHelper = GPSHelper(this)

        //коллбек геолокации
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                if (locationHelper?.locationRequest == null) {
                    return
                }

                val location = locationResult?.lastLocation

                parseGPS(location?.latitude, location?.longitude)

                if (locationResult != null) {
                    for (loc in locationResult.locations) {
                        parseGPS(loc.latitude, loc.longitude)
                    }
                }
            }
        }

        //обработчик нажатия на кнопку смены города
        findViewById<Button>(R.id.button_list).setOnClickListener {
            val intent = Intent(this, CitiesRecyclerViewActivity::class.java)
            startActivityForResult(intent, citiesRequestCode)
        }

        //проверяем, была ли запущена геолокация до этого
        if (requestingLocationUpdates) {
            performPermissionRequest()
        } else {
            val url =
                "https://api.openweathermap.org/data/2.5/weather?q=$cityName&units=metric&appid=key&lang=ru"
            requestJSON(url)
        }
    }

    //обрабатываем данные, пришедшие с активити выбора городов и режимы работы gps/список
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == citiesRequestCode) {
            if (resultCode == RESULT_OK) {
                val extras = data?.extras
                if (extras != null) {
                    cityName = extras.getString("cityName").toString()
                    //если имя в ответе GPS - запускаем геолокацию, иначе делаем запрос по полученному названию города
                    if (cityName == "GPS") {
                        performPermissionRequest()
                        requestingLocationUpdates = true
                    } else {
                        requestingLocationUpdates = false
                        val url =
                            "https://api.openweathermap.org/data/2.5/weather?q=$cityName&units=metric&appid=key&lang=ru"
                        requestJSON(url)
                    }
                }
            }
        }
    }
    //разбираем данные локации и выполняем запрос по геоданным
    private fun parseGPS(lat: Double?, long: Double?) {
        if (lat == null || long == null) return

        locationHelper?.stopLocationUpdates(locationCallback)

        val url =
            "https://api.openweathermap.org/data/2.5/weather?lat=$lat&lon=$long&units=metric&appid=key&lang=ru"
        requestJSON(url)
    }

    override fun onResume() {
        super.onResume()
        //заблокируем работу с GPS в случае работы по списку городов
        if (requestingLocationUpdates) {
            locationHelper?.startLocationUpdates(locationCallback)
            locationHelper?.getLocation { lat, long ->
                parseGPS(lat, long)
            }
        }
    }

    override fun onPause() {
        //сохраним текущие настройки в Shared Preferences для восстановления при закрытии приложения
        val editor = sharedPreferences.edit()
        editor.putString("cityName", cityName)
        editor.putBoolean("requestingLocationUpdates", requestingLocationUpdates)
        editor.apply()
        //заблокируем работу с GPS в случае работы по списку городов
        if (requestingLocationUpdates) locationHelper?.stopLocationUpdates(locationCallback)
        super.onPause()
    }

    //реакция на получение/не получение разрешений
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            locationPermissionCode -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //в случае удачи выставляем флаг работы с локацией
                    requestingLocationUpdates = true
                    Toast.makeText(this, "Разрешение получено", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(
                        this,
                        "Разрешение не получено. Выберите город из списка или дайте разрешение.",
                        Toast.LENGTH_LONG
                    ).show()
                    //в случае отказа переходим на второе активити для выбора города
                    val intent = Intent(this, CitiesRecyclerViewActivity::class.java)
                    requestingLocationUpdates = false
                    startActivityForResult(intent, citiesRequestCode)
                }
            }
        }
    }

    //запрос JSON с помощью Fuel
    private fun requestJSON(url: String) {
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
                        //запускаем парсинг полученных данных
                        parseJSON(result.get())
                    }
                }
            }
        }
    }

    //парсинг JSON и размещение данных по View
    private fun parseJSON(jsonString: String) {
        //преоборазуем по шаблону полученный JSON
        val response = Gson().fromJson(
            jsonString,
            OpenWeatherResponseClass::class.java
        )

        cityNameTextView.text = "В городе ${response.name} ${response.weather[0].description}"
        tempTextView.text = "${(response.main.temp).roundToInt()}"

        val uri = "@drawable/cond${response.weather[0].icon}"
        conditionImageView.setImageResource(
            resources.getIdentifier(
                uri,
                "drawable",
                packageName
            )
        )

        feelsLikeTempTextView.text = "${(response.main.feels_like).roundToInt()}"
        windSpeedTextView.text = "${response.wind.speed.roundToInt()}"
        humidityTextView.text = "${response.main.humidity}"
        pressureTextView.text = "${(response.main.pressure / 1.333).roundToInt()}"

        localTimeTextView.text = getDateFormat("EEEE, dd MMMM yyyy, HH:mm", response.dt)
    }

    //преобразование Unix TimeStamp
    fun getDateFormat(pattern: String, time: Int): String {
        val sdf = SimpleDateFormat(pattern, Locale("ru"))
        val fDate = Date(time * 1000L)
        return sdf.format(fDate)
    }

    //запрос разрешений или получение локации в случае уже полученных разрешений
    fun performPermissionRequest() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_NETWORK_STATE
                ), locationPermissionCode
            )
        } else {
            locationHelper?.getLocation { lat, long ->
                parseGPS(lat, long)
            }
        }
    }

}

