package net.logstash.log4j;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObjectBuilder;
import org.apache.log4j.Layout;
import org.apache.log4j.spi.LocationInfo;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;

/**
 * Class to encode log4j events into logstash json event format.
 */
public class JSONEventLayout extends Layout {

    private boolean locationInfo = false;
    private boolean ignoreThrowable = false;
    private boolean activeIgnoreThrowable = ignoreThrowable;
    private static String hostname;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZ");
    private static final JsonBuilderFactory BUILDER = Json.createBuilderFactory(null);

    static {
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            hostname = "unknown-host";
        }
    }

    /**
     * Create a iso timestamp.
     *
     * @param timestamp the current timestamp in milliseconds
     * @return the timestamp format as "yyyy-MM-dd'T'HH:mm:ss.SSSZZ"
     */
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
        long timestamp = loggingEvent.getTimeStamp();
        JsonObjectBuilder builder = BUILDER.createObjectBuilder();
        builder.add("@source_host", hostname);
        builder.add("@message", loggingEvent.getRenderedMessage());
        builder.add("@timestamp", dateFormat(timestamp));
        builder.add(
                "@fields", encodeFields(loggingEvent));
        return builder.build().toString()
                + "\n";
    }

    /**
     * Eoncode fields to json event section.
     *
     * @param loggingEvent the source
     * @return the eoncoded field section
     */
    protected JsonObjectBuilder encodeFields(LoggingEvent loggingEvent) {
        JsonObjectBuilder builder = BUILDER.createObjectBuilder();
        if (locationInfo) {
            LocationInfo info = loggingEvent.getLocationInformation();
            builder.add("file", info.getFileName());
            builder.add("line_number", info.getLineNumber());
            builder.add("class", info.getClassName());
            builder.add("method", info.getMethodName());
        }
        builder.add(
                "mdc", encodeMap(loggingEvent.getProperties()));
        if (loggingEvent.getNDC() != null) {
            builder.add("ndc", loggingEvent.getNDC());
        } else {
            builder.add("ndc", "");
        }
        builder.add("level", loggingEvent.getLevel().toString());
        builder.add("exception", encodeException(loggingEvent));
        return builder;
    }

    protected JsonObjectBuilder encodeException(LoggingEvent loggingEvent) {
        JsonObjectBuilder builder = BUILDER.createObjectBuilder();
        if (loggingEvent.getThrowableInformation() != null) {
            final ThrowableInformation throwableInformation = loggingEvent.getThrowableInformation();
            final Throwable throwable = throwableInformation.getThrowable();
            if (throwable.getClass().getCanonicalName() != null) {
                builder.add("exception_class", throwable.getClass().getCanonicalName());
            }
            if (throwableInformation.getThrowable().getMessage() != null) {
                builder.add("exception_message", throwable.getMessage());
            }
            if (throwableInformation.getThrowableStrRep() != null) {
                StringBuilder stackTrace = new StringBuilder();
                if (throwableInformation.getThrowableStrRep().length > 0) {
                    String[] traces = throwableInformation.getThrowableStrRep();
                    for (String trace : traces) {
                        stackTrace.append(trace).append("\n");
                    }
                }
                builder.add("stacktrace", stackTrace.toString());
            }
        }
        return builder;
    }

    /**
     * Convert a map into a json object.
     *
     * @param map the map to convert
     * @return a json object
     */
    protected JsonObjectBuilder encodeMap(Map<String, Object> map) {
        JsonObjectBuilder builder = BUILDER.createObjectBuilder();
        for (Entry<String, Object> entry : map.entrySet()) {
            builder.add(entry.getKey(), entry.getValue().toString());
        }
        return builder;
    }

    /**
     * {@inheritDoc}
     */
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
     * {@inheritDoc}
     */
    public void activateOptions() {
        activeIgnoreThrowable = ignoreThrowable;
    }
}
