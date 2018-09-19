package com.kdg.gnome.metrics.http;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Created by mingzhang2 on 16/11/14.
 */
public class MetricsServlet extends HttpServlet {
  private static final Logger LOG = LogManager.getLogger("ES_OUT_INFO");

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    LOG.debug("doGet");
    PrintWriter pw = resp.getWriter();
    pw.write("{\n" + "    \"name\" : \"java.lang:type=Memory\",\n"
        + "    \"modelerType\" : \"sun.management.MemoryImpl\",\n"
        + "    \"NonHeapMemoryUsage\" : {\n"
        + "    \"committed\" : 51773440,\n" + "    \"init\" : 24313856,\n"
        + "    \"max\" : 136314880,\n" + "    \"used\" : 51462336\n"
        + "}");
  }
}
