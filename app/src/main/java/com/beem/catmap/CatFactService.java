package com.beem.catmap;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONException;
import org.json.JSONObject;

public class CatFactService {

    public interface CatFactCallback {
        void onSuccess(String fact);
        void onError(String errorMessage);
    }

    public static void getRandomCatFact(Context context, CatFactCallback callback) {
        String url = "https://catfact.ninja/fact";

        RequestQueue queue = Volley.newRequestQueue(context);

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        String fact = jsonObject.getString("fact");
                        callback.onSuccess(fact);
                    } catch (JSONException e) {
                        callback.onError("JSON ayrıştırma hatası");
                        e.printStackTrace();
                    }
                },
                error -> {
                    callback.onError("İstek hatası: " + error.getMessage());
                });

        queue.add(stringRequest);
    }
}