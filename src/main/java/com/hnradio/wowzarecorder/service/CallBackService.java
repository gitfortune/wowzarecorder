package com.hnradio.wowzarecorder.service;

import com.google.gson.Gson;
import com.hnradio.wowzarecorder.bean.ProgramBean;
import com.hnradio.wowzarecorder.config.RecorderProperties;
import com.hnradio.wowzarecorder.utils.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CallBackService {

    /**
     * 向节目单系统发送数据
     * @param properties
     * @param streamName
     * @param program
     */
    public void sendData(RecorderProperties properties, String streamName, ProgramBean program){
        String start = program.getStarttime().replace(":", "");
        String end = program.getEndtime().replace(":", "");
        String filePath = properties.getStorage()+"/"+streamName+"/"+ DateUtil.getDate("yyyyMMdd");
        String fileName = streamName+"_"+DateUtil.getDate("yyyyMMdd")+"_"+start+"_"+end;

        program.setFilePath(filePath+"/"+fileName);
        String json = new Gson().toJson(program);

//        log.info("数据已发出"+json);
    }
}
