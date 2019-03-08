package com.hnradio.wowzarecorder.utils;

import com.burgstaller.okhttp.AuthenticationCacheInterceptor;
import com.burgstaller.okhttp.CachingAuthenticatorDecorator;
import com.burgstaller.okhttp.digest.CachingAuthenticator;
import com.burgstaller.okhttp.digest.Credentials;
import com.burgstaller.okhttp.digest.DigestAuthenticator;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * okhttp工具类
 */
@Slf4j
public class OkHttpUtil {

    /**
     * okhttp的摘要认证 Digest authentication
     */
    public static Response digest(String userName,String passWord,String url) {

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
        Response response = null;
        try {
            response = client.newCall(request).execute();
        } catch (IOException e) {
            log.error("okHttp调用失败：{}",url,e);
        }finally {
            if(response != null) {
                response.body().close();
            }
        }
        return response;
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
     * 发送json参数的post请求
     */
    public static String post(String url,String json){
        log.info("发送的json"+json);
        OkHttpClient okHttpClient = new OkHttpClient();
        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(mediaType, json);
        Request request = new Request.Builder().url(url).post(body)
                .addHeader("content-type", "application/json").build();

        String res = null;
        try {
            Response response = okHttpClient.newCall(request).execute();
            if (!response.isSuccessful()) {
                log.error("Okhttp发送post请求没成功");
            }
            res = response.body().source().readUtf8();
        } catch (IOException e) {
            log.error("Okhttp发送post请求出错",e);
        }
        return res;
    }


    //原文：https://blog.csdn.net/qq_16240393/article/details/54863646
}
