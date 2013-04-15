package net.logstash.log4j;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import net.minidev.json.JSONObject;
import org.apache.log4j.Layout;
import org.apache.log4j.spi.LocationInfo;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;

public class JSONEventLayout extends Layout {

    private boolean locationInfo = false;
    private boolean ignoreThrowable = false;
    private boolean activeIgnoreThrowable = ignoreThrowable;
    private long timestamp;
    private String ndc;
    private Map mdc;
    private LocationInfo info;
    private HashMap<String, Object> fieldData;
    private HashMap<String, Object> exceptionInformation;
    private JSONObject logstashEvent;
    private static String hostname;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZ");

    static {
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            hostname = "unknown-host";
        }
    }

    public static String dateFormat(long timestamp) {
        return DATE_FORMAT.format(new Date(timestamp));
    }

    /**
     * For backwards compatibility, the default is to generate location information in the log messages.
     */
    public JSONEventLayout() {
        this(true);
    }

    /**
     * Creates a layout that optionally inserts location information into log messages.
     *
     * @param locationInfo whether or not to include location information in the log messages.
     */
    public JSONEventLayout(boolean locationInfo) {
        this.locationInfo = locationInfo;
    }

    /**
     * {@inheritDoc}
     */
    public String format(LoggingEvent loggingEvent) {
        timestamp = loggingEvent.getTimeStamp();
        fieldData = new HashMap<String, Object>();
        exceptionInformation = new HashMap<String, Object>();
        mdc = loggingEvent.getProperties();
        ndc = loggingEvent.getNDC();

        logstashEvent = new JSONObject();

        logstashEvent.put("@source_host", hostname);
        logstashEvent.put("@message", loggingEvent.getRenderedMessage());
        logstashEvent.put("@timestamp", dateFormat(timestamp));

        if (loggingEvent.getThrowableInformation() != null) {
            final ThrowableInformation throwableInformation = loggingEvent.getThrowableInformation();
            final Throwable throwable = throwableInformation.getThrowable();
            if (throwable.getClass().getCanonicalName() != null) {
                exceptionInformation.put("exception_class", throwable.getClass().getCanonicalName());
            }
            if (throwableInformation.getThrowable().getMessage() != null) {
                exceptionInformation.put("exception_message", throwable.getMessage());
            }
            if (throwableInformation.getThrowableStrRep() != null) {
                StringBuilder stackTrace = new StringBuilder();
                if (throwableInformation.getThrowableStrRep().length > 0) {
                    String[] traces = throwableInformation.getThrowableStrRep();
                    for (String trace : traces) {
                        stackTrace.append(trace).append("\n");
                    }
                }
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

        addFieldData(
                "mdc", mdc);
        addFieldData(
                "ndc", ndc);
        addFieldData(
                "level", loggingEvent.getLevel().toString());

        logstashEvent.put(
                "@fields", fieldData);
        return logstashEvent.toString()
                + "\n";
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

    public void activateOptions() {
        activeIgnoreThrowable = ignoreThrowable;
    }

    private void addFieldData(String keyname, Object keyval) {
        if (null != keyval) {
            fieldData.put(keyname, keyval);
        }
    }
}
