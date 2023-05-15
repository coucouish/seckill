package com.example.seckilldemo.utils;

import com.example.seckilldemo.pojo.User;
import com.example.seckilldemo.vo.RespBean;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 生成用户工具类
 */
public class UserUtil {
    private static void createUser(int count) throws Exception {
        List<User> users = new ArrayList<>(count);
        for(int i = 0; i < count; i++){
            User user = new User();
            user.setId(13000000000L + i);
            user.setNickname("user"+i);
            user.setSalt("1a2b3c4d");
            user.setLoginCount(1);
            user.setRegisterDate(new Date());
            user.setPassword(MD5Util.inputPassToDBPass("123456",user.getSalt()));
            users.add(user);
        }
        System.out.println("create user");

        //插入数据库
        Connection conn = getConn();
        String sql = "insert into t_user(login_count,nickname,register_date,salt,password,id) values(?,?,?,?,?,?)";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        for(int i = 0; i < count; i++){
            User user = users.get(i);
            pstmt.setInt(1,user.getLoginCount());
            pstmt.setString(2,user.getNickname());
            pstmt.setTimestamp(3,new Timestamp(user.getRegisterDate().getTime()));
            pstmt.setString(4,user.getSalt());
            pstmt.setString(5,user.getPassword());
            pstmt.setLong(6,user.getId());
            pstmt.addBatch();
        }
        pstmt.executeBatch();
        pstmt.clearParameters();
        conn.close();
        System.out.println("insert into db");

        //登录，生成userTicket
        String urlString = "http://localhost:8080/login/doLogin";
        File file = new File("E://work//codeLearning//courses//config.txt");
        if(file.exists()){
            file.delete();
        }
        RandomAccessFile raf = new RandomAccessFile(file,"rw");
        raf.seek(0); //将文件指针移动到文件开始位置
        for(int i = 0 ; i < users.size(); i++){
            User user = users.get(i);
            URL url = new URL(urlString);
            HttpURLConnection co = ((HttpURLConnection) url.openConnection());
            co.setRequestMethod("POST");
            co.setDoOutput(true);
            OutputStream out = co.getOutputStream();
            String params = "mobile=" + user.getId() + "&password=" + MD5Util.inputPassToFromPass("123456");
            out.write(params.getBytes());
            out.flush();
            InputStream inputStream = co.getInputStream();
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            byte[] buff = new byte[1024];
            int len = 0;
            while((len = inputStream.read(buff)) >= 0){
                bout.write(buff,0,len);
            }
            inputStream.close();
            bout.close();
            String response = new String(bout.toByteArray());
            System.out.println(response);

//            ObjectMapper mapper = new ObjectMapper();
            //add to allow single quote
//            JsonFactory factory = new JsonFactory();
//            factory.enable(JsonParser.Feature.ALLOW_SINGLE_QUOTES);
            ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);;

            RespBean respBean = mapper.readValue(response,RespBean.class);
//            System.out.println("!!!!Mark");
            String userTicket = ((String) respBean.getObj());
            System.out.println(userTicket);
            System.out.println("create userTicket :"+user.getId());
            String row = user.getId() + "," +userTicket;
            raf.seek(raf.length());
            raf.write(row.getBytes());
            raf.write("\r\n".getBytes());
            System.out.println("write to file :" + user.getId());
        }
        raf.close();
        System.out.println("over");
    }

    private static Connection getConn() throws Exception {
        String url = "jdbc:mysql://localhost:3306/seckill?useUnicode=true&characterEncoding=UTF-8&serverTimezone=GMT%2B8";
        String username = "root";
        String password = "1234";
        String driver = "com.mysql.cj.jdbc.Driver";
        Class.forName(driver);
        return DriverManager.getConnection(url,username,password);
    }


    public static void main(String[] args) throws Exception {
        createUser(5000);
    }
}
