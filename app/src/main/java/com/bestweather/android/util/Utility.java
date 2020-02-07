package com.bestweather.android.util;

import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.bestweather.android.db.City;
import com.bestweather.android.db.District;
import com.bestweather.android.db.Province;
import com.bestweather.android.gson.Weather;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import static android.content.ContentValues.TAG;
/*
解析从服务器返回的json数据。
 */

public class Utility {
    //省级数据
    public static boolean handleProvinceResponse(String response){
        if (!TextUtils.isEmpty(response)){
            try {
                JSONArray allProvinces=new JSONArray(response);
                for (int i=0;i<allProvinces.length();i++){
                    JSONObject provinceObject=allProvinces.getJSONObject(i);
                    Province province=new Province();
                    Log.w(TAG, "handleProvinceResponse: "+provinceObject.getInt("id") );
                    province.setProvinceCode(provinceObject.getInt("id"));
                    province.setProvinceName(provinceObject.getString("name"));
                    province.save();
                }
                return true;
            }catch (JSONException e){
                e.printStackTrace();
            }
        }
        return false;
    }
    //市级数据
    public static boolean handleCityResponse(String response,int provinceId){
        Log.w(TAG, "handleCityResponse: city going" );
        if (!TextUtils.isEmpty(response)){
            try {
                JSONArray allCityies=new JSONArray(response);
                for (int i=0;i<allCityies.length();i++){
                    JSONObject cityObject=allCityies.getJSONObject(i);
                    City city=new City();
                    city.setCityName(cityObject.getString("name"));
                    city.setCityCode(cityObject.getInt("id"));
                    city.setProvinceId(provinceId);
                    city.save();
                }
                return true;
            }catch (JSONException e){
                e.printStackTrace();
            }
        }
        Log.w(TAG, "handleCityResponse: No data" );        return false;
    }
    //区级数据
    public static boolean handleDistrictResponse(String response,int cityId){
        if (!TextUtils.isEmpty(response)){
            try {
                JSONArray allDistricts=new JSONArray(response);
                for (int i=0;i<allDistricts.length();i++){
                    JSONObject districtObject=allDistricts.getJSONObject(i);
                    District district=new District();
                    district.setDistrictName(districtObject.getString("name"));
                    district.setWeatherId(districtObject.getString("weather_id"));
                    district.setCityId(cityId);
                    district.save();
                }
                return true;
            }catch (JSONException e){
                e.printStackTrace();
            }
        }
        return false;
    }

    public static Weather handleWeatherResponse(String response){
        try {
            JSONObject jsonObject=new JSONObject(response);
            JSONArray jsonArray=jsonObject.getJSONArray("HeWeather");
            String weatherContent=jsonArray.getJSONObject(0).toString();
            return new Gson().fromJson(weatherContent,Weather.class);
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }
}
