import com.kdg.gnome.share.task.AES;

/**
 * Created by hbwang on 2018/3/20
 */
public class Main {


    public static void main(String[] args) throws Exception {

//        List<String> a = new ArrayList<>();
//        a.add("aaa");
//        a.add("aab");
//        a.add("aac");
//
//        System.out.printf("" + a);
//
        String token = "fb4fcd3f1ff01bb2";
        String str = "impressId=8bee72cd-9860-41a0-b9d3-860680387f0e-1530008034780&accountId=16&advertiseId=46&campaignId=998&creativeId=340&adslotId=10220057&media=netease_newsreader_android.apk&s=1080*540&at=4&dt=android&deviceId=null&ctr=0.01&mediaid=20180545&channel=999&adunit=6364&op=1000.0&p=1000.0";

        String strTmp = AES.encrypt(str, token, true);

        String monitor = "http://127.0.0.1:9092/i?p=" + strTmp;

        System.out.println(AES.encrypt("1111", token, true));


//        String str = "[\"999995\",\"99999\"]";
//        JSONArray pidArr = JSON.parseArray(str);
//        for (Object json : pidArr) {
//            System.out.println((String) json);
//        }
//        System.out.println((pidArr.get(0)));

        int a = -8;

        int d = a >> 2;
        System.out.println(d);

        int c = -1;
        int b = a >>> 30;
        System.out.println(b);
    }
 }
