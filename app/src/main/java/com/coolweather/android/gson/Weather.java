package com.coolweather.android.gson;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Weather {
    public String status;//成功返回OK，失败返回原因
    public Basic basic;
    public AQI aqi;
    public Now now;
    public Suggestion suggestion;

    @SerializedName("daily_forecast")//包含一个数组

    //使用List集合来引用Forecast类
    public List<Forecast> forecastList;
}
