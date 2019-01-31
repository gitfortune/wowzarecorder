package com.hnradio.wowzarecorder;

import com.hnradio.wowzarecorder.bean.ChannelBean;
import com.hnradio.wowzarecorder.bean.ProgramBean;
import com.hnradio.wowzarecorder.config.RecorderProperties;
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

    RecorderProperties properties;


    /*public static RecorderRunnable recorderRunnable;


    @PostConstruct
    public void init(){
        recorderRunnable = this;
        recorderRunnable.properties = this.properties;
    }*/

    private String streamName;

    private ChannelBean channel;

    int i = 1;

    public RecorderRunnable(ChannelBean channel, RecorderProperties properties) {
        this.streamName = channel.getStreamName();
        this.channel = channel;
        this.properties = properties;
    }

    @Override
    public void run() {
        log.info("线程开始执行");
        boolean flag = true;
        //获取此频率下所有节目
        List<ProgramBean> programs = channel.getPrograms();
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        for(ProgramBean program : programs){
            if(!program.isWillSplit()){
                continue;
            }
            if(flag){
                //只对每天第一个节目生效
                startRecording(program);
                flag = false;
            }else {
                //执行切片命令
                executorService.execute(() -> splitStream(program));
            }
            //计算该节目时长
            long timeDiff = DateUtil.timeDiff(program.getStarttime(), program.getEndtime());
            try {
                //先休眠一个节目时长后
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                log.error("根据节目时长休眠的线程出错：{}",e);
            }

        }
    }

    /**
     * 每天第一次执行，直接执行startRecording
     */
    public void startRecording(ProgramBean program){
        log.info("每天第一次执行，直接执行startRecording");
        //先创建文件夹
        String directory = createDirectories();

        String userName = properties.getUserName();
        String passWord = properties.getPassWord();
        String urlPrefix = properties.getUrlPrefix();
        String start = program.getStarttime().replace(":", "");
        String end = program.getEndtime().replace(":", "");
        String fileName = streamName+"_"+DateUtil.getDate("yyyyMMdd")+"_"+start+"_"+end;
        String url = urlPrefix+"/livestreamrecord?app=live&streamname="+streamName+ i++ +"&action=startRecording&format=2&outputPath="+directory
                + "&outputFile="+fileName+".mp4";
        try {
            OkHttpUtil.digest(userName,passWord,url);
        } catch (IOException e) {
            log.error("每天第一次执行录制命令出错：{}",e);
        }
    }

    /**
     * 切片，分割直播流，向wowza发送先停止再开始的命令
     */
    public void splitStream(ProgramBean program) {

        String userName = properties.getUserName();
        String passWord = properties.getPassWord();
        String urlPrefix = properties.getUrlPrefix();
        String start = program.getStarttime().replace(":", "");
        String end = program.getEndtime().replace(":", "");
        String fileName = streamName+"_"+DateUtil.getDate("yyyyMMdd")+"_"+start+"_"+end;
        String directory = createDirectories();
        /*if(StringUtils.isBlank(directory)){
            log.info("找不到存储目录");
            return;
        }*/
        //停止命令
        String stopCommand = urlPrefix+"/livestreamrecord?app=live&streamname="+streamName+"&action=stopRecording&format=2";
        //开始命令
        String startCommand = urlPrefix+"/livestreamrecord?app=live&streamname="+streamName+"&action=startRecording&format=2&outputPath="+directory
                + "&outputFile="+fileName+".mp4";
        try {
            //停止
            OkHttpUtil.digest(userName,passWord,stopCommand);
            //开始
            OkHttpUtil.digest(userName,passWord,startCommand);
        } catch (IOException e) {
            log.error("向wowza发送切片命令出错：{}",e);
        }
        log.info(streamName+"向wowza发送先停止再开始命令");
        //执行更改后缀命令
//        changeSuffix(program,properties.getUrlPrefix()+uuid.toString());
    }

    /**
     * 创建文件夹
     */
    public String createDirectories(){
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
    public String getStoragePath(ProgramBean program){
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

}
