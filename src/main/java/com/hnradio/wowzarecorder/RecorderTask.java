package com.hnradio.wowzarecorder;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.hnradio.wowzarecorder.bean.ChannelBean;
import com.hnradio.wowzarecorder.utils.DateUtil;
import com.hnradio.wowzarecorder.utils.OkHttpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;

/**
 * 切片（录制）任务
 */
@Component
@Slf4j
public class RecorderTask {

    List<ChannelBean> channelList;

    ScheduledExecutorService service;

    @Scheduled(cron = "30 28 16 * * ?")
    public void RecorderExecutor(){
        try {
            //获取节目单json数据
            String sync = OkHttpUtil.getSync("http://program.hndt.com/get/vodset");
            List<ChannelBean> channels = new Gson().fromJson(sync,new TypeToken<List<ChannelBean>>() {
            }.getType());
            //过滤出需要切片的频率
            channelList = channels.stream().filter(item -> item.isWillSplit() == true).collect(Collectors.toList());
            log.info("需要录制的频率数：{}",channelList.size());
            //创建线程池，设置核心线程数
            service = Executors.newScheduledThreadPool(channelList.size());
            startUp();
        } catch (IOException e) {
            log.error("请求节目单出错：{}",e.getMessage());
            e.printStackTrace();
        }

    }


    public void startUp(){
        for(ChannelBean channel : channelList){
            service.execute(new RecorderRunnable());
        }


    }

    @Scheduled(cron = "30 28 16 * * ?")
    public void Test(){
        log.info("即将执行！");
         do {
            log.info("已经开始执行");
        }while (DateUtil.equalsWith("16:29"));



    }




}
