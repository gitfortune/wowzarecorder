package com.hnradio.wowzarecorder;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.hnradio.wowzarecorder.bean.ChannelBean;
import com.hnradio.wowzarecorder.bean.ProgramBean;
import com.hnradio.wowzarecorder.config.RecorderProperties;
import com.hnradio.wowzarecorder.service.CallBackService;
import com.hnradio.wowzarecorder.utils.OkHttpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
    private RecorderProperties properties;

    @Autowired
    private CallBackService callBackService;

    private List<ChannelBean> channelList;

    private ScheduledExecutorService service;

//    @Scheduled(cron = "50 59 23 * * ?")
    @Scheduled(cron = "00 27 09 * * ?")
    public void RecorderExecutor(){
        try {
            //获取第二天的节目单json数据
            String sync = OkHttpUtil.getSync("http://program.hndt.com/get/vodset");
            //保存节目单
            saveProgramGuides(sync);

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
        }
    }

    /**
     * 启动线程，开始执行录制任务
     */
    public void startUp(){
//        service.execute(new RecorderRunnable(channelList.get(37)));
        for(ChannelBean channel : channelList){
            service.execute(new RecorderRunnable(channel,properties,callBackService));
        }
    }

    /**
     * 每天晚上定时关闭线程池
     */
    @Scheduled(cron = "56 57 23 * * ?")
//    @Scheduled(cron = "00 56 17 * * ?")
    public void shutDownThreadPool(){
        String urlPrefix = properties.getUrlPrefix();
        String userName = properties.getUserName();
        String passWord = properties.getPassWord();
        log.info("即将关闭线程池");
        //先发出停止录制命令
        for(ChannelBean channel : channelList){
            String stopCommand = urlPrefix+"/livestreamrecord?app=live&streamname="+channel.getStreamName()+"&action=stopRecording&format=2&option=append";
            OkHttpUtil.digest(userName, passWord, stopCommand);

            //找出每个频率的最后一个节目
            ProgramBean program = channel.getPrograms().get(channel.getPrograms().size()-1);
            //向节目单系统发送数据
            callBackService.sendData(properties,channel.getStreamName(),program);
            log.info("最后一次发停止命令"+program.getName());
        }
        service.shutdown();
        try {
            //等待x秒
            if(!service.awaitTermination(60, TimeUnit.SECONDS)){
                service.shutdownNow();
            }
        } catch (InterruptedException e) {
            log.error("等待终止线程超时：{}",e);
        }finally {
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

    /**
     * 保存节目单
     */
    private void saveProgramGuides(String content){
        FileOutputStream outputStream = null;
        FileChannel channel = null;
        //明天的日期
        String yyyyMMdd = LocalDate.now().plusDays(1).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        try {
            Path path = Paths.get(properties.getProgramGuides());
            //如果文件目录不存在
            if(!Files.exists(path)){
                try {
                    Files.createDirectory(path);
                } catch (IOException e) {
                    log.error("文件夹创建失败",e);
                }
            }
            File file = new File(properties.getProgramGuides()+"/"+yyyyMMdd+".json");
            if(!file.exists()){
                file.createNewFile();
            }
            outputStream = new FileOutputStream(file);
            channel = outputStream.getChannel();
            ByteBuffer buffer = ByteBuffer.wrap(content.getBytes());
            buffer.put(content.getBytes());
            buffer.flip();     //此处必须要调用buffer的flip方法
            buffer.clear();
            channel.write(buffer);
        } catch (IOException e) {
            log.error("保存节目单失败：{}",e);
        } finally {
            if(channel != null){
                try {
                    channel.close();
                } catch (IOException e) {
                    log.error("关闭FileChannel失败：{}",e);
                }
            }
            if(outputStream != null){
                try {
                    outputStream.close();
                } catch (IOException e) {
                    log.error("关闭FileOutputStream失败：{}",e);
                }
            }

        }

    }

}
