package com.skedgo.tripkit.ui.utils;

import com.skedgo.tripkit.ui.TripKitUI;

import java.io.IOException;
import java.util.List;

import androidx.core.util.Pair;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * This was deprecated.
 * Please consider using Retrofit or
 * inject {@link OkHttpClient} into class for testability.
 */
@Deprecated
public class HttpUtils {
    private static final MediaType MEDIA_TYPE_JSON = MediaType.parse("application/json");

    @Deprecated
    public static String get(String url, List<Pair<String, Object>> params) throws IOException {
        final HttpUrl.Builder builder = HttpUrl.parse(url).newBuilder();
        if (params != null) {
            for (int i = 0, size = params.size(); i < size; i++) {
                Pair<String, Object> param = params.get(i);
                builder.addQueryParameter(param.first, String.valueOf(param.second));
            }
        }
        final OkHttpClient httpClient = TripKitUI.getInstance().httpClient();
        final Request request = new Request.Builder()
            .url(builder.toString())
            .build();
        final Response response = httpClient.newCall(request).execute();
        if (!response.isSuccessful()) {
            throw new IOException("Unexpected code " + response);
        }
        return response.body().string();
    }

    @Deprecated
    public static String post(String url, String data) throws IOException, RuntimeException {
        final OkHttpClient httpClient = TripKitUI.getInstance().httpClient();
        final Request request = new Request.Builder()
            .url(url)
            .post(RequestBody.create(MEDIA_TYPE_JSON, data))
            .build();
        final Response response = httpClient.newCall(request).execute();
        if (!response.isSuccessful()) {
            throw new IOException("Unexpected code " + response);
        }
        return response.body().string();
    }
}