package com.example.ankitdeora2856.alertapp;
import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;

import java.util.HashMap;
import java.util.Map;

public class DatabaseRequest extends StringRequest {
    private static final String REGISTER_REQUEST_URL = "http://battikgp.net23.net/DatabaseDriver2.php";
    private Map<String, String> params;

    public DatabaseRequest(double latitude,double longitude, float speed,int b_id, Response.Listener<String> listener) {
        super(Method.POST, REGISTER_REQUEST_URL, listener, null);
        params = new HashMap<>();
        params.put("latitude", latitude+"");
        params.put("b_id", b_id + "");
        params.put("speed_kmph", speed+"");
        params.put("longitude", longitude+"");
    }

    @Override
    public Map<String, String> getParams() {
        return params;
    }
}
