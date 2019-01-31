package com.hnradio.wowzarecorder.utils;

import com.burgstaller.okhttp.AuthenticationCacheInterceptor;
import com.burgstaller.okhttp.CachingAuthenticatorDecorator;
import com.burgstaller.okhttp.digest.CachingAuthenticator;
import com.burgstaller.okhttp.digest.Credentials;
import com.burgstaller.okhttp.digest.DigestAuthenticator;
import okhttp3.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * okhttp工具类
 */
public class OkHttpUtil {

    /**
     * okhttp的摘要认证 Digest authentication
     */
    public static int digest(String userName,String passWord,String url) throws IOException {

        final DigestAuthenticator authenticator = new DigestAuthenticator(new Credentials(userName, passWord));

        final Map<String, CachingAuthenticator> authCache = new ConcurrentHashMap<>();

        final OkHttpClient client = new OkHttpClient.Builder()
                .authenticator(new CachingAuthenticatorDecorator(authenticator, authCache))
                .addInterceptor(new AuthenticationCacheInterceptor(authCache))
                .build();
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        Response response = client.newCall(request).execute();
        return response.code();
    }

    /**
     * 同步的Get请求
     *
     * @param url
     * @return responseStr
     * @throws IOException
     */
    public static String getSync(String url) throws IOException {
        // 创建OKHttpClient对象
        OkHttpClient okHttpClient = new OkHttpClient();
        // 创建一个Request
        final Request request = new Request.Builder()
                .url(url)
                .build();
        Call call = okHttpClient.newCall(request);
        // 返回值为response
        Response response = call.execute();
        // 将response转化成String
        String responseStr = response.body().string();
        return responseStr;
    }

    /**
     * 异步的Get请求
     *
     * @param url url
     */
    public static void getAsyn(String url) {
        // 创建OKHttpClient对象
        OkHttpClient okHttpClient = new OkHttpClient();
        // 创建一个Request
        final Request request = new Request.Builder()
                .url(url)
                .build();
        Call call = okHttpClient.newCall(request);
        // 请求加入调度
        call.enqueue(new Callback() {
            // 请求失败的回调
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            // 请求成功的回调
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                // 将response转化成String
                String responseStr = response.body().string();
            }
        });
    }

    /**
     * 同步的Post请求
     *
     * @param url    url
     * @param params params
     * @return responseStr
     * @throws IOException
     */
    public static String postSync(String url, Map<String, String> params)
            throws IOException {
        // RequestBody
        RequestBody requestBody;
        if (params == null) {
            params = new HashMap<>();
        }
        // 创建OKHttpClient对象
        OkHttpClient okHttpClient = new OkHttpClient();
        FormBody.Builder builder = new FormBody.Builder();
        /**
         * 在这对添加的参数进行遍历
         */
        for (Map.Entry<String, String> map : params.entrySet()) {
            String key = map.getKey();
            String value;
            /**
             * 判断值是否是空的
             */
            if (map.getValue() == null) {
                value = "";
            } else {
                value = map.getValue();
            }
            /**
             * 把key和value添加到formBody中
             */
            builder.add(key, value);
        }
        requestBody = builder.build();
        // 创建一个Request
        final Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();
        Call call = okHttpClient.newCall(request);
        // 返回值为response
        Response response = call.execute();
        // 将response转化成String
        String responseStr = response.body().string();
        return responseStr;
    }
    //原文：https://blog.csdn.net/qq_16240393/article/details/54863646

    /**
     * 异步的Post请求
     *
     * @param url    url
     * @param params params
     * @return responseStr
     */
    public static void postAsyn(String url, Map<String, String> params) {
        // RequestBody
        RequestBody requestBody;
        if (params == null) {
            params = new HashMap<>();
        }
        // 创建OKHttpClient对象
        OkHttpClient okHttpClient = new OkHttpClient();
        FormBody.Builder builder = new FormBody.Builder();
        /**
         * 在这对添加的参数进行遍历
         */
        for (Map.Entry<String, String> map : params.entrySet()) {
            String key = map.getKey();
            String value;
            /**
             * 判断值是否是空的
             */
            if (map.getValue() == null) {
                value = "";
            } else {
                value = map.getValue();
            }
            /**
             * 把key和value添加到formBody中
             */
            builder.add(key, value);
        }
        requestBody = builder.build();
        // 创建一个Request
        final Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();
        Call call = okHttpClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                // 将response转化成String
                String responseStr = response.body().string();
            }
        });
    }
}
