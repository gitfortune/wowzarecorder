package com.hnradio.wowzarecorder;

import com.hnradio.wowzarecorder.bean.ChannelBean;
import com.hnradio.wowzarecorder.bean.ProgramBean;
import com.hnradio.wowzarecorder.config.RecorderProperties;
import com.hnradio.wowzarecorder.service.CallBackService;
import com.hnradio.wowzarecorder.utils.DateUtil;
import com.hnradio.wowzarecorder.utils.OkHttpUtil;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * @Auther: zhenghao
 * @Date: 2019/1/29
 * @Description: 单个频率的录制线程
 */
@Slf4j
public class RecorderRunnable implements Runnable {

    private CallBackService callBackService;

    private RecorderProperties properties;

    private String streamName;

    private ChannelBean channel;

    private String userName;

    private String passWord;

    /**
     *  url前缀
     */
    private String urlPrefix;

    /**
     * 存储目录
     */
    private String directory;

    private ScheduledThreadPoolExecutor executor;

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
        /* 避免了当重新部署时，每天第一个节目录制不上的问题。终止程序后，很多频率处于startRecording状态，
         * 当新代码重新部署好，第一次运行的start命令将不起作用，所以要先stopRecording，再开始新的录制
         */
        this.stopRecording();
        //获取此频率下所有节目
        List<ProgramBean> programs = channel.getPrograms();

        executor = new ScheduledThreadPoolExecutor(1);
        //创建当天目录
        directory = createDirectories();
        //找出需要录制的节目
        List<ProgramBean> collect = programs.stream().filter(this::willSplit).collect(Collectors.toList());

        for(ProgramBean programBean : collect){

            //计算节目的开始时间距离0点有多少毫秒
            long startDelay = DateUtil.timeDiff("00:00:00", programBean.getStarttime());
            //设定需要执行的命令，设定延迟执行时间
            executor.schedule(()->startRecording(programBean),startDelay,TimeUnit.MILLISECONDS);

            //计算节目的结束时间距离0点有多少毫秒，然后提前一秒
            LocalTime minusTime = LocalTime.parse(programBean.getEndtime()).minusSeconds(1L);
            long endDelay = DateUtil.timeDiff("00:00:00", minusTime.toString());
            //要提前一秒结束录制，避免和下一个开始时间冲突，上个节目的结束时间也是下个节目的开始时间
            executor.schedule(this::stopRecording,endDelay,TimeUnit.MILLISECONDS);

            //切片后（录制结束后），向节目单系统发送数据
            long pushTime = DateUtil.timeDiff("00:00:00", programBean.getEndtime());
            executor.schedule(() -> callBackService.pushData(properties, streamName, programBean), pushTime, TimeUnit.MILLISECONDS);

            //如果节目的结束时间是23:59:00，设定到23:59:50关闭这个单线程池
            if("23:59".equals(programBean.getEndtime())){
                long shutDownDelay = DateUtil.timeDiff("00:00:00","23:59:50");
                executor.schedule(this::shutDown,shutDownDelay,TimeUnit.MILLISECONDS);
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
        Response digest = OkHttpUtil.digest(userName, passWord, url);
        log.info("当前录制节目名称为：{}，{}",fileName,digest.toString());
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
            log.error("ffmpeg出错了",e);
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

    /**
     * 找出需要录制的节目
     */
    private boolean willSplit(ProgramBean programBean){
        return programBean.isWillSplit();
    }

    /**
     * 关闭线程池
     */
    private void shutDown(){
        executor.shutdown();
        executor.shutdownNow();
        log.info("shutDown ScheduledThreadPoolExecutor");
    }

}
