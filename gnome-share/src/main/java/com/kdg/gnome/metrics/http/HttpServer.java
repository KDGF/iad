//package com.kdg.gnome.metrics.http;
//
//import com.google.common.base.Preconditions;
//import com.kdg.gnome.Constants;
//import com.kdg.gnome.jmx.JMXJsonServlet;
//import org.eclipse.jetty.server.Connector;
//import org.eclipse.jetty.server.Server;
//import org.eclipse.jetty.server.ServerConnector;
//import org.eclipse.jetty.servlet.ServletHolder;
//import org.eclipse.jetty.webapp.WebAppContext;
//import org.apache.logging.log4j.Logger;
//import org.apache.logging.log4j.LogManager;
//
//import javax.servlet.http.HttpServlet;
//import java.io.FileNotFoundException;
//
///**
// * Created by mingzhang2 on 16/11/14.
// */
//public final class HttpServer {
//  private static final Logger LOG = LogManager.getLogger("ES_OUT_INFO");
//
//  private Server webServer;
//  private ServerConnector connector;
//  private WebAppContext webAppContext;
//
//  private static WebAppContext createWebAppContext() {
//    WebAppContext context = new WebAppContext();
//    context.setContextPath("/");
//
//    return context;
//  }
//
//  public HttpServer(String host, int port) throws Exception {
//    webServer = new Server();
//    connector = new ServerConnector(webServer);
//    connector.setHost(host);
//    connector.setPort(port);
//    webServer.setConnectors(new Connector[] {connector});
//    webAppContext = createWebAppContext();
//
//    initializeWebServer();
//
//    webServer.setHandler(webAppContext);
//  }
//
//  public void start() throws Exception {
//    webServer.start();
//  }
//
//  public void stop() throws Exception {
//    webServer.stop();
//  }
//
//  public void join() throws InterruptedException {
//    webServer.join();
//  }
//
//  public boolean isAlive() {
//    return webServer != null && webServer.isStarted();
//  }
//
//  @Override
//  public String toString() {
//    return super.toString();
//  }
//
//  private void initializeWebServer() throws FileNotFoundException {
//    Preconditions.checkNotNull(webAppContext);
//
//    // TODO 补全日志信息
//    String resourceBase;
//
//    String homeDir = System.getProperty(Constants.GNOME_HOME_DIR);
//    if (homeDir != null) {
//      resourceBase = homeDir + "/" + Constants.GNOME_WEBAPP_DIR;
//    } else {
//      LOG.warn("");
//      resourceBase = "./" + Constants.GNOME_WEBAPP_DIR;
//    }
//
////    File resourceDir = new File(resourceBase);
////    if (!resourceDir.exists() || !resourceDir.isDirectory()) {
////      LOG.error("");
////      throw new FileNotFoundException("");
////    }
////    LOG.info("set");
//    // 设置webapps的路径
//    webAppContext.setResourceBase(resourceBase);
//    addDefaultServlets();
//  }
//
//  public void addServlet(String name, String pathSpec,
//                         Class<? extends HttpServlet> clazz) {
//    ServletHolder holder = new ServletHolder(clazz);
//    if (name != null) {
//      holder.setName(name);
//    }
//
//    webAppContext.addServlet(holder, pathSpec);
//  }
//
//  protected void addDefaultServlets() {
//    addServlet("jmx", "/jmx", JMXJsonServlet.class);
//  }
//}
