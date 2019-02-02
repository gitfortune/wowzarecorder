package com.hnradio.wowzarecorder;

import com.hnradio.wowzarecorder.bean.ChannelBean;
import com.hnradio.wowzarecorder.bean.ProgramBean;
import com.hnradio.wowzarecorder.config.RecorderProperties;
import com.hnradio.wowzarecorder.service.CallBackService;
import com.hnradio.wowzarecorder.utils.DateUtil;
import com.hnradio.wowzarecorder.utils.OkHttpUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * 单个频率的录制线程
 */
@Slf4j
public class RecorderRunnable implements Runnable {

    private CallBackService callBackService;

    private RecorderProperties properties;

    private String streamName;

    private ChannelBean channel;

    private String userName;

    private String passWord;

    //url前缀
    private String urlPrefix;

    //存储目录
    private String directory;

    RecorderRunnable(ChannelBean channel, RecorderProperties properties, CallBackService callBackService) {
        this.streamName = channel.getStreamName();
        this.channel = channel;
        this.properties = properties;
        this.userName = properties.getUserName();
        this.passWord = properties.getPassWord();
        this.urlPrefix = properties.getUrlPrefix();
        this.callBackService = callBackService;
    }

    @Override
    public void run() {
        log.info("线程开始执行");
        boolean flag = true;
        //获取此频率下所有节目
        List<ProgramBean> programs = channel.getPrograms();
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        //创建当天目录
        directory = createDirectories();

        String time = programs.get(0).getStarttime();
        //每天第一个节目，判断开始时间是否是00：00，如果不是，计算它的开始时间距离0点有多少毫秒，线程开始休眠，等节目开始再执行下一步
        if(!"00:00".equals(time)){
            try {
                Thread.sleep(DateUtil.timeDiff("00:00", time));
            } catch (InterruptedException e) {
                log.error("等待节目开始的线程休眠被中断：{}",e);
            }
        }

        //循环当前频率下的节目，按照顺序进行录制
        for(ProgramBean program : programs){
            //如果这个节目不需要录制
            if(!program.isWillSplit()){
                //不管之前有没有在录制，先发出停止录制命令
                this.stopRecording();
                log.info(program.getName()+"不录制");
                try {
                    //根据这个节目时长休眠
                    Thread.sleep(DateUtil.timeDiff(program.getStarttime(), program.getEndtime()));
//                    Thread.sleep(15000);
                } catch (InterruptedException e) {
                    log.error("非录制节目休眠时线程被中断：{}",e);
                }
                continue;
            }
            if(flag){
                //只对每天第一个需要录制的节目生效
                startRecording(program);
                flag = false;
            }else {
                //执行切片命令
                executorService.execute(() -> splitStream(program));
            }
            //计算该节目时长
            long timeDiff = DateUtil.timeDiff(program.getStarttime(), program.getEndtime());
            try {
                //休眠一个节目时长
                Thread.sleep(timeDiff);
            } catch (InterruptedException e) {
                log.error("等待录制结束的线程休眠被中断：{}",e);
            }
        }
    }

    /**
     * 开始录制命令
     */
    private void startRecording(ProgramBean program){

        String fileName = this.getFileName(program);
        String url = urlPrefix+"/livestreamrecord?app=live&streamname="+streamName +"&action=startRecording&option=append&format=2&outputPath="+directory
                + "&outputFile="+fileName+".mp4";
        OkHttpUtil.digest(userName, passWord, url);
        log.info("当前录制节目名称为：{}",fileName);
    }

    /**
     * 停止录制命令
     */
    private void stopRecording(){
        //停止命令
        String stopCommand = urlPrefix+"/livestreamrecord?app=live&streamname="+streamName+"&action=stopRecording&format=2&option=append";
        OkHttpUtil.digest(userName, passWord, stopCommand);
    }

    /**
     * 切片，分割直播流，向wowza发送先停止再开始的命令
     */
    private void splitStream(ProgramBean program) {

        //停止命令
        this.stopRecording();

        //开始命令
        this.startRecording(program);
        log.info("执行切片命令");
        //切片后，向节目单系统发送数据
        callBackService.sendData(properties, channel.getStreamName(), program);
    }

    /**
     * 创建文件夹
     */
    private String createDirectories(){
        String filePath = properties.getStorage()+"/"+streamName+"/"+DateUtil.getDate("yyyyMMdd");
        Path path = Paths.get(filePath);
        //如果文件目录不存在
        if(!Files.exists(path)){
            try {
                Files.createDirectories(path);

            } catch (IOException e) {
                log.error("文件夹创建失败",e);
            }
        }
        return filePath;
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
     * 获取存储路径和新文件名
     */
    private String getStoragePath(ProgramBean program){
        String yyyyMMdd = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String name = program.getName();
        String startTime = program.getStarttime();
        String endTime = program.getEndtime();

        String fileName = name+"_"+startTime+"_"+endTime;

        File file = new File(properties.getStorage() + "/" + yyyyMMdd);
        if(!file.exists()){
            file.mkdirs();
        }
        return file.getPath()+"/"+fileName;
    }

    /**
     * 生成录制文件名称
     */
    private String getFileName(ProgramBean program){
        String start = program.getStarttime().replace(":", "");
        String end = program.getEndtime().replace(":", "");
        return streamName+"_"+DateUtil.getDate("yyyyMMdd")+"_"+start+"_"+end;
    }
}
