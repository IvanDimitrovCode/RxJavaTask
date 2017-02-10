package com.example.ivandimitrov.rxjavatest.retrofit;

/**
 * Created by Ivan Dimitrov on 2/10/2017.
 */


import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Example {

    @SerializedName("type")
    @Expose
    private String type;
    @SerializedName("value")
    @Expose
    private Value  value;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Value getValue() {
        return value;
    }

    public void setValue(Value value) {
        this.value = value;
    }

}