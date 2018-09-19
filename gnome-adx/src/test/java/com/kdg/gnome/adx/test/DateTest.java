package com.kdg.gnome.adx.test;

import com.kdg.gnome.share.task.AES;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Created by hbwang on 2017/12/11
 */
public class DateTest {

    public static void main(String[] args) throws Exception {


//        System.out.println(Calendar.getInstance().get(Calendar.DAY_OF_MONTH));
//
//        System.out.println(Calendar.getInstance().get(Calendar.DAY_OF_WEEK));
//        System.out.println(Calendar.getInstance().get(Calendar.HOUR_OF_DAY));
//        System.out.println(Calendar.getInstance().get(Calendar.HOUR));
//
//        System.out.println(URLEncoder.encode("http://192.168.111.76:8081/ad/impress?impressId=123&mid=9&uu=0"));
//
////        http://127.0.0.1:8081/ad/click?clickId=3401b40f-1f41-487b-9f69-e541e89d39aa-1513338303237&creativeId=1&redirect=
//
//        System.out.println(URLEncoder.encode("http://www.sina.com"));

//        String a = AES.encrypt("2000|1482230774", "d6797b0f95f8ead1", false);
//
//        String b = AES.decrypt("3dd5e5b6c16eea4635af0cd9820dda34", "d6797b0f95f8ead1", true);
//        System.out.println(a);
//        System.out.println(b);

        String s = "PdXltsFu6kY1rwzZgg3aNA==";
        System.out.println(s.length());


        BOY boy1 = new BOY();
        boy1.a = 1;
        boy1.b = 1;
        BOY boy2 = new BOY();
        boy1.b = 9;
        boy2.b = 9;
        List<BOY> boys = new ArrayList<>();

        boys.add(boy1);
        boys.add(boy2);


        System.out.println(boys);
        for (BOY boy : boys) {
            System.out.println(boy);
        }

        for (BOY boy : boys) {
            boy.a = 3;
            boy.b = 4;
        }
        for (BOY boy : boys) {
            System.out.println(boy.a + "   " + boy.b);
        }

        System.out.println(boys);
    }


    public static class BOY {
        public int a;
        public int b;
    }
}
