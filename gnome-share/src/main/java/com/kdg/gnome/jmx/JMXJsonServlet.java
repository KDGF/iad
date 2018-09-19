package com.kdg.gnome.jmx;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import javax.management.*;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.TabularData;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by mingzhang2 on 16/11/14.
 */
public class JMXJsonServlet extends HttpServlet {
  private static final Logger LOG = LogManager.getLogger("ES_OUT_INFO");

  private static final String CALLBACK_PARAM = "callback";

  protected transient MBeanServer mBeanServer = null;

  @Override
  public void init() throws ServletException {
    // Retrieve the MBean server
    mBeanServer = ManagementFactory.getPlatformMBeanServer();
  }

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) {
    String jsonpcb = null;
    PrintWriter writer = null;
    try {
      /*if (!isInstrumentationAccessAllowed(request, response)) {
        return;
      }*/

      JsonGenerator jg = null;
      response.setCharacterEncoding("utf-8");

      writer = response.getWriter();

      // "callback" parameter implies JSONP outpout
      jsonpcb = request.getParameter(CALLBACK_PARAM);
      if (jsonpcb != null) {
        response.setContentType("application/javascript; charset=utf-8");
        writer.write(jsonpcb + "(");
      } else {
        response.setContentType("application/json; charset=utf-8");
      }
      JsonFactory jsonFactory = new JsonFactory();
      jg = jsonFactory.createJsonGenerator(writer);
      jg.disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);
      jg.useDefaultPrettyPrinter();
      jg.writeStartObject();

      if (mBeanServer == null) {
        jg.writeStringField("result", "ERROR");
        jg.writeStringField("message", "No MBeanServer could be found");
        jg.close();
        LOG.error("No MBeanServer could be found.");
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        return;
      }

      // query per mbean attribute
      String getmethod = request.getParameter("get");
      if (getmethod != null) {
        String[] splitStrings = getmethod.split("\\:\\:");
        if (splitStrings.length != 2) {
          jg.writeStringField("result", "ERROR");
          jg.writeStringField("message", "query format is not as expected.");
          jg.close();
          response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
          return;
        }
        listBeans(jg, new ObjectName(splitStrings[0]), splitStrings[1],
          response);
        jg.close();
        return;

      }

      // query per mbean
      String qry = request.getParameter("qry");
      if (qry == null) {
        qry = "*:*";
      }
      listBeans(jg, new ObjectName(qry), null, response);
      jg.close();

    } catch (IOException e) {
      LOG.error("Caught an exception while processing JMX request", e);
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    } catch (MalformedObjectNameException e) {
      LOG.error("Caught an exception while processing JMX request", e);
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    } finally {
      if (jsonpcb != null) {
        writer.write(");");
      }
      if (writer != null) {
        writer.close();
      }
    }
  }

  private void listBeans(JsonGenerator jg, ObjectName qry, String attribute,
                         HttpServletResponse response) throws IOException {
    LOG.debug("Listing beans for {}", qry);
    Set<ObjectName> names = null;
    names = mBeanServer.queryNames(qry, null);

    jg.writeArrayFieldStart("beans");
    Iterator<ObjectName> it = names.iterator();
    while (it.hasNext()) {
      ObjectName oname = it.next();
      if (oname != null) {
        if (!oname.toString().startsWith("gnome.adx:name")) {
          continue;
        }
      }
      MBeanInfo minfo;
      String code = "";
      Object attributeinfo = null;
      try {
        minfo = mBeanServer.getMBeanInfo(oname);
        code = minfo.getClassName();
        String prs = "";
        try {
          if ("org.apache.commons.modeler.BaseModelMBean".equals(code)) {
            prs = "modelerType";
            code = (String) mBeanServer.getAttribute(oname, prs);
          }
          if (attribute != null) {
            prs = attribute;
            attributeinfo = mBeanServer.getAttribute(oname, prs);
          }
        } catch (AttributeNotFoundException e) {
          // If the modelerType attribute was not found, the class name is used
          // instead.
          LOG.error("getting attribute " + prs + " of " + oname
            + " threw an exception", e);
        } catch (MBeanException e) {
          // The code inside the attribute getter threw an exception so log it,
          // and fall back on the class name
          LOG.error("getting attribute " + prs + " of " + oname
            + " threw an exception", e);
        } catch (RuntimeException e) {
          // For some reason even with an MBeanException available to them
          // Runtime exceptionscan still find their way through, so treat them
          // the same as MBeanException
          LOG.error("getting attribute " + prs + " of " + oname
            + " threw an exception", e);
        } catch (ReflectionException e) {
          // This happens when the code inside the JMX bean (setter?? from the
          // java docs) threw an exception, so log it and fall back on the
          // class name
          LOG.error("getting attribute " + prs + " of " + oname
            + " threw an exception", e);
        }
      } catch (InstanceNotFoundException e) {
        //Ignored for some reason the bean was not found so don't output it
        continue;
      } catch (IntrospectionException e) {
        // This is an internal error, something odd happened with reflection so
        // log it and don't output the bean.
        LOG.error("Problem while trying to process JMX query: " + qry
          + " with MBean " + oname, e);
        continue;
      } catch (ReflectionException e) {
        // This happens when the code inside the JMX bean threw an exception, so
        // log it and don't output the bean.
        LOG.error("Problem while trying to process JMX query: " + qry
          + " with MBean " + oname, e);
        continue;
      }

      jg.writeStartObject();
      jg.writeStringField("name", oname.toString());

      jg.writeStringField("modelerType", code);
      if ((attribute != null) && (attributeinfo == null)) {
        jg.writeStringField("result", "ERROR");
        jg.writeStringField("message", "No attribute with name " + attribute
          + " was found.");
        jg.writeEndObject();
        jg.writeEndArray();
        jg.close();
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        return;
      }

      if (attribute != null) {
        writeAttribute(jg, attribute, attributeinfo);
      } else {
        MBeanAttributeInfo attrs[] = minfo.getAttributes();
        for (int i = 0; i < attrs.length; i++) {
          writeAttribute(jg, oname, attrs[i]);
        }
      }
      jg.writeEndObject();
    }
    jg.writeEndArray();
  }

  private void writeAttribute(JsonGenerator jg, ObjectName oname, MBeanAttributeInfo attr) throws IOException {
    if (!attr.isReadable()) {
      return;
    }
    String attName = attr.getName();
    if ("modelerType".equals(attName)) {
      return;
    }
    if (attName.indexOf("=") >= 0 || attName.indexOf(":") >= 0
      || attName.indexOf(" ") >= 0) {
      return;
    }
    Object value = null;
    try {
      value = mBeanServer.getAttribute(oname, attName);
    } catch (RuntimeMBeanException e) {
      // UnsupportedOperationExceptions happen in the normal course of business,
      // so no need to log them as errors all the time.
      if (e.getCause() instanceof UnsupportedOperationException) {
//        LOG.debug("getting attribute "+attName+" of "+oname+" threw an exception", e);
      } else {
        LOG.error("getting attribute " + attName + " of " + oname + " threw an exception", e);
      }
      return;
    } catch (RuntimeErrorException e) {
      // RuntimeErrorException happens when an unexpected failure occurs in getAttribute
      // for example https://issues.apache.org/jira/browse/DAEMON-120
//      LOG.debug("getting attribute "+attName+" of "+oname+" threw an exception", e);
      return;
    } catch (AttributeNotFoundException e) {
      //Ignored the attribute was not found, which should never happen because the bean
      //just told us that it has this attribute, but if this happens just don't output
      //the attribute.
      return;
    } catch (MBeanException e) {
      //The code inside the attribute getter threw an exception so log it, and
      // skip outputting the attribute
      LOG.error("getting attribute " + attName + " of " + oname + " threw an exception", e);
      return;
    } catch (RuntimeException e) {
      //For some reason even with an MBeanException available to them Runtime exceptions
      //can still find their way through, so treat them the same as MBeanException
      LOG.error("getting attribute " + attName + " of " + oname + " threw an exception", e);
      return;
    } catch (ReflectionException e) {
      //This happens when the code inside the JMX bean (setter?? from the java docs)
      //threw an exception, so log it and skip outputting the attribute
      LOG.error("getting attribute " + attName + " of " + oname + " threw an exception", e);
      return;
    } catch (InstanceNotFoundException e) {
      //Ignored the mbean itself was not found, which should never happen because we
      //just accessed it (perhaps something unregistered in-between) but if this
      //happens just don't output the attribute.
      return;
    }
//    LOG.debug(">>>>>{}:{}:{}", oname, attName, value);
    writeAttribute(jg, attName, value);
  }

  private void writeAttribute(JsonGenerator jg, String attName, Object value) throws IOException {
    jg.writeFieldName(attName);
    writeObject(jg, value);
  }

  private void writeObject(JsonGenerator jg, Object value) throws IOException {
    if (value == null) {
      jg.writeNull();
    } else {
      Class<?> c = value.getClass();
      if (c.isArray()) {
        jg.writeStartArray();
        int len = Array.getLength(value);
        for (int j = 0; j < len; j++) {
          Object item = Array.get(value, j);
          writeObject(jg, item);
        }
        jg.writeEndArray();
      } else if (value instanceof Number) {
        Number n = (Number) value;
        jg.writeNumber(n.toString());
      } else if (value instanceof Boolean) {
        Boolean b = (Boolean) value;
        jg.writeBoolean(b);
      } else if (value instanceof CompositeData) {
        CompositeData cds = (CompositeData) value;
        CompositeType comp = cds.getCompositeType();
        Set<String> keys = comp.keySet();
        jg.writeStartObject();
        for (String key : keys) {
          writeAttribute(jg, key, cds.get(key));
        }
        jg.writeEndObject();
      } else if (value instanceof TabularData) {
        TabularData tds = (TabularData) value;
        jg.writeStartArray();
        for (Object entry : tds.values()) {
          writeObject(jg, entry);
        }
        jg.writeEndArray();
      } else {
        jg.writeString(value.toString());
      }
    }
  }

}
