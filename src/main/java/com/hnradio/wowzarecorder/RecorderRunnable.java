package com.hnradio.wowzarecorder;

import com.hnradio.wowzarecorder.bean.ChannelBean;
import com.hnradio.wowzarecorder.bean.ProgramBean;
import com.hnradio.wowzarecorder.config.RecorderProperties;
import com.hnradio.wowzarecorder.utils.DateUtil;
import com.hnradio.wowzarecorder.utils.OkHttpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * 单个频率的录制线程
 */
@Slf4j
public class RecorderRunnable implements Runnable {

    @Autowired
    RecorderProperties properties;

    String streamName;

    ChannelBean channel;

    String name;

    public RecorderRunnable(ChannelBean channel) {
        this.channel = channel;
        this.streamName = channel.getStreamName();
        this.name = channel.getName();
    }

    @Override
    public void run() {
        boolean flag = true;
        //创建存储文件夹
        //获取此频率下所有节目
        List<ProgramBean> programs = channel.getPrograms();
//        log.info("AAAAA"+programs.size());
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        for(ProgramBean program : programs){
            if(!program.isWillSplit()){
                continue;
            }
            if(flag){
                //只对每天第一个节目生效
                startRecording();
                flag = false;
            }
            //计算该节目时长
            long timeDiff = DateUtil.timeDiff(program.getStarttime(), program.getEndtime());
            try {
                //先休眠一个节目时长后
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                log.error("根据节目时长休眠时出错：{}",e);
            }
            //执行切片命令
            executorService.execute(() -> splitStream(program));
        }
    }

    /**
     * 每天第一次执行，直接执行startRecording
     */
    public void startRecording(){
        log.info("每天第一次执行，直接执行startRecording");
//        OkHttpUtil.getSync(url+"/livestreamrecord?app=live&streamname="+streamName+"&action=startRecording&format=1");
    }

    /**
     * 切片，分割直播流，向wowza发送先停止再开始的命令
     */
    public void splitStream(ProgramBean program) {
//        log.info(name+"向wowza发送先停止再开始命令");
        UUID uuid = UUID.randomUUID();
        String urlPrefix = properties.getUrlPrefix();
        String startCommand = urlPrefix+"/livestreamrecord?app=live&streamname="+streamName+"&action=startRecording&format=1&outputPath="+properties.getTemp()
                              + "&outputFile="+uuid+".flv";
        try {
//            OkHttpUtil.getSync(urlPrefix+"/livestreamrecord?app=live&streamname="+streamName+"&action=stopRecording&format=1");
            OkHttpUtil.getSync(startCommand);
        } catch (IOException e) {
            log.error("向wowza发送分割命令出错：{}",e);
        }
        //执行更改后缀命令
        changeSuffix(program,properties.getUrlPrefix()+uuid.toString());
    }

    /**
     * 更改后缀，用ffmpeg转换格式，音频转为m4a
     */
    public String changeSuffix(ProgramBean program,String path){
        String inputPath = path+".flv";
        String outputPath = getStoragePath(program)+".m4a";
        List<String> commend = new ArrayList<>();
        commend.add(properties.getFfmpeg());
        commend.add("-i");
        commend.add(inputPath);
        commend.add("-y");  //如果有重名，覆盖掉
        commend.add("-loglevel");
        commend.add("error");
        commend.add("-acodec"); //设定声音编解码器，未设定时则使用与输入流相同的编解码器
        commend.add("-vn"); //不处理视频
        commend.add("-c");
        commend.add("copy");
        commend.add(outputPath);
        try {
            ProcessBuilder builder = new ProcessBuilder();
            builder.command(commend);
            Process process = builder.start();
            process.waitFor();  //执行完才能继续下一步，输出路径
        } catch (IOException | InterruptedException e) {
            log.error("ffmpeg出错了：{}",e);
//            throw new Exception();
        }
        return outputPath;
    }

    /**
     * 获取存储路径
     */
    public String getStoragePath(ProgramBean program){
        String yyyyMMdd = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String name = program.getName();
        String startTime = program.getStarttime();
        String endTime = program.getEndtime();

        String fileName = name+"_"+startTime+"_"+endTime;

        File file = new File(properties.getStorage() + "/" + yyyyMMdd);
        if(!file.exists()){
            file.mkdir();
        }
        return file.getPath()+"/"+fileName;
    }



}
