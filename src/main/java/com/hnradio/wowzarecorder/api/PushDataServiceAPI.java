package com.hnradio.wowzarecorder.api;

import com.hnradio.wowzarecorder.common.RestResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(value = "hndt-program",fallback= PushDataRemoteHystrix.class)
public interface PushDataServiceAPI {

    @PostMapping(value = "/program/api/vodpost")
    RestResponse create(String string);
}

@Component
class PushDataRemoteHystrix implements PushDataServiceAPI {

    @Override
    public RestResponse create(String string) {
        return RestResponse.validfail(string);
    }

}
