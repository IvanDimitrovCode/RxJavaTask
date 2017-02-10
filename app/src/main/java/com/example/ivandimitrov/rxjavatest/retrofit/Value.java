package com.example.ivandimitrov.rxjavatest.retrofit;

/**
 * Created by Ivan Dimitrov on 2/10/2017.
 */

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Value {

    @SerializedName("id")
    @Expose
    private Integer id;
    @SerializedName("joke")
    @Expose
    private String  joke;
    @SerializedName("categories")
    @Expose
    private List<String> categories = null;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getJoke() {
        return joke;
    }

    public void setJoke(String joke) {
        this.joke = joke;
    }

    public List<String> getCategories() {
        return categories;
    }

    public void setCategories(List<String> categories) {
        this.categories = categories;
    }

}