package net.logstash.log4j;

import junit.framework.Assert;
import net.logstash.log4j.fieldnames.LogstashCommonFieldNames;
import net.logstash.log4j.fieldnames.LogstashFieldNames;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import org.apache.log4j.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;


/* TODO: I made modifications so this test class would cover both V1 and V2 -- The intent being to use Junit's "Parameterized"
   TODO: functionality to run the full suite of tests, once for each layout.  Unfortunately, to keep true to the original tests
   TODO: this required a bunch of "instanceof" conditionals - not ideal.  So, going forward, it would be preferable to
   TODO: refactor and clean this up.  Maybe it makes more sense to just separate the test classes into V1 and V2
 */

/**
 * Created with IntelliJ IDEA.
 * User: jvincent
 * Date: 12/5/12
 * Time: 12:07 AM
 * To change this template use File | Settings | File Templates.
 */
@RunWith(Parameterized.class)
public class JSONEventLayoutV1Test {
    Logger logger;
    MockAppenderV1 appender;

    static final String userFieldsSingle = "field1:value1";
    static final String userFieldsMulti = "field2:value2,field3:value3";
    static final String userFieldsSingleProperty = "field1:propval1";

    static final String[] logstashFields = new String[]{
            "message",
            "source_host",
            "@timestamp",
            "@version"
    };

    private Layout jsonLayout;

    @Parameterized.Parameters
    public static java.util.Collection<Layout[]> data() {

        Layout[] layout1Array = new Layout[]{new JSONEventLayoutV1()};
        Layout[] layout2Array = new Layout[]{new JSONEventLayoutV2()};
        List<Layout[]> list = new ArrayList<Layout[]>();
        list.add(layout1Array);
        list.add(layout2Array);
        return list;

    }

    public JSONEventLayoutV1Test(Layout layout) {
        jsonLayout = layout;
    }

    @Before
    public void setupTestAppender() {

        appender = new MockAppenderV1(jsonLayout);
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
        String additionalDataProperty = JSONEventLayoutV1.ADDITIONAL_DATA_PROPERTY;
        if (appender.getLayout() instanceof JSONEventLayoutV2) {
            JSONEventLayoutV2 layout = (JSONEventLayoutV2) appender.getLayout();
            additionalDataProperty = layout.ADDITIONAL_DATA_PROPERTY;
        }
        System.setProperty(additionalDataProperty, userFieldsSingleProperty);
        logger.info("this is an info message with user fields");
        String message = appender.getMessages()[0];
        Assert.assertTrue("Event is not valid JSON", JSONValue.isValidJsonStrict(message));
        Object obj = JSONValue.parse(message);
        JSONObject jsonObject = (JSONObject) obj;
        Assert.assertTrue("Event does not contain field 'field1'", jsonObject.containsKey("field1"));
        Assert.assertEquals("Event does not contain value 'value1'", "propval1", jsonObject.get("field1"));
        System.clearProperty(additionalDataProperty);
    }

    @Test
    public void testJSONEventLayoutHasUserFieldsFromConfig() {
        IJSONEventLayout layout = getJsonEventLayout();
        String prevUserData = layout.getUserFields();
        layout.setUserFields(userFieldsSingle);

        logger.info("this is an info message with user fields");
        String message = appender.getMessages()[0];
        Assert.assertTrue("Event is not valid JSON", JSONValue.isValidJsonStrict(message));
        Object obj = JSONValue.parse(message);
        JSONObject jsonObject = (JSONObject) obj;
        Assert.assertTrue("Event does not contain field 'field1'", jsonObject.containsKey("field1"));
        Assert.assertEquals("Event does not contain value 'value1'", "value1", jsonObject.get("field1"));

        layout.setUserFields(prevUserData);
    }


    @Test
    public void testJSONEventLayoutUserFieldsMulti() {
        IJSONEventLayout layout = getJsonEventLayout();
        String prevUserData = layout.getUserFields();
        layout.setUserFields(userFieldsMulti);

        logger.info("this is an info message with user fields");
        String message = appender.getMessages()[0];
        Assert.assertTrue("Event is not valid JSON", JSONValue.isValidJsonStrict(message));
        Object obj = JSONValue.parse(message);
        JSONObject jsonObject = (JSONObject) obj;
        Assert.assertTrue("Event does not contain field 'field2'", jsonObject.containsKey("field2"));
        Assert.assertEquals("Event does not contain value 'value2'", "value2", jsonObject.get("field2"));
        Assert.assertTrue("Event does not contain field 'field3'", jsonObject.containsKey("field3"));
        Assert.assertEquals("Event does not contain value 'value3'", "value3", jsonObject.get("field3"));

        layout.setUserFields(prevUserData);
    }

    @Test
    public void testJSONEventLayoutUserFieldsPropOverride() {
        String additionalDataProperty = JSONEventLayoutV1.ADDITIONAL_DATA_PROPERTY;
        if (appender.getLayout() instanceof JSONEventLayoutV2) {
            JSONEventLayoutV2 layout = (JSONEventLayoutV2) appender.getLayout();
            additionalDataProperty = layout.ADDITIONAL_DATA_PROPERTY;
        }
        // set the property first
        System.setProperty(additionalDataProperty, userFieldsSingleProperty);

        // set the config values
        IJSONEventLayout layout = getJsonEventLayout();
        String prevUserData = layout.getUserFields();
        layout.setUserFields(userFieldsSingle);

        logger.info("this is an info message with user fields");
        String message = appender.getMessages()[0];
        Assert.assertTrue("Event is not valid JSON", JSONValue.isValidJsonStrict(message));
        Object obj = JSONValue.parse(message);
        JSONObject jsonObject = (JSONObject) obj;
        Assert.assertTrue("Event does not contain field 'field1'", jsonObject.containsKey("field1"));
        Assert.assertEquals("Event does not contain value 'propval1'", "propval1", jsonObject.get("field1"));

        layout.setUserFields(prevUserData);
        System.clearProperty(additionalDataProperty);

    }

    @Test
    public void testJSONEventLayoutHasKeys() {
        logger.info("this is a test message");
        String message = appender.getMessages()[0];
        Object obj = JSONValue.parse(message);
        JSONObject jsonObject = (JSONObject) obj;

        List<String> fieldNames = Arrays.asList(logstashFields);
        if (appender.getLayout() instanceof JSONEventLayoutV2) {
            JSONEventLayoutV2 layout = (JSONEventLayoutV2) appender.getLayout();
            LogstashCommonFieldNames commonFieldNames = layout.getFieldNames();
            fieldNames = commonFieldNames.listCommonNames();
        }

        for (String fieldName : fieldNames) {
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
    public void testJSONEventLayoutHasMDC() {

        MDC.put("foo", "bar");
        logger.warn("I should have MDC data in my log");
        String message = appender.getMessages()[0];
        Object obj = JSONValue.parse(message);
        JSONObject jsonObject = (JSONObject) obj;

        if (appender.getLayout() instanceof JSONEventLayoutV2) {
            //flattened by default
            Assert.assertEquals("MDC is wrong", "bar", jsonObject.get("foo"));
        } else {
            JSONObject mdc = (JSONObject) jsonObject.get("mdc");
            Assert.assertEquals("MDC is wrong", "bar", mdc.get("foo"));
        }
    }

    @Test
    public void testJSONEventLayoutHasNestedMDC() {
        HashMap nestedMdc = new HashMap<String, String>();
        nestedMdc.put("bar", "baz");
        MDC.put("foo", nestedMdc);
        logger.warn("I should have nested MDC data in my log");
        String message = appender.getMessages()[0];
        Object obj = JSONValue.parse(message);
        JSONObject jsonObject = (JSONObject) obj;

        if (appender.getLayout() instanceof JSONEventLayoutV2) {
            //flattened by default
            Assert.assertTrue("Event is missing foo key", jsonObject.containsKey("foo"));
            JSONObject nested = (JSONObject) jsonObject.get("foo");
            Assert.assertEquals("Nested MDC data is wrong", "baz", nested.get("bar"));

        } else {

            JSONObject mdc = (JSONObject) jsonObject.get("mdc");
            JSONObject nested = (JSONObject) mdc.get("foo");
            Assert.assertTrue("Event is missing foo key", mdc.containsKey("foo"));
            Assert.assertEquals("Nested MDC data is wrong", "baz", nested.get("bar"));
        }
    }

    @Test
    public void testJSONEventLayoutExceptions() {
        String exceptionMessage = new String("shits on fire, yo");
        logger.fatal("uh-oh", new IllegalArgumentException(exceptionMessage));
        String message = appender.getMessages()[0];
        Object obj = JSONValue.parse(message);
        JSONObject jsonObject = (JSONObject) obj;

        if (appender.getLayout() instanceof JSONEventLayoutV2) {
            //flattened
            JSONEventLayoutV2 layout = (JSONEventLayoutV2) appender.getLayout();
            Assert.assertEquals("Exception class missing", "java.lang.IllegalArgumentException", jsonObject.get(layout.getFieldNames().getExceptionClass()));
            Assert.assertEquals("Exception exception message", exceptionMessage, jsonObject.get(layout.getFieldNames().getExceptionMessage()));
        } else {
            JSONObject exceptionInformation = (JSONObject) jsonObject.get("exception");
            Assert.assertEquals("Exception class missing", "java.lang.IllegalArgumentException", exceptionInformation.get("exception_class"));
            Assert.assertEquals("Exception exception message", exceptionMessage, exceptionInformation.get("exception_message"));
        }
    }

    @Test
    public void testJSONEventLayoutHasClassName() {
        logger.warn("warning dawg");
        String message = appender.getMessages()[0];
        Object obj = JSONValue.parse(message);
        JSONObject jsonObject = (JSONObject) obj;

        String nameOfValueToGet = "class";
        if (appender.getLayout() instanceof JSONEventLayoutV2) {
            JSONEventLayoutV2 layout = (JSONEventLayoutV2) appender.getLayout();
            nameOfValueToGet = layout.getFieldNames().getCallerClass();
        }

        Assert.assertEquals("Logged class does not match", this.getClass().getCanonicalName().toString(), jsonObject.get(nameOfValueToGet));
    }

    @Test
    public void testJSONEventHasFileName() {
        logger.warn("whoami");
        String message = appender.getMessages()[0];
        Object obj = JSONValue.parse(message);
        JSONObject jsonObject = (JSONObject) obj;

        String nameOfValueToGet = "file";
        if (appender.getLayout() instanceof JSONEventLayoutV2) {
            JSONEventLayoutV2 layout = (JSONEventLayoutV2) appender.getLayout();
            nameOfValueToGet = layout.getFieldNames().getCallerFile();
        }

        Assert.assertNotNull("File value is missing", jsonObject.get(nameOfValueToGet));
    }


    @Test
    public void testJSONEventHasLoggerName() {
        logger.warn("whoami");
        String message = appender.getMessages()[0];
        Object obj = JSONValue.parse(message);
        JSONObject jsonObject = (JSONObject) obj;

        String nameOfValueToGet = "logger_name";
        if (appender.getLayout() instanceof JSONEventLayoutV2) {
            JSONEventLayoutV2 layout = (JSONEventLayoutV2) appender.getLayout();
            nameOfValueToGet = layout.getFieldNames().getLogger();
        }

        Assert.assertNotNull("LoggerName value is missing", jsonObject.get(nameOfValueToGet));
    }

    @Test
    public void testJSONEventHasThreadName() {
        logger.warn("whoami");
        String message = appender.getMessages()[0];
        Object obj = JSONValue.parse(message);
        JSONObject jsonObject = (JSONObject) obj;

        String nameOfValueToGet = "thread_name";
        if (appender.getLayout() instanceof JSONEventLayoutV2) {
            JSONEventLayoutV2 layout = (JSONEventLayoutV2) appender.getLayout();
            nameOfValueToGet = layout.getFieldNames().getLogger();
        }

        Assert.assertNotNull("ThreadName value is missing", jsonObject.get(nameOfValueToGet));
    }

    @Test
    public void testJSONEventLayoutNoLocationInfo() {
        IJSONEventLayout layout = getJsonEventLayout();
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
        IJSONEventLayout layout = getJsonEventLayout();
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
        Assert.assertEquals("format does not produce expected output", "2013-04-01T19:36:31.207Z", JSONEventLayoutV2.dateFormat(timestamp));
    }

    protected IJSONEventLayout getJsonEventLayout() {
        return (IJSONEventLayout) appender.getLayout();
    }

}
