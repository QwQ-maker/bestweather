package com.bestweather.android;


import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bestweather.android.db.City;
import com.bestweather.android.db.District;
import com.bestweather.android.db.Province;
import com.bestweather.android.util.HttpUtil;
import com.bestweather.android.util.Utility;

import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import static org.litepal.LitePalBase.TAG;

public class ChooseAreaFragment extends Fragment {
    public static final int LEVEL_PROVINCE=0;
    public static final int LEVEL_CITY=1;
    public static final int LEVEL_DISTRICT=2;
    private ProgressDialog progressDialog;
    private Button backButton;
    private TextView titleText;
    private ListView listView;
    private ArrayAdapter<String> adapter;
    private List<String>dataList=new ArrayList<>();
    private List<Province>provinceList;
    private List<City>cityList;
    private List<District>districtList;
    private Province selectedProvince;
    private City selectedCity;
    private District selectedDistrict;
    private int selectedLevel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view=inflater.inflate(R.layout.choose_area,container,false);
        titleText=view.findViewById(R.id.title_text);
        backButton=view.findViewById(R.id.back_button);
        listView=view.findViewById(R.id.choose_view);
        adapter=new ArrayAdapter<>(getContext(),android.R.layout.simple_list_item_1,dataList);
        listView.setAdapter(adapter);;
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (selectedLevel==LEVEL_PROVINCE){
                    selectedProvince=provinceList.get(position);
                    Log.w(TAG, "onItemClick: "+selectedProvince.getId()+"+"+selectedProvince.getProvinceCode() );
                    queryCities();
                }
                else if (selectedLevel==LEVEL_CITY){
                    selectedCity=cityList.get(position);
                    queryDistrict();
                }else if (selectedLevel==LEVEL_DISTRICT){
                    String weatherId=districtList.get(position).getWeatherId();
                    Intent intent=new Intent(getActivity(),WeatherActivity.class);
                    intent.putExtra("weather_id",weatherId);
                    startActivity(intent);
                    getActivity().finish();
                }
            }
        });
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedLevel==LEVEL_DISTRICT){
                    queryCities();
                }
                else if (selectedLevel==LEVEL_CITY){
                    queryProvinces();
                }
            }
        });
        queryProvinces();
    }


    private void showProgressDialog(){
        if (progressDialog==null){
            progressDialog=new ProgressDialog(getActivity());
            progressDialog.setMessage("Loading");
            progressDialog.setCancelable(false);
        }
        progressDialog.show();
    }

    private void closeProgressDialog(){
        if (progressDialog!=null){
            progressDialog.dismiss();
        }
    }

    private void queryProvinces(){
        titleText.setText("中国");
        backButton.setVisibility(View.GONE);
        provinceList=DataSupport.findAll(Province.class);
        if (provinceList.size()>0){
            dataList.clear();
            for (Province province:provinceList){
                dataList.add(province.getProvinceName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            selectedLevel=LEVEL_PROVINCE;
        }
        else {
            String adresss="http://guolin.tech/api/china";
            queryFromServer(adresss,"province");
        }
    }

    private void queryCities(){
        titleText.setText(selectedProvince.getProvinceName());
        backButton.setVisibility(View.VISIBLE);
        cityList=DataSupport.where("provinceid=?",String.valueOf(selectedProvince.getId())).find(City.class);
        if (cityList.size()>0){
            dataList.clear();
            for (City city:cityList){
                dataList.add(city.getCityName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            selectedLevel=LEVEL_CITY;
        }
        else {
            int provinceCode=selectedProvince.getProvinceCode();
            Log.w(TAG, "queryCities: "+provinceCode );
            String adresss="http://guolin.tech/api/china/"+provinceCode;
            queryFromServer(adresss,"city");
        }
    }

    private void queryDistrict(){
        titleText.setText(selectedCity.getCityName());
        backButton.setVisibility(View.VISIBLE);
        districtList=DataSupport.where("cityid=?",String.valueOf(selectedCity.getId())).find(District.class);
        if (districtList.size()>0){
            dataList.clear();
            for (District district:districtList){
                dataList.add(district.getDistrictName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            selectedLevel=LEVEL_DISTRICT;
        }
        else {
            int provinceCode=selectedProvince.getProvinceCode();
            int cityCode=selectedCity.getCityCode();
            String adresss="http://guolin.tech/api/china/"+provinceCode+"/"+cityCode;
            queryFromServer(adresss,"district");
        }

    }

    private void queryFromServer(String address,final String type){
        showProgressDialog();
        HttpUtil.sendOkHttpRequest(address, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(getContext(), "Failurea", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseText=response.body().string();
                boolean result=false;
                if ("province".equals(type)){
                    Log.w(TAG, "onResponse: result" );
                    result=Utility.handleProvinceResponse(responseText);
                }
                else if ("city".equals(type)){
                    result=Utility.handleCityResponse(responseText,selectedProvince.getId());
                }
                else if ("district".equals(type)){
                    result=Utility.handleDistrictResponse(responseText,selectedCity.getId());
                }
                if (result){
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeProgressDialog();
                            if ("province".equals(type)){
                                queryProvinces();
                            }
                            else if ("city".equals(type)){
                                queryCities();
                            }
                            else if("district".equals(type)){
                                queryDistrict();
                            }
                        }
                    });
                }
            }
        });

    }
}
