package com.linglingdr00.weather.ui.forecast

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linglingdr00.weather.network.WeatherApi
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ForecastViewModel() : ViewModel() {

    private val TAG = "ForecastViewModel"
    // 授權碼
    private val API_KEY = "CWA-ED867C96-5D9B-44D4-A82F-619948365F33"

    // 提供給 recycler view 顯示的 ForecastItem list
    private val _forecastItemList = MutableLiveData<List<ForecastItem>>()
    val forecastItemList: LiveData<List<ForecastItem>> = _forecastItemList

    // 儲存取得資料的 array list
    private var dataArrayList: ArrayList<MutableMap<String, String>> = arrayListOf()
    // WeatherApi 狀態的 enum class
    enum class WeatherApiStatus { LOADING, ERROR, DONE }

    // 儲存 WeatherApi 的狀態
    private val _status = MutableLiveData<WeatherApiStatus>()
    val status: LiveData<WeatherApiStatus> = _status

    init {
        getWeatherData()
    }

    private fun getWeatherData() {

        viewModelScope.launch {
            //設定 WeatherApi 狀態為 LOADING
            _status.value = WeatherApiStatus.LOADING
            try {
                // 呼叫 WeatherApi 取得所有資料
                val allData = WeatherApi.retrofitService.getForecastData(key = API_KEY)
                // 新增一個 mapArrayList 存放所需資料
                val mapArrayList = ArrayList<MutableMap<String, String>>()

                allData.records.location.forEachIndexed { index, location ->
                    // 新增一個 map 存放資料(key, value)
                    val hashMap: MutableMap<String, String> = mutableMapOf()

                    val locationName = location.locationName
                    //Log.d(TAG, "index: $index, locationName: $locationName")
                    hashMap.put("location", locationName)

                    location.weatherElement.forEachIndexed { index, weatherElement ->
                        val elementName = weatherElement.elementName
                        //Log.d(TAG, "index: $index, elementName: $elementName")

                        weatherElement.time.forEachIndexed { index, time ->
                            val timeIndex = index+1
                            val startTime = datetimeFormat(time.startTime)
                            val endTime = datetimeFormat(time.endTime)
                            val timeString = "$startTime - $endTime"
                            hashMap.put("time$timeIndex", timeString)

                            val parameter = time.parameter.parameterName
                            val name = elementName
                            hashMap.put("$name$timeIndex", parameter)

                            if (name=="Wx") {
                                // 取得天氣代碼
                                val weatherCode = time.parameter.parameterValue
                                hashMap.put("weatherCode$timeIndex", weatherCode)
                            }
                        }
                    }
                    // 將 map 加入 array list 中
                    mapArrayList.add(hashMap)
                    //Log.d("$index: ", "${mapArrayList[index]}")
                }
                // 進一步處理資料
                handleData(mapArrayList)
                //設定 WeatherApi 狀態為 DONE
                _status.value = WeatherApiStatus.DONE
            } catch (e: Exception) {
                Log.d(TAG, "Failure: ${e.message}")
                //設定 WeatherApi 狀態為 ERROR
                _status.value = WeatherApiStatus.ERROR
                //設 forecastItemList 為空 list
                _forecastItemList.value = listOf()
            }
        }
    }

    private fun datetimeFormat(timeString: String): String {
        // 將 string 轉成 LocalDateTime
        val formatter1 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val dt = LocalDateTime.parse(timeString, formatter1)
        //Log.d(TAG,"dateTime: $dt")

        // 取得今天日期
        val now = LocalDate.now()
        val date: String

        if (now.equals(dt.toLocalDate())) {
            date = "今天"
        } else {
            date = "明天"
        }
        //Log.d(TAG, "date: $date")

        // 將 LocalDateTime 格式化成 string
        val formatter2 = DateTimeFormatter.ofPattern("HH:mm")
        val time = dt.format(formatter2)
        //Log.d(TAG, "time: $time")
        val formatTime = "$date $time"
        //Log.d(TAG, "date time: $formatTime")
        return formatTime
    }

    // 處理 data (array list 可使用 key 取得 value)
    private fun handleData(arrayList: ArrayList<MutableMap<String, String>>) {

        // temperature format
        arrayList.forEachIndexed { index, mutableMap ->

            for (i in 1..3) {
                // 把 MinT(最低溫) 和 MaxT(最高溫) 結合成一個 string
                val minT = mutableMap.get("MinT$i")
                //Log.d(TAG, "minT: $minT")
                val maxT = mutableMap.get("MaxT$i")
                //Log.d(TAG, "maxT: $maxT")
                val temperature = "$minT°C - $maxT°C"
                //Log.d(TAG, "temperature$i: $temperature")
                mutableMap.put("temperature$i", temperature)
                // 移除 MinT 和 MaxT
                mutableMap.remove("MinT$i")
                mutableMap.remove("MaxT$i")
            }
            Log.d("$index: ", "${arrayList[index]}")
        }
        //將 dataArrayList 設為處理好的資料
        dataArrayList = arrayList
    }

    // 轉換成 transToForecastItem
    private fun transToForecastItem(arrayList: ArrayList<MutableMap<String, String>>) {
        val tempList = mutableListOf<ForecastItem>()
        arrayList.forEachIndexed { index, mutableMap ->
            val item = ForecastItem(
                mutableMap.get("location").toString(),
                mutableMap.get("time1").toString(),
                mutableMap.get("weatherCode1").toString(),
                mutableMap.get("Wx1").toString(),
                mutableMap.get("PoP1").toString(),
                mutableMap.get("temperature1").toString(),
                mutableMap.get("time2").toString(),
                mutableMap.get("weatherCode2").toString(),
                mutableMap.get("Wx2").toString(),
                mutableMap.get("PoP2").toString(),
                mutableMap.get("temperature2").toString(),
                mutableMap.get("time3").toString(),
                mutableMap.get("weatherCode3").toString(),
                mutableMap.get("Wx3").toString(),
                mutableMap.get("PoP3").toString(),
                mutableMap.get("temperature3").toString()
            )
            tempList += item
            Log.d(TAG, "tempList: $tempList")
        }
        _forecastItemList.value = tempList
        Log.d(TAG, "_forecastItem: ${_forecastItemList.value}")
    }

    // 根據選擇的區域設定不同資料
    fun setAreaData(areaList: MutableList<String>) {
        //新增一個空 array list
        val tempArrayList: ArrayList<MutableMap<String, String>> = arrayListOf()
        //空 array list 的 index
        var index = 0
        //跑需要加入空 list 的每個縣市
        for (city in areaList) {
            var tempIndex = 0
            dataArrayList.forEachIndexed { index, map ->
                //找到 location 和 city 相同的那筆資料
                if (map.get("location").equals(city)) {
                    // 將 index 存起來
                    tempIndex = index
                }
            }
            //將篩選出的資料加入 tempArrayList
            tempArrayList.add(index, dataArrayList[tempIndex])
            Log.d(TAG, "index:$index, add: ${tempArrayList[index]}")
            index += 1
        }
        Log.d(TAG, "tempArrayList: $tempArrayList")
        // 將新的 array list 資料轉成 ForecastItem
        transToForecastItem(tempArrayList)
    }
}