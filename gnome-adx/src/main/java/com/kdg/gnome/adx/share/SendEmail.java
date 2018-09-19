package com.kdg.gnome.adx.share;

import com.kdg.gnome.share.UtilOper;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * Created by hbwang on 2018/7/25
 */
public class SendEmail {

    private static final String adxConfPath =
            Thread.currentThread().getContextClassLoader().getResource("adx.conf").getPath();
    private static String mails = UtilOper.getStringValue(adxConfPath, "mails", "wanghaibo@kedabeijing.com,zhangzhuo@kedabeijing.com");

    public static void main(String[] args) throws MessagingException, UnsupportedEncodingException {
        sendEmail("101000000000000000000000000");
    }

    public static  InternetAddress[]  Address(){

        //多个接收账号
        String str = mails;
        InternetAddress[] address=null;
        try {
            List list = new ArrayList();//不能使用string类型的类型，这样只能发送一个收件人
            String []median = str.split(",");//对输入的多个邮件进行逗号分割
            for(int i=0; i<median.length; i++){
                list.add(new InternetAddress(median[i]));
            }
            address = (InternetAddress[])list.toArray(new InternetAddress[list.size()]);

        } catch (AddressException e) {
            e.printStackTrace();
        }
        return address;
    }
    public static void sendEmail(String msgText) throws MessagingException{

        Properties props = new Properties();
//        // 开启debug调试
//        props.setProperty("mail.debug", "true");
        // 发送服务器需要身份验证
        props.setProperty("mail.smtp.auth", "true");
        // 设置邮件服务器主机名
        props.setProperty("mail.host", "smtp.kedabeijing.com");
//        props.setProperty("mail.host", "smtp.163.com");
        // 发送邮件协议名称
        props.setProperty("mail.transport.protocol", "smtp");

        // 设置环境信息
        Session session = Session.getInstance(props);

        // 创建邮件对象
        Message msg = new MimeMessage(session);
        msg.setSubject("计划曝光量级邮件通知");
        // 设置邮件内容
        msg.setText(msgText);
        // 设置发件人 的名字
        msg.setFrom(new InternetAddress("adx-tech@kedabeijing.com"));

        Transport transport = session.getTransport();
        // 连接邮件服务器
        transport.connect("adx-tech@kedabeijing.com", "Adx@2018#KeDa");
        // 发送邮件  。设置收件人
        transport.sendMessage(msg, Address());
        // 关闭连接
        transport.close();
    }
}
