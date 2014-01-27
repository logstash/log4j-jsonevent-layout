package net.logstash.log4j;

import junit.framework.Assert;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.NDC;
import org.junit.After;
import org.junit.Before;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Created with IntelliJ IDEA.
 * User: jvincent
 * Date: 12/5/12
 * Time: 12:07 AM
 * To change this template use File | Settings | File Templates.
 */
public class JSONEventLayoutV1Test {
    static Logger logger;
    static MockAppenderV1 appender;
    static MockAppenderV1 userFieldsAppender;
    static JSONEventLayoutV1 userFieldsLayout;
    static final String userFieldsSingle = new String("field1:value1");
    static final String userFieldsMulti = new String("field2:value2,field3:value3");
    static final String userFieldsSingleProperty = new String("field1:propval1");

    static final String[] logstashFields = new String[]{
            "message",
            "source_host",
            "@timestamp",
            "@version"
    };

    @BeforeClass
    public static void setupTestAppender() {
        appender = new MockAppenderV1(new JSONEventLayoutV1());
        logger = Logger.getRootLogger();
        appender.setThreshold(Level.TRACE);
        appender.setName("mockappenderv1");
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
    public void testJSONEventLayoutHasUserFieldsFromProps() {
        System.setProperty(JSONEventLayoutV1.ADDITIONAL_DATA_PROPERTY, userFieldsSingleProperty);
        logger.info("this is an info message with user fields");
        String message = appender.getMessages()[0];
        Assert.assertTrue("Event is not valid JSON", JSONValue.isValidJsonStrict(message));
        Object obj = JSONValue.parse(message);
        JSONObject jsonObject = (JSONObject) obj;
        Assert.assertTrue("Event does not contain field 'field1'" , jsonObject.containsKey("field1"));
        Assert.assertEquals("Event does not contain value 'value1'", "propval1", jsonObject.get("field1"));
        System.clearProperty(JSONEventLayoutV1.ADDITIONAL_DATA_PROPERTY);
    }

    @Test
    public void testJSONEventLayoutHasUserFieldsFromConfig() {
        JSONEventLayoutV1 layout = (JSONEventLayoutV1) appender.getLayout();
        String prevUserData = layout.getUserFields();
        layout.setUserFields(userFieldsSingle);

        logger.info("this is an info message with user fields");
        String message = appender.getMessages()[0];
        Assert.assertTrue("Event is not valid JSON", JSONValue.isValidJsonStrict(message));
        Object obj = JSONValue.parse(message);
        JSONObject jsonObject = (JSONObject) obj;
        Assert.assertTrue("Event does not contain field 'field1'" , jsonObject.containsKey("field1"));
        Assert.assertEquals("Event does not contain value 'value1'", "value1", jsonObject.get("field1"));

        layout.setUserFields(prevUserData);
    }

    @Test
    public void testJSONEventLayoutUserFieldsMulti() {
        JSONEventLayoutV1 layout = (JSONEventLayoutV1) appender.getLayout();
        String prevUserData = layout.getUserFields();
        layout.setUserFields(userFieldsMulti);

        logger.info("this is an info message with user fields");
        String message = appender.getMessages()[0];
        Assert.assertTrue("Event is not valid JSON", JSONValue.isValidJsonStrict(message));
        Object obj = JSONValue.parse(message);
        JSONObject jsonObject = (JSONObject) obj;
        Assert.assertTrue("Event does not contain field 'field2'" , jsonObject.containsKey("field2"));
        Assert.assertEquals("Event does not contain value 'value2'", "value2", jsonObject.get("field2"));
        Assert.assertTrue("Event does not contain field 'field3'" , jsonObject.containsKey("field3"));
        Assert.assertEquals("Event does not contain value 'value3'", "value3", jsonObject.get("field3"));

        layout.setUserFields(prevUserData);
    }

    @Test
    public void testJSONEventLayoutUserFieldsPropOverride() {
        // set the property first
        System.setProperty(JSONEventLayoutV1.ADDITIONAL_DATA_PROPERTY, userFieldsSingleProperty);

        // set the config values
        JSONEventLayoutV1 layout = (JSONEventLayoutV1) appender.getLayout();
        String prevUserData = layout.getUserFields();
        layout.setUserFields(userFieldsSingle);

        logger.info("this is an info message with user fields");
        String message = appender.getMessages()[0];
        Assert.assertTrue("Event is not valid JSON", JSONValue.isValidJsonStrict(message));
        Object obj = JSONValue.parse(message);
        JSONObject jsonObject = (JSONObject) obj;
        Assert.assertTrue("Event does not contain field 'field1'" , jsonObject.containsKey("field1"));
        Assert.assertEquals("Event does not contain value 'propval1'", "propval1", jsonObject.get("field1"));

        layout.setUserFields(prevUserData);
        System.clearProperty(JSONEventLayoutV1.ADDITIONAL_DATA_PROPERTY);

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

    @Test
    public void testJSONEventLayoutHasNDC() {
        String ndcData = new String("json-layout-test");
        NDC.push(ndcData);
        logger.warn("I should have NDC data in my log");
        String message = appender.getMessages()[0];
        Object obj = JSONValue.parse(message);
        JSONObject jsonObject = (JSONObject) obj;

        Assert.assertEquals("NDC is wrong", ndcData, jsonObject.get("ndc"));
    }

    @Test
    public void testJSONEventLayoutExceptions() {
        String exceptionMessage = new String("shits on fire, yo");
        logger.fatal("uh-oh", new IllegalArgumentException(exceptionMessage));
        String message = appender.getMessages()[0];
        Object obj = JSONValue.parse(message);
        JSONObject jsonObject = (JSONObject) obj;
        JSONObject exceptionInformation = (JSONObject) jsonObject.get("exception");

        Assert.assertEquals("Exception class missing", "java.lang.IllegalArgumentException", exceptionInformation.get("exception_class"));
        Assert.assertEquals("Exception exception message", exceptionMessage, exceptionInformation.get("exception_message"));
    }

    @Test
    public void testJSONEventLayoutHasClassName() {
        logger.warn("warning dawg");
        String message = appender.getMessages()[0];
        Object obj = JSONValue.parse(message);
        JSONObject jsonObject = (JSONObject) obj;

        Assert.assertEquals("Logged class does not match", this.getClass().getCanonicalName().toString(), jsonObject.get("class"));
    }

    @Test
    public void testJSONEventHasFileName() {
        logger.warn("whoami");
        String message = appender.getMessages()[0];
        Object obj = JSONValue.parse(message);
        JSONObject jsonObject = (JSONObject) obj;

        Assert.assertNotNull("File value is missing", jsonObject.get("file"));
    }

    @Test
    public void testJSONEventHasLoggerName() {
        logger.warn("whoami");
        String message = appender.getMessages()[0];
        Object obj = JSONValue.parse(message);
        JSONObject jsonObject = (JSONObject) obj;
        Assert.assertNotNull("LoggerName value is missing", jsonObject.get("logger_name"));
    }

    @Test
    public void testJSONEventHasThreadName() {
        logger.warn("whoami");
        String message = appender.getMessages()[0];
        Object obj = JSONValue.parse(message);
        JSONObject jsonObject = (JSONObject) obj;
        Assert.assertNotNull("ThreadName value is missing", jsonObject.get("thread_name"));
    }

    @Test
    public void testJSONEventLayoutNoLocationInfo() {
        JSONEventLayoutV1 layout = (JSONEventLayoutV1) appender.getLayout();
        boolean prevLocationInfo = layout.getLocationInfo();

        layout.setLocationInfo(false);

        logger.warn("warning dawg");
        String message = appender.getMessages()[0];
        Object obj = JSONValue.parse(message);
        JSONObject jsonObject = (JSONObject) obj;

        Assert.assertFalse("atFields contains file value", jsonObject.containsKey("file"));
        Assert.assertFalse("atFields contains line_number value", jsonObject.containsKey("line_number"));
        Assert.assertFalse("atFields contains class value", jsonObject.containsKey("class"));
        Assert.assertFalse("atFields contains method value", jsonObject.containsKey("method"));

        // Revert the change to the layout to leave it as we found it.
        layout.setLocationInfo(prevLocationInfo);
    }

    @Test
    @Ignore
    public void measureJSONEventLayoutLocationInfoPerformance() {
        JSONEventLayoutV1 layout = (JSONEventLayoutV1) appender.getLayout();
        boolean locationInfo = layout.getLocationInfo();
        int iterations = 100000;
        long start, stop;

        start = System.currentTimeMillis();
        for (int i = 0; i < iterations; i++) {
            logger.warn("warning dawg");
        }
        stop = System.currentTimeMillis();
        long firstMeasurement = stop - start;

        layout.setLocationInfo(!locationInfo);
        start = System.currentTimeMillis();
        for (int i = 0; i < iterations; i++) {
            logger.warn("warning dawg");
        }
        stop = System.currentTimeMillis();
        long secondMeasurement = stop - start;

        System.out.println("First Measurement (locationInfo: " + locationInfo + "): " + firstMeasurement);
        System.out.println("Second Measurement (locationInfo: " + !locationInfo + "): " + secondMeasurement);

        // Clean up
        layout.setLocationInfo(!locationInfo);
    }

    @Test
    public void testDateFormat() {
        long timestamp = 1364844991207L;
        Assert.assertEquals("format does not produce expected output", "2013-04-01T19:36:31.207Z", JSONEventLayoutV1.dateFormat(timestamp));
    }
}
