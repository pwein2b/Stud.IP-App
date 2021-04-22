package org.studip.unofficial_app.api.routes;

import java.util.HashMap;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;

public interface Discovery
{
    @GET("api.php/discovery")
    public Call<HashMap<String, HashMap<String,String>>> discovery();
    
}
