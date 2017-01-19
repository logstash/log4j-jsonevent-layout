package net.logstash.log4j;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.logstash.log4j.data.HostData;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.apache.log4j.Layout;
import org.apache.log4j.helpers.LogLog;
import org.apache.log4j.spi.LocationInfo;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;

import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

public class JSONEventLayoutV1 extends Layout {

    private boolean locationInfo = false;
    private String customUserFields;

    private final String hostname = new HostData().getHostName();
    private static final int VERSION = 1;

    private static final ObjectMapper JSON_SERIALIZER = new ObjectMapper();
    private Map<String, Object> logstashEvent;

    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");
    private static final FastDateFormat ISO_DATETIME_TIME_ZONE_FORMAT_WITH_MILLIS = FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", UTC);
    public static final String ADDITIONAL_DATA_PROPERTY = "net.logstash.log4j.JSONEventLayoutV1.UserFields";

    public static String dateFormat(long timestamp) {
        return ISO_DATETIME_TIME_ZONE_FORMAT_WITH_MILLIS.format(timestamp);
    }

    /**
     * For backwards compatibility, the default is to generate location information
     * in the log messages.
     */
    public JSONEventLayoutV1() {
        this(true);
    }

    /**
     * Creates a layout that optionally inserts location information into log messages.
     *
     * @param locationInfo whether or not to include location information in the log messages.
     */
    public JSONEventLayoutV1(boolean locationInfo) {
        this.locationInfo = locationInfo;
    }

    @Override
    public String format(LoggingEvent loggingEvent) {
        String threadName = loggingEvent.getThreadName();
        long timestamp = loggingEvent.getTimeStamp();
        HashMap<String, Object> exceptionInformation = new HashMap<String, Object>();
        Map mdc = loggingEvent.getProperties();
        String ndc = loggingEvent.getNDC();

        logstashEvent = new HashMap<String, Object>();
        String whoami = this.getClass().getSimpleName();

        /*
         * All v1 of the event format requires is
         * "@timestamp" and "@version"
         * Every other field is arbitrary
         */
        logstashEvent.put("@version", VERSION);
        logstashEvent.put("@timestamp", dateFormat(timestamp));

        /*
         * Extract and add fields from log4j config, if defined
         */
        if (getUserFields() != null) {
            String userFields = getUserFields();
            LogLog.debug(String.format("[%s] Got user data from log4j property: %s", whoami, userFields));
            addUserFields(userFields);
        }

        /*
         * Extract fields from system properties, if defined
         * Note that CLI props will override conflicts with log4j config
         */
        String additionalDataSystemProperty = System.getProperty(ADDITIONAL_DATA_PROPERTY);
        if (additionalDataSystemProperty != null) {
            if (getUserFields() != null) {
                LogLog.warn(String.format("[%s] Loading UserFields from command-line. This will override any UserFields set in the log4j configuration file", whoami));
            }
            LogLog.debug(String.format("[%s] Got user data from system property: %s", whoami, additionalDataSystemProperty));
            addUserFields(additionalDataSystemProperty);
        }

        /*
         * Now we start injecting our own stuff.
         */
        logstashEvent.put("source_host", hostname);
        logstashEvent.put("message", loggingEvent.getRenderedMessage());

        final ThrowableInformation throwableInformation = loggingEvent.getThrowableInformation();
        if (throwableInformation != null) {
            Throwable throwable = throwableInformation.getThrowable();
            String className = throwable.getClass().getCanonicalName();
            if (className != null) {
                exceptionInformation.put("exception_class", className);
            }
            String message = throwable.getMessage();
            if (message != null) {
                exceptionInformation.put("exception_message", message);
            }
            String[] throwableStrRep = throwableInformation.getThrowableStrRep();
            if (throwableStrRep != null) {
                String stackTrace = StringUtils.join(throwableStrRep, "\n");
                exceptionInformation.put("stacktrace", stackTrace);
            }
            addEventData("exception", exceptionInformation);
        }

        if (locationInfo) {
            LocationInfo info = loggingEvent.getLocationInformation();
            addEventData("file", info.getFileName());
            addEventData("line_number", info.getLineNumber());
            addEventData("class", info.getClassName());
            addEventData("method", info.getMethodName());
        }

        addEventData("logger_name", loggingEvent.getLoggerName());
        addEventData("mdc", mdc);
        addEventData("ndc", ndc);
        addEventData("level", loggingEvent.getLevel().toString());
        addEventData("thread_name", threadName);

        try {
            return JSON_SERIALIZER.writeValueAsString(logstashEvent) + "\n";
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean ignoresThrowable() {
        return false;
    }

    /**
     * Query whether log messages include location information.
     *
     * @return true if location information is included in log messages, false otherwise.
     */
    public boolean getLocationInfo() {
        return locationInfo;
    }

    /**
     * Set whether log messages should include location information.
     *
     * @param locationInfo true if location information should be included, false otherwise.
     */
    public void setLocationInfo(boolean locationInfo) {
        this.locationInfo = locationInfo;
    }

    public String getUserFields() { return customUserFields; }
    public void setUserFields(String userFields) { this.customUserFields = userFields; }

    public void activateOptions() {
    }

    private void addUserFields(String data) {
        if (null != data) {
            String[] pairs = data.split(",");
            for (String pair : pairs) {
                String[] userField = pair.split(":", 2);
                if (userField.length > 1 && userField[0] != null) {
                    String key = userField[0];
                    String val = userField[1];
                    addEventData(key, val);
                }
            }
        }
    }
    private void addEventData(String keyname, Object keyval) {
        if (null != keyval) {
            logstashEvent.put(keyname, keyval);
        }
    }
}
