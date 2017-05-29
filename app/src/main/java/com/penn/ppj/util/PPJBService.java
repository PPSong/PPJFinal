package com.penn.ppj.util;

import io.reactivex.Observable;
import retrofit2.http.Body;
import retrofit2.http.POST;

/**
 * Created by penn on 06/04/2017.
 */

public interface PPJBService {
    @POST("api")
    Observable<String> api(@Body String jBody);
}
