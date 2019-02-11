package com.hnradio.wowzarecorder;

import com.burgstaller.okhttp.AuthenticationCacheInterceptor;
import com.burgstaller.okhttp.CachingAuthenticatorDecorator;
import com.burgstaller.okhttp.digest.CachingAuthenticator;
import com.burgstaller.okhttp.digest.DigestAuthenticator;
import com.burgstaller.okhttp.digest.Credentials;
import com.google.gson.Gson;
import com.hnradio.wowzarecorder.api.PushDataServiceAPI;
import com.hnradio.wowzarecorder.bean.ProgramBean;
import com.hnradio.wowzarecorder.config.RecorderProperties;
import com.hnradio.wowzarecorder.utils.DateUtil;
import com.hnradio.wowzarecorder.utils.OkHttpUtil;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.commons.lang3.RandomUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PublicKey;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@SpringBootTest
@RunWith(SpringRunner.class)
@Slf4j
public class RecorderTaskTest {

    String url = "http://hnradio:123456@localhost:8086";

    String streamName = "myStream";

    @Autowired
    RecorderProperties properties;

    @Autowired
    PushDataServiceAPI serviceAPI;

    @Test
    public void test1(){
        String yyyyMMdd = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

//        RecorderTaskTest.log.info(yyyyMMdd);
//        RecorderTaskTest.log.info(LocalTime.now()+"");
//        RecorderTaskTest.log.info(LocalDate.now().plusDays(1).format(DateTimeFormatter.ofPattern("yyyyMMdd")));
        log.info(DateUtil.getDate("yyyy-MM-dd"));
//        DateUtil.timeDiff("09:00","09:45");
    }

    @Test
    public void stop(){
        try {
            String sync = OkHttpUtil.getSync(url + "/livestreamrecord?app=live&streamname=" + streamName + "&action=stopRecording&format=1");
            RecorderTaskTest.log.info(sync);
        } catch (IOException e) {
            RecorderTaskTest.log.error("发送停止命令出错{}：",e);
            e.printStackTrace();
        }
    }


    @Test
    public void est3() throws IOException {
        final DigestAuthenticator authenticator = new DigestAuthenticator(new Credentials("hnradio", "123456"));

        final Map<String, CachingAuthenticator> authCache = new ConcurrentHashMap<>();
        final OkHttpClient client = new OkHttpClient.Builder()
                .authenticator(new CachingAuthenticatorDecorator(authenticator, authCache))
                .addInterceptor(new AuthenticationCacheInterceptor(authCache))
                .build();

        String url = "http://localhost:8086/livestreamrecord?app=live&streamname=myStream&action=startRecording&format=1";
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        Response response = client.newCall(request).execute();
        RecorderTaskTest.log.info(response.code()+"");
    }


    @Test
    public void test5(){
        String filePath = "E:/tmp/stream/20190120";
        File file = new File(filePath);
        RecorderTaskTest.log.info(file.exists()+"");
        if(!file.exists()){
            file.mkdirs();
        }
    }

    @Test
    public void test6(){
        RecorderTaskTest.log.info(properties.getPassWord());
    }

    /*@Test
    public void Test7(){
        String filePath = properties.getStorage()+"/"+streamName+"/"+ DateUtil.getDate("yyyyMMdd");
        Path path = Paths.get(filePath);
        //如果文件目录不存在
        if(!Files.exists(path)){
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                log.error("文件夹创建失败",e);
            }
        }
        String urlPrefix = properties.getUrlPrefix();


        String stopUrl = urlPrefix+"/livestreamrecord?app=live&streamname="+streamName+"&action=stopRecording&format=2";

        try{
            for(int i = 0;i < 200;i ++){
                String startUrl = urlPrefix+"/livestreamrecord?app=live&streamname="+streamName +"&action=startRecording&format=2&outputPath="+filePath
                        + "&outputFile=newFile"+ i++ +".mp4";
                Response hnradio = OkHttpUtil.digest("hnradio", "123456", startUrl);
                log.info(startUrl);
                log.info("开始命令："+hnradio.code());
                Thread.sleep(10000);
                Response response = OkHttpUtil.digest("hnradio", "123456", stopUrl);
                log.info("停止命令："+response.code());
                log.info(stopUrl);
                Thread.sleep(1000);
            }
        }catch(Exception e){
            log.error("出错了");
        }

    }*/



    public void test7() throws IOException {
        String content = OkHttpUtil.getSync("http://program.hndt.com/get/vodset");
        FileOutputStream outputStream = null;
        FileChannel channel = null;
        String yyyyMMdd = LocalDate.now().plusDays(1).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        try {
            Path path = Paths.get(properties.getProgramGuides());
            //如果文件目录不存在
            if(!Files.exists(path)){
                try {
                    Files.createDirectory(path);
                } catch (IOException e) {
                    RecorderTaskTest.log.error("文件夹创建失败",e);
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
            RecorderTaskTest.log.error("保存节目单失败：{}",e);
        } finally {
            if(channel != null){
                try {
                    channel.close();
                } catch (IOException e) {
                    RecorderTaskTest.log.error("关闭FileChannel失败：{}",e);
                }
            }
            if(outputStream != null){
                try {
                    outputStream.close();
                } catch (IOException e) {
                    RecorderTaskTest.log.error("关闭FileOutputStream失败：{}",e);
                }
            }

        }

    }

    @Test
    public void test8(){
        long timeDiff = DateUtil.timeDiff("00:00:00", "01:59");
        RecorderTaskTest.log.info(timeDiff+"");

        long timeDiff1 = DateUtil.timeDiff("00:00:00", "01:59:59");
        RecorderTaskTest.log.info(timeDiff1+"");

        LocalTime localTime = LocalTime.parse("12:00").minusSeconds(1L);
        log.info(localTime.toString());

        long timeDiff3 = DateUtil.timeDiff("00:00:00", "00:59");


    }

    @Test
    public void test9(){
        RandomUtils.nextInt();
    }

    @Test
    public void test10(){
        ProgramBean programBean = new ProgramBean();
        programBean.setFilePath("/var/test");
        programBean.setChannelId(1);
        programBean.setName("河南 你早");
        programBean.setWillSplit(true);
        programBean.setId(1);
        programBean.setSigna("MSwwNjowMCwwNjozMA==");
        programBean.setCreateDate("2019-2-12");
        programBean.setStarttime("06:00");
        programBean.setEndtime("06:30");
        String json = new Gson().toJson(programBean);
        serviceAPI.create(json);
    }

    @Test
    public void test11(){
        LocalTime startParse = LocalTime.parse("00:00:00");

        if(startParse.getHour() == 00){

            log.info("true");
        }else {
            log.info("");
        }

    }

}

