package com.kdg.gnome.adx.test;

import com.kdg.gnome.adx.handler.GServiceServerHandler;
import com.kdg.gnome.adx.share.GSessionInfo;
import com.kdg.gnome.http.GServer;
import com.kdg.gnome.share.Constants;
import com.kdg.gnome.share.OriginRequest;
import com.kdg.gnome.share.UtilOper;
import org.httpkit.HeaderMap;
import org.httpkit.server.*;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.nio.ByteBuffer;

import static org.httpkit.HttpUtils.HttpEncode;

public class TestAdnOri
{
    private static final Logger log = LogManager.getLogger("ES_OUT_INFO");
    public static class TestServerHandler implements IHandler
    {
        public TestServerHandler()
        {}

        public void close(int timeoutMs)
        {
            System.err.println("close.");
        }

        public void handle(AsyncChannel channel, Frame frame)
        {
            System.err.println("handle channel and frame");
        }

        public void handle(HttpRequest request, final RespCallback callback)
        {
            OriginRequest originReq = new OriginRequest();
            if(request.method.KEY == request.method.GET.KEY)
            {
                originReq.type = Constants.HTTP_REQ_TYPE_GET;
                originReq.getQuery = request.queryString;
            }
            else if(request.method.KEY == request.method.POST.KEY)
            {
                originReq.type = Constants.HTTP_REQ_TYPE_POST;
                originReq.postBody = request.getPostBody();
            }
            else
            {
                originReq.type = Constants.HTTP_REQ_TYPE_OTHER;
            }
            
            originReq.url = request.uri;
            originReq.headers = request.getHeaders();
            
            GSessionInfo sessInfo = GSessionInfo.getNewSession(originReq);
            
            String testData = "start_aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa" + 
        "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb" + 
                    "cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc" +
        "dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd" + 
                    "eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee" + 
        "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff" +
                    "gggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggg_end";
            
            String rspData = sessInfo.sid + "1_" + testData + "2_" + testData + "3_" + testData;
            HeaderMap header = new HeaderMap();
            header.put("Connection", "Keep-Alive");
            header.put("Content-Type", "application/json");
            ByteBuffer[] bytes = HttpEncode(200, header, rspData);
            callback.run(bytes);
            
            log.debug(testData);
        }

        public void handle(AsyncChannel channel, Frame.TextFrame frame)
        {
            System.err.println("handle channel and TextFrame frame");
        }

        public void clientClose(AsyncChannel channel, int status)
        {
            System.err.println("handle channel and status");
        }
    }

    public static void main(String[] args) throws Exception
    {
        args = new String[2];
        args[0] = "localhost";
        args[1] = "8080";
        if (null == args || args.length <= 1)
        {
            System.err.println("no ip or port param TestAdn, and exit!");
            return;
        }
        
        String ip = args[0];
        int port = Integer.parseInt(args[1]);
        
        GServer srvServer = new GServer(ip, port, new TestServerHandler(), 12);
        srvServer.start();
        
        while(true)
        {
            UtilOper.sleep(5000);
            System.err.println("TestAdn: main...thead...sleep...5...second.");
        }
    }
}
