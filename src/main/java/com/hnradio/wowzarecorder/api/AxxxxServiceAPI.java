package com.hnradio.wowzarecorder.api;

import com.hnradio.wowzarecorder.bean.ProgramBean;
import com.hnradio.wowzarecorder.common.RestResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;

/*
@FeignClient(value = "xxxx",fallback= AxxxxxRemoteHystrix.class)
public interface AxxxxServiceAPI {

    @PostMapping(value = "/xxx")
    RestResponse<ProgramBean> create(String string);
}

@Component
class AxxxxxRemoteHystrix implements AxxxxServiceAPI {

    @Override
    public RestResponse<ProgramBean> create(String string) {
        return RestResponse.validfail(string);
    }

}*/
