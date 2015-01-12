package net.logstash.log4j;

import junit.framework.Assert;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.NDC;
import org.apache.log4j.MDC;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class JSONEventLayoutTest {
    static Logger logger;
    static MockAppender appender;
    static final String[] logstashFields = new String[]{
            "@message",
            "@source_host",
            "@fields",
            "@timestamp"
    };

    @BeforeClass
    public static void setupTestAppender() {
        appender = new MockAppender(new JSONEventLayout());
        logger = Logger.getRootLogger();
        appender.setThreshold(Level.TRACE);
        appender.setName("mockappender");
        appender.activateOptions();
        logger.addAppender(appender);
    }

    @After
    public void clearTestAppender() {
        NDC.clear();
        appender.clear();
        appender.close();
    }

    @Test
    public void testJSONEventLayoutIsJSON() {
        logger.info("this is an info message");
        String message = appender.getMessages()[0];
        Assert.assertTrue("Event is not valid JSON", JSONValue.isValidJsonStrict(message));
    }

    @Test
    public void testJSONEventLayoutHasKeys() {
        logger.info("this is a test message");
        String message = appender.getMessages()[0];
        Object obj = JSONValue.parse(message);
        JSONObject jsonObject = (JSONObject) obj;

        for (String fieldName : logstashFields) {
            Assert.assertTrue("Event does not contain field: " + fieldName, jsonObject.containsKey(fieldName));
        }
    }
}
