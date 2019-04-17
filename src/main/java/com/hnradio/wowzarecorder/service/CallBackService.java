package com.hnradio.wowzarecorder.service;

import com.google.gson.Gson;
import com.hnradio.wowzarecorder.api.PushDataServiceAPI;
import com.hnradio.wowzarecorder.bean.ProgramBean;
import com.hnradio.wowzarecorder.common.RestResponse;
import com.hnradio.wowzarecorder.config.RecorderProperties;
import com.hnradio.wowzarecorder.utils.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CallBackService {

    @Autowired
    PushDataServiceAPI serviceAPI;

    /**
     * 向节目单系统推送数据
     * @param properties
     * @param streamName
     * @param program
     */
    public void pushData(RecorderProperties properties, String streamName, ProgramBean program){
        String start = program.getStarttime().replace(":", "");
        String end = program.getEndtime().replace(":", "");
        String filePath = properties.getStorage()+"/"+streamName+"/"+ DateUtil.getDate("yyyyMMdd");
        String fileName = streamName+"_"+DateUtil.getDate("yyyyMMdd")+"_"+start+"_"+end+".mp4";

        program.setFilePath(filePath+"/"+fileName);
        program.setCreateDate(DateUtil.getDate("yyyy-MM-dd"));
        String json = new Gson().toJson(program);
        //向新节目单发送它要保存的数据
        RestResponse response = serviceAPI.create(json);
        if(response != null){
            log.info("调用pushData的返回信息",response.toString());
        }
        log.info("发送数据{}",program.getFilePath());
        
    }
}
