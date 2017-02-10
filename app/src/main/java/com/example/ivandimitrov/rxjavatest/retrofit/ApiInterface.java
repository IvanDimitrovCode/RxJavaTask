package com.example.ivandimitrov.rxjavatest.retrofit;

import io.reactivex.Observable;
import retrofit2.http.GET;

/**
 * Created by Ivan Dimitrov on 2/10/2017.
 */

public interface ApiInterface {
    @GET("random")
    Observable<Example> getJoke();
}
