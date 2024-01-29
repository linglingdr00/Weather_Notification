package com.linglingdr00.weather.ui.forecast

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.viewModelScope
import com.linglingdr00.weather.ItemDecoration
import com.linglingdr00.weather.R
import com.linglingdr00.weather.databinding.FragmentForecastBinding
import kotlinx.coroutines.launch

class ForecastFragment : Fragment(), AdapterView.OnItemSelectedListener {

    private val TAG = "ForecastFragment"
    private val forecastViewModel: ForecastViewModel by activityViewModels()
    private var binding: FragmentForecastBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 每次進入 ForecastFragment 時都 load data
        forecastViewModel.viewModelScope.launch {
            try {
                //載入天氣預報資料
                forecastViewModel.getForecastData()
                Log.d(TAG, "載入天氣預報資料成功")
            } catch (e: Exception) {
                Log.d(TAG, "載入天氣預報資料失敗")
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val fragmentBinding = FragmentForecastBinding.inflate(inflater, container, false)
        binding = fragmentBinding

        return fragmentBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 設定 lifecycleOwner
        binding?.lifecycleOwner = this
        // 設定 view model 為 ForecastViewModel
        binding?.viewModel = forecastViewModel
        // 設 adapter 為 ForecastAdapter
        binding?.forecastRecyclerView?.adapter = ForecastAdapter()
        // 設定 ForecastItemDecoration 調整 item 邊距
        binding?.forecastRecyclerView?.addItemDecoration(ItemDecoration())
    }

    //新增 toolbar 的下拉式選單(spinner) menu
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        Log.d(TAG, "onCreateOptionsMenu()")
        menu.clear()
        inflater.inflate(R.menu.toolbar_menu, menu)

        val myCitySpinner = menu.findItem(R.id.citySpinner)
        val citySpinner = myCitySpinner?.actionView as Spinner
        // 設定顯示 citySpinner
        citySpinner.visibility = View.INVISIBLE

        val myAreaSpinner = menu.findItem(R.id.areaSpinner)
        val areaSpinner = myAreaSpinner?.actionView as Spinner

        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.area_array, // 選單中的 item
            android.R.layout.simple_spinner_item,
        ).also { adapter ->
            // 設定 dropdown 樣式
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // 設定 spinner 的 adapter
            areaSpinner.adapter = adapter
        }
        areaSpinner.onItemSelectedListener = this@ForecastFragment

    }

    // 當 spinner menu 選擇 item 時的動作
    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        Log.d(TAG, "onItemSelected: $position, $id")
        val cityList = when (position) {
            0 -> resources.getStringArray(R.array.northern_array).toList() as MutableList<String>
            1 -> resources.getStringArray(R.array.central_array).toList() as MutableList<String>
            2 -> resources.getStringArray(R.array.southern_array).toList() as MutableList<String>
            3 -> resources.getStringArray(R.array.eastern_array).toList() as MutableList<String>
            else -> resources.getStringArray(R.array.outlying_array).toList() as MutableList<String>
        }
        Log.d(TAG, "position: $position, cityList: $cityList")

        // 確認資料處理完成後
        /*if (forecastViewModel.status.value == ForecastViewModel.ForecastWeatherApiStatus.DONE) {
            // 設定顯示地區資料
            forecastViewModel.setAreaData(cityList)
            Log.d(TAG, "setAreaData()")
        }*/

        // 當資料處理完成時
        forecastViewModel.status.observe(viewLifecycleOwner) {
            if (it == ForecastViewModel.ForecastWeatherApiStatus.DONE) {
                // 設定顯示地區資料
                forecastViewModel.setAreaData(cityList)
                Log.d(TAG, "setAreaData()")
            }
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        Log.d(TAG, "onNothingSelected")
    }

    override fun onResume() {
        super.onResume()
        // 設定 toolbar title
        (activity as AppCompatActivity).supportActionBar?.title = getString(R.string.title_forecast)
        // 設定 menu
        setHasOptionsMenu(true)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

}