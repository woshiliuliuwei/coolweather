package com.coolweather.android;

import android.app.ProgressDialog;
import android.os.Build;
import android.os.Bundle;

import android.annotation.TargetApi;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.coolweather.android.db.City;
import com.coolweather.android.db.County;
import com.coolweather.android.db.Province;
import com.coolweather.android.util.HttpUtil;
import com.coolweather.android.util.Utility;

import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import static android.content.ContentValues.TAG;
import static org.litepal.LitePalApplication.getContext;

/**
 * Created by HZK on 2018/12/19.
 */
public class ChooseAreaFragment extends Fragment {
    public static final int LEVEL_PROVINCE = 0;
    public static final int LEVEL_CITY = 1;
    public static final int LEVEL_COUNTRY = 2;

    private ProgressDialog progressDialog;
    private TextView title_text;
    private Button back_button;
    private ListView listView;

    private ArrayAdapter<String> adapter;
    private List<String> datalist = new ArrayList<>();

    /**
     * 省列表
     */
    private List<Province> provinceList;
    /**
     * 市列表
     */
    private List<City> cityList;
    /**
     * 县列表
     */
    private List<County> countryList;
    /**
     * 选中的省
     */
    private Province selectedProvince;
    /**
     * 选中的城市
     */
    private City selectCity;
    /**
     * 当前选中的级别
     */
    private int currentlevel;


   @TargetApi(Build.VERSION_CODES.M)
    @Override
   //获取控件实例
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.choose_area, container, false);
        title_text = (TextView) view.findViewById(R.id.title_text);
        back_button = (Button) view.findViewById(R.id.back_button);
        listView = (ListView) view.findViewById(R.id.list_view);
        //初始化ArrayAdapter
        adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, datalist);
        //设置为ListView的适配器
        listView.setAdapter(adapter);
        return view;
    }

    @Override
    /*设置ListView Button的点击事件*/
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        //监听列表
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (currentlevel == LEVEL_PROVINCE) {
                    selectedProvince = provinceList.get(position);
                    queryCities();
                } else if (currentlevel == LEVEL_CITY) {
                    selectCity = cityList.get(position);
                    queryCounties();
                }
            }
        });
        back_button.setOnClickListener(new View.OnClickListener() {
                                               @Override
            public void onClick(View v) {
                if (currentlevel == LEVEL_COUNTRY) {
                    queryCities();
                } else if (currentlevel == LEVEL_CITY) {
                    queryProvinces();
                }
            }
        });
        //从这开始加载省级数据
        queryProvinces();
    }

    /**
     * 查询所有省，优先从数据库中查询，若是没有再到服务器上查询
     */
    private void queryProvinces() {
        title_text.setText("中国");

        back_button.setVisibility(View.GONE);//将按钮隐藏
        provinceList = DataSupport.findAll(Province.class);//读取省级数据
        Log.d("ChooseAreaFragment","进来了");
        if (provinceList.size() > 0) {

            datalist.clear();
            for (Province province : provinceList) {
                datalist.add(province.getProvinceName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentlevel = LEVEL_PROVINCE;
        } else {
            String address = "http://guolin.tech/api/china";
            queryFromServer(address, "province");
        }
    }

    /**
     * 查询省内的市，优先从数据库中查询，若是没有再到服务器上查询
     */
    private void queryCities() {
        title_text.setText(selectedProvince.getProvinceName());
        back_button.setVisibility(View.VISIBLE);
        cityList = DataSupport.where("provinceid = ?", String.valueOf(selectedProvince.getId())).find(City.class);
        if (cityList.size() > 0) {
            datalist.clear();
            for (City city : cityList) {
                datalist.add(city.getCityName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentlevel = LEVEL_CITY;
        } else {
            int provinceCode = selectedProvince.getProvinceCode();
            String address = "http://guolin.tech/api/china/" + provinceCode;
            queryFromServer(address, "city");
        }
    }

    /**
     * 查询市内的县，优先从数据库中查询，若是没有再到服务器上查询
     */
    private void queryCounties() {
        title_text.setText(selectCity.getCityName());
        back_button.setVisibility(View.VISIBLE);
        countryList = DataSupport.where("cityid=?", String.valueOf(selectCity.getId())).find(County.class);
        if (countryList.size() > 0) {
            datalist.clear();
            for (County county : countryList) {
                datalist.add(county.getCountyName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentlevel = LEVEL_COUNTRY;
        } else {
            int provinceCode = selectedProvince.getProvinceCode();
            int cityCode = selectCity.getCityCode();
            String address = "http://guolin.tech/api/china/" + provinceCode + "/" + cityCode;
            queryFromServer(address, "county");
        }
    }

    /**
     * 根据传入的地址在服务器上查询省市县数据
     *
     @param address
     * @param type
     */
    private void queryFromServer(String address, final String type) {
        showProgressDialog();
        //向服务器发送请求


        HttpUtil.sendOkHttpRequest(address, new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responsetext = response.body().string();
                boolean result = false;
                if ("province".equals(type)) {
                    result = Utility.handleProvinceResponse(responsetext);
                } else if ("city".equals(type)) {
                    result = Utility.handleCityResponse(responsetext, selectedProvince.getId());
                } else if ("country".equals(type)) {
                    result = Utility.handleCountyResponse(responsetext, selectCity.getId());
                }
                if(result){
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeProgressDialog();
                            if ("province".equals(type)) {
                                queryProvinces();
                            } else if ("city".equals(type)) {
                                queryCities();
                            } else if ("country".equals(type)) {
                                queryCounties();
                            }
                        }
                    });
                }
            }
            @Override
            public void onFailure(Call call, IOException e) {
                getActivity().runOnUiThread(new Runnable() {
                   // @TargetApi(Build.VERSION_CODES.M)
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(getContext(),"加载失败",Toast.LENGTH_SHORT).show();
                    }
                });
            }


        });
    }
    //显示进度对话框
    private void showProgressDialog() {
        if(progressDialog==null){
            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setMessage("正在加载...");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();
    }
//关闭进度对话框
    private void closeProgressDialog() {
        if(progressDialog!=null){
            progressDialog.dismiss();
        }
    }

}

