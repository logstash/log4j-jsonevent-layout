package net.logstash.log4j;


import net.logstash.log4j.data.HostData;
import net.logstash.log4j.fieldnames.LogstashFieldNames;
import net.minidev.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.apache.log4j.Layout;
import org.apache.log4j.helpers.LogLog;
import org.apache.log4j.helpers.OnlyOnceErrorHandler;
import org.apache.log4j.spi.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;


/**
 * Log4j JSON Layout providing mutable output field names
 * <p/>
 * Based upon the similar field name solution for logback found here
 * https://github.com/logstash/logstash-logback-encoder
 * <p/>
 * Also allows for "flattening" of the output structure, removing any nested structures
 */
public class JSONEventLayoutV2 extends Layout implements IJSONEventLayout {

    protected ErrorHandler errorHandler = new OnlyOnceErrorHandler();

    private LogstashFieldNames fieldNames = new LogstashFieldNames();
    private boolean locationInfo = true;
    private String customUserFields;
    private boolean ignoreThrowable = false;
    private String hostname = new HostData().getHostName();
    private static Integer version = 1;

    public static final TimeZone UTC = TimeZone.getTimeZone("UTC");
    public static final FastDateFormat ISO_DATETIME_TIME_ZONE_FORMAT_WITH_MILLIS = FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", UTC);
    public static final String ADDITIONAL_DATA_PROPERTY = "net.logstash.log4j.JSONEventLayoutV2.UserFields";

    public static String dateFormat(long timestamp) {
        return ISO_DATETIME_TIME_ZONE_FORMAT_WITH_MILLIS.format(timestamp);
    }

    public JSONEventLayoutV2() {
        this(true);
    }

    public JSONEventLayoutV2(boolean isLocationInfo) {
        locationInfo = isLocationInfo;
    }

    public String format(LoggingEvent loggingEvent) {
        JSONObject lsEvent = createLogstashEvent(loggingEvent);

        return lsEvent.toString() + "\n";
    }

    protected JSONObject createLogstashEvent(LoggingEvent loggingEvent) {
        String threadName = loggingEvent.getThreadName();
        Long timestamp = loggingEvent.getTimeStamp();

        Map mdc = loggingEvent.getProperties();
        String ndc = loggingEvent.getNDC();

        JSONObject logstashEvent = new JSONObject();
        String whoami = this.getClass().getSimpleName();

        /**
         * All v1 of the event format requires is
         * "@timestamp" and "@version"
         * Every other field is arbitrary
         */
        addEventData(logstashEvent, fieldNames.getVersion(), version);
        addEventData(logstashEvent, fieldNames.getTimestamp(), dateFormat(timestamp));

        /**
         * Extract and add fields from log4j config, if defined
         */
        if (getUserFields() != null) {
            String userFlds = getUserFields();
            LogLog.debug("[" + whoami + "] Got user data from log4j property: " + userFlds);
            addUserFields(logstashEvent, userFlds);
        }

        /**
         * Extract fields from system properties, if defined
         * Note that CLI props will override conflicts with log4j config
         */
        if (System.getProperty(ADDITIONAL_DATA_PROPERTY) != null) {
            if (getUserFields() != null) {
                LogLog.warn("[" + whoami + "] Loading UserFields from command-line. This will override any UserFields set in the log4j configuration file");
            }
            String userFieldsProperty = System.getProperty(ADDITIONAL_DATA_PROPERTY);
            LogLog.debug("[" + whoami + "] Got user data from system property: " + userFieldsProperty);
            addUserFields(logstashEvent, userFieldsProperty);
        }

        /**
         * Now we start injecting our own stuff.
         */
        addEventData(logstashEvent, fieldNames.getHostName(), hostname);
        addEventData(logstashEvent, fieldNames.getMessage(), loggingEvent.getRenderedMessage());

        if (loggingEvent.getThrowableInformation() != null) {
            final ThrowableInformation throwableInformation = loggingEvent.getThrowableInformation();

            HashMap<String, Object> exceptionInformation = new HashMap<String, Object>();
            if (throwableInformation.getThrowable().getClass().getCanonicalName() != null) {
                exceptionInformation.put(fieldNames.getExceptionClass(), throwableInformation.getThrowable().getClass().getCanonicalName());
            }
            if (throwableInformation.getThrowable().getMessage() != null) {
                exceptionInformation.put(fieldNames.getExceptionMessage(), throwableInformation.getThrowable().getMessage());
            }
            if (throwableInformation.getThrowableStrRep() != null) {
                String stackTrace = StringUtils.join(throwableInformation.getThrowableStrRep(), "\n");
                exceptionInformation.put(fieldNames.getStackTrace(), stackTrace);
            }
            if (fieldNames.getException() != null) {
                addEventData(logstashEvent, fieldNames.getException(), exceptionInformation);
            } else {
                addEventData(logstashEvent, exceptionInformation);
            }

        }

        if (getLocationInfo()) {
            LocationInfo info = loggingEvent.getLocationInformation();
            Map<String, String> locMap = new HashMap<String, String>();

            addEventData(locMap, fieldNames.getCallerFile(), info.getFileName());
            addEventData(locMap, fieldNames.getCallerLine(), info.getLineNumber());
            addEventData(locMap, fieldNames.getCallerClass(), info.getClassName());
            addEventData(locMap, fieldNames.getCallerMethod(), info.getMethodName());

            if (fieldNames.getCaller() != null) {
                addEventData(logstashEvent, fieldNames.getCaller(), locMap);
            } else {
                addEventData(logstashEvent, locMap);
            }

           /* addEventData(logstashEvent, fieldNames.getCallerFile(), info.getFileName());
            addEventData(logstashEvent, fieldNames.getCallerLine(), info.getLineNumber());
            addEventData(logstashEvent, fieldNames.getCallerClass(), info.getClassName());
            addEventData(logstashEvent, fieldNames.getCallerMethod(), info.getMethodName());*/
        }

        addEventData(logstashEvent, fieldNames.getLogger(), loggingEvent.getLoggerName());


        if (fieldNames.getMdc() != null) {
            addEventData(logstashEvent, fieldNames.getMdc(), mdc);

        } else {
            addEventData(logstashEvent, mdc);
        }


        addEventData(logstashEvent, fieldNames.getNdc(), ndc);
        addEventData(logstashEvent, fieldNames.getLevel(), loggingEvent.getLevel().toString());
        addEventData(logstashEvent, fieldNames.getThread(), threadName);

        return logstashEvent;
    }

    private void addEventData(JSONObject logstashEvent, Map map) {
        Set<Map.Entry> entries = map.entrySet();
        for (Map.Entry entry : entries) {
            String key = entry.getKey().toString();
            Object value = entry.getValue();
            addEventData(logstashEvent, key, value);
        }
    }

    private void addEventData(JSONObject logstashEvent, String keyName, Object keyVal) {
        if (keyVal != null && keyName != null) {
            logstashEvent.put(keyName, keyVal);
        }
    }

    private void addEventData(Map map, String keyName, Object keyVal) {
        if (keyVal != null && keyName != null) {
            map.put(keyName, keyVal);
        }
    }

    //TODO: This should be just using a JSON string instead of comma separated "name:value" pairs
    private void addUserFields(JSONObject logstashEvent, String data) {
        if (data != null) {
            String[] pairs = data.split(",");
            for (String pair : pairs) {
                String[] userField = pair.split(":", 2);
                if (userField[0] != null) {
                    String key = userField[0];
                    String val = userField[1];
                    addEventData(logstashEvent, key, val);
                }
            }
        }
    }


    public LogstashFieldNames getFieldNames() {
        return fieldNames;
    }

    public void setFieldNames(LogstashFieldNames fieldNames) {
        this.fieldNames = fieldNames;
    }

    public void setFieldsClassName(String fieldsClassName) {
        try {
            Class clazz = Class.forName(fieldsClassName);
            Object o = clazz.newInstance();
            if (o instanceof LogstashFieldNames) {
                setFieldNames((LogstashFieldNames) o);
            } else {
                errorHandler.error("Class for " + fieldsClassName + " is not a valid type for defining field names.  Will use default field names");
            }

        } catch (Exception e) {
            errorHandler.error("Failed to load class for FieldNames " + fieldsClassName, e, ErrorCode.GENERIC_FAILURE);
        }
    }

    public void setFlattenOutput(boolean isFlatten) {
        fieldNames.setFlattenOutput(isFlatten);
    }

    @Override
    public boolean ignoresThrowable() {
        return ignoreThrowable;
    }

    /**
     * Query whether log messages include location information.
     *
     * @return true if location information is included in log messages, false otherwise.
     */
    @Override
    public boolean getLocationInfo() {
        return locationInfo;
    }

    /**
     * Set whether log messages should include location information.
     *
     * @param locationInfo true if location information should be included, false otherwise.
     */
    @Override
    public void setLocationInfo(boolean locationInfo) {
        this.locationInfo = locationInfo;
    }

    @Override
    public String getUserFields() {
        return customUserFields;
    }

    @Override
    public void setUserFields(String userFields) {
        this.customUserFields = userFields;
    }

    public void activateOptions() {

        //activeIgnoreThrowable = ignoreThrowable;
    }
}
