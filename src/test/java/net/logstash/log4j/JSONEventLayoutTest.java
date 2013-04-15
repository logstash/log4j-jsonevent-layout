package net.logstash.log4j;

import java.io.StringReader;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonReaderFactory;
import junit.framework.Assert;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.NDC;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Test to ensure json event layouter for logstash is working correctly.
 */
public class JSONEventLayoutTest {

    private static Logger logger;
    private static JsonReaderFactory parser = Json.createReaderFactory(null);
    private static MockAppender appender;
    private static final String[] LOGSTASHFIELDS = new String[]{
        "@message",
        "@source_host",
        "@fields",
        "@timestamp"
    };
    public static final long TIMESTAMP = 1364844991207L;

    /**
     * Setup MockAppender.
     */
    @BeforeClass
    public static void setupTestAppender() {
        appender = new MockAppender(new JSONEventLayout());
        logger = Logger.getRootLogger();
        appender.setThreshold(Level.TRACE);
        appender.setName("mockappender");
        appender.activateOptions();
        logger.addAppender(appender);
    }

    /**
     * Clear MockAppender.
     */
    @After
    public void clearTestAppender() {
        NDC.clear();
        appender.clear();
        appender.close();
    }

    /**
     * Test if JSON API produces correct JSON.
     */
    @Test
    public void testJSONEventLayoutIsJSON() {
        logger.info("this is an info message");
        String message = appender.getMessages()[0];
        try {
            JsonObject jsonObject = createObject(message);
            Assert.assertNotNull(jsonObject);
        } catch (Exception e) {
            Assert.fail("No valid json data.");
        }
    }

    /**
     * Test if log messages contains key.
     */
    @Test
    public void testJSONEventLayoutHasKeys() {
        logger.info("this is a test message");
        String message = appender.getMessages()[0];
        JsonObject jsonObject = createObject(message);

        for (String fieldName : LOGSTASHFIELDS) {
            Assert.assertTrue("Event does not contain field:" + fieldName, jsonObject.containsKey(fieldName));
        }
    }

    /**
     * Test if log message has field named level.
     */
    @Test
    public void testJSONEventLayoutHasFieldLevel() {
        logger.fatal("this is a new test message");
        String message = appender.getMessages()[0];
        JsonObject jsonObject = createObject(message);
        JsonObject atFields = jsonObject.getJsonObject("@fields");

        Assert.assertEquals("Log level is wrong", "FATAL", atFields.getString("level"));
    }

    /**
     * Test if log message has a NDC value.
     */
    @Test
    public void testJSONEventLayoutHasNDC() {
        String ndcData = "json-layout-test";
        NDC.push(ndcData);
        logger.warn("I should have NDC data in my log");
        String message = appender.getMessages()[0];

        JsonObject jsonObject = createObject(message);
        JsonObject atFields = jsonObject.getJsonObject("@fields");

        Assert.assertEquals("NDC is wrong", ndcData, atFields.getString("ndc"));
    }

    /**
     * Test if log messages contains an exception.
     */
    @Test
    public void testJSONEventLayoutExceptions() {
        String exceptionMessage = "shits on fire, yo";
        logger.fatal("uh-oh", new IllegalArgumentException(exceptionMessage));
        String message = appender.getMessages()[0];
        JsonObject jsonObject = createObject(message);
        JsonObject atFields = jsonObject.getJsonObject("@fields");
        JsonObject exceptionInformation = atFields.getJsonObject("exception");

        Assert.assertEquals(
                "Exception class missing",
                "java.lang.IllegalArgumentException",
                exceptionInformation.getString("exception_class"));
        Assert.assertEquals(
                "Exception exception message",
                exceptionMessage,
                exceptionInformation.getString("exception_message"));
    }

    /**
     * Test if log messages has a class name.
     */
    @Test
    public void testJSONEventLayoutHasClassName() {
        logger.warn("warning dawg");
        String message = appender.getMessages()[0];
        JsonObject jsonObject = createObject(message);
        JsonObject atFields = jsonObject.getJsonObject("@fields");

        Assert.assertEquals(
                "Logged class does not match",
                this.getClass().getCanonicalName().toString(),
                atFields.getString("class"));
    }

    /**
     * Test if log message has a file name.
     */
    @Test
    public void testJSONEventHasFileName() {
        logger.warn("whoami");
        String message = appender.getMessages()[0];
        JsonObject jsonObject = createObject(message);
        JsonObject atFields = jsonObject.getJsonObject("@fields");

        Assert.assertNotNull("File value is missing", atFields.getString("file"));
    }

    /**
     * Test if log message will not contain any location info if parameter is set.
     */
    @Test
    public void testJSONEventLayoutNoLocationInfo() {
        JSONEventLayout layout = (JSONEventLayout) appender.getLayout();
        boolean prevLocationInfo = layout.getLocationInfo();

        layout.setLocationInfo(false);

        logger.warn("warning dawg");
        String message = appender.getMessages()[0];
        JsonObject jsonObject = createObject(message);
        JsonObject atFields = jsonObject.getJsonObject("@fields");

        Assert.assertFalse("atFields contains file value", atFields.containsKey("file"));
        Assert.assertFalse("atFields contains line_number value", atFields.containsKey("line_number"));
        Assert.assertFalse("atFields contains class value", atFields.containsKey("class"));
        Assert.assertFalse("atFields contains method value", atFields.containsKey("method"));

        // Revert the change to the layout to leave it as we found it.
        layout.setLocationInfo(prevLocationInfo);
    }

    /**
     * Messure performance.
     */
    @Test
    @Ignore
    public void measureJSONEventLayoutLocationInfoPerformance() {
        JSONEventLayout layout = (JSONEventLayout) appender.getLayout();
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

    /**
     * Test date formating.
     */
    @Test
    @Ignore
    public void testDateFormat() {
        long timestamp = TIMESTAMP;
        Assert.assertEquals(
                "format does not produce expected output",
                "2013-04-01T21:36:31.207+02:00",
                JSONEventLayout.dateFormat(timestamp));
    }

    private JsonObject createObject(String input) {

        System.out.println(input);

        JsonReader reader = parser.createReader(new StringReader(input));
        return reader.readObject();
    }
}
