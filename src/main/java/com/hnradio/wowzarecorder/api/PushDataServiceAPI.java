package com.hnradio.wowzarecorder.api;

import com.hnradio.wowzarecorder.bean.ProgramBean;
import com.hnradio.wowzarecorder.common.RestResponse;
//import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;

//@FeignClient(value = "micro-program",fallback= PushDataRemoteHystrix.class)
public interface PushDataServiceAPI {

    @PostMapping(value = "/api/vodpost")
    RestResponse<ProgramBean> create(String string);
}

@Component
class PushDataRemoteHystrix implements PushDataServiceAPI {

    @Override
    public RestResponse<ProgramBean> create(String string) {
        return RestResponse.validfail(string);
    }

}
