package com.hnradio.wowzarecorder;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.hnradio.wowzarecorder.bean.ChannelBean;
import com.hnradio.wowzarecorder.config.RecorderProperties;
import com.hnradio.wowzarecorder.utils.OkHttpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 切片（录制）任务
 */
@Component
@Slf4j
public class RecorderTask {

    @Autowired
    RecorderProperties properties;

    List<ChannelBean> channelList;

    ScheduledExecutorService service;

    @Scheduled(cron = "50 52 10 * * ?")
    public void RecorderExecutor(){
        try {
            //获取节目单json数据
            String sync = OkHttpUtil.getSync("http://program.hndt.com/get/vodset");
            List<ChannelBean> channels = new Gson().fromJson(sync,new TypeToken<List<ChannelBean>>() {
            }.getType());

            //过滤出需要切片的频率
            channelList = channels.stream().filter(this::willSplit).collect(Collectors.toList());
            log.info("需要录制的频率数：{}",channelList.size());

            //创建线程池，设置核心线程数
            service = Executors.newScheduledThreadPool(channelList.size());
            startUp();
        } catch (IOException e) {
            log.error("请求节目单出错：{}",e);
            e.printStackTrace();
        }
    }

    /**
     * 启动线程，开始执行录制任务
     */
    public void startUp(){
//        service.execute(new RecorderRunnable(channelList.get(37)));
        for(ChannelBean channel : channelList){
            service.execute(new RecorderRunnable(channel,properties));
        }
    }

    /**
     * 每天23：55开始关闭线程池
     */
    @Scheduled(cron = "00 59 08 * * ?")
    public void shutDownThreadPool(){
        service.shutdown();
        try {
            //等待200秒
            if(!service.awaitTermination(200, TimeUnit.SECONDS)){
                service.shutdownNow();
            }
        } catch (InterruptedException e) {
            log.error("等待终止线程超时，将关闭线程池：{}",e);
            service.shutdownNow();
        }
        log.info("关闭线程池");
    }

    /**
     * 找出isWillSplit为true并且programs不为空的频率
     */
    private boolean willSplit(ChannelBean channelBean){
        return channelBean.isWillSplit() && !CollectionUtils.isEmpty(channelBean.getPrograms());
    }

}
