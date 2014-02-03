package net.logstash.log4j;

import net.logstash.log4j.data.HostData;
import net.minidev.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.apache.log4j.Layout;
import org.apache.log4j.spi.LocationInfo;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

public class JSONEventLayoutV0 extends Layout {

    private boolean renderObjectFields = false;
    private boolean locationInfo = false;

    private String tags;
    private boolean ignoreThrowable = false;

    private boolean activeIgnoreThrowable = ignoreThrowable;
    private String hostname = new HostData().getHostName();
    private String threadName;
    private long timestamp;
    private String ndc;
    private Map mdc;
    private LocationInfo info;
    private HashMap<String, Object> fieldData;
    private HashMap<String, Object> exceptionInformation;

    private JSONObject logstashEvent;

    public static final TimeZone UTC = TimeZone.getTimeZone("UTC");
    public static final FastDateFormat ISO_DATETIME_TIME_ZONE_FORMAT_WITH_MILLIS = FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", UTC);

    public static String dateFormat(long timestamp) {
        return ISO_DATETIME_TIME_ZONE_FORMAT_WITH_MILLIS.format(timestamp);
    }

    /**
     * For backwards compatibility, the default is to generate location information
     * in the log messages.
     */
    public JSONEventLayoutV0() {
        this(true, false);
    }

    /**
     * Creates a layout that optionally inserts location information into log messages.
     *
     * @param locationInfo whether or not to include location information in the log messages.
     * @param renderObjectFields whether or not to render the fields of the message object into the json when an object is logged to log4j.
     *                     Rendering the fields is done using reflection and incurs a performance cost
     */
    public JSONEventLayoutV0(boolean locationInfo, boolean renderObjectFields) {
        this.locationInfo = locationInfo;
        this.renderObjectFields = renderObjectFields;
    }

    public String format(LoggingEvent loggingEvent) {
        threadName = loggingEvent.getThreadName();
        timestamp = loggingEvent.getTimeStamp();
        fieldData = new HashMap<String, Object>();
        exceptionInformation = new HashMap<String, Object>();
        mdc = loggingEvent.getProperties();
        ndc = loggingEvent.getNDC();

        logstashEvent = new JSONObject();

        logstashEvent.put("@source_host", hostname);
        logstashEvent.put("@message", loggingEvent.getRenderedMessage());
        logstashEvent.put("@timestamp", dateFormat(timestamp));

        if (renderObjectFields) {
            Object messageObj = loggingEvent.getMessage();
            if (messageObj instanceof Serializable && !(messageObj instanceof String)) {
                addObjectFieldData(messageObj);
            }
        }

        if (loggingEvent.getThrowableInformation() != null) {
            final ThrowableInformation throwableInformation = loggingEvent.getThrowableInformation();
            if (throwableInformation.getThrowable().getClass().getCanonicalName() != null) {
                exceptionInformation.put("exception_class", throwableInformation.getThrowable().getClass().getCanonicalName());
            }
            if (throwableInformation.getThrowable().getMessage() != null) {
                exceptionInformation.put("exception_message", throwableInformation.getThrowable().getMessage());
            }
            if (throwableInformation.getThrowableStrRep() != null) {
                String stackTrace = StringUtils.join(throwableInformation.getThrowableStrRep(), "\n");
                exceptionInformation.put("stacktrace", stackTrace);
            }
            addFieldData("exception", exceptionInformation);
        }

        if (locationInfo) {
            info = loggingEvent.getLocationInformation();
            addFieldData("file", info.getFileName());
            addFieldData("line_number", info.getLineNumber());
            addFieldData("class", info.getClassName());
            addFieldData("method", info.getMethodName());
        }

        addFieldData("loggerName", loggingEvent.getLoggerName());
        addFieldData("mdc", mdc);
        addFieldData("ndc", ndc);
        addFieldData("level", loggingEvent.getLevel().toString());
        addFieldData("threadName", threadName);

        logstashEvent.put("@fields", fieldData);
        return logstashEvent.toString() + "\n";
    }

    private void addObjectFieldData(Object messageObj) {
        Field[] fields = messageObj.getClass().getFields();
        Object value = null;

        for(Field f : fields) {
            try {
                value = f.get(messageObj);
                if (value != null) fieldData.put(f.getName(), value);
            } catch (IllegalAccessException e) {
            }
        }
        Method[] methods = messageObj.getClass().getMethods();
        for(Method m : methods)
        {
            if(m.getName().startsWith("get"))
            {
                try {
                    value = m.invoke(messageObj);
                } catch (IllegalAccessException e) {
                } catch (InvocationTargetException e) {
                }
                if (value != null) fieldData.put(m.getName().substring(3), value);
            }
        }
    }

    public boolean ignoresThrowable() {
        return ignoreThrowable;
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

    /**
     * Set whether or not to render the fields of the message object into the json when an object is logged to log4j.
     * Rendering the fields is done using reflection and incurs a performance cost
     * @param renderObjectFields
     */
    public void setRenderObjectFields(boolean renderObjectFields) {
        this.renderObjectFields = renderObjectFields;
    }
    
    public void activateOptions() {
        activeIgnoreThrowable = ignoreThrowable;
    }

    private void addFieldData(String keyname, Object keyval) {
        if (null != keyval) {
            fieldData.put(keyname, keyval);
        }
    }
}
