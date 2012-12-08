package net.logstash.log4j;

import net.logstash.log4j.data.HostData;

import java.util.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import net.minidev.json.JSONObject;
import org.apache.commons.lang.*;
import org.apache.log4j.Layout;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;
import org.apache.log4j.spi.LocationInfo;

public class JSONEventLayout extends Layout {

    private String tags;
    private boolean ignoreThrowable = false;

    private boolean activeIgnoreThrowable = ignoreThrowable;
    private String hostname;
    private long timestamp;
    private String ndc;
    private Map mdc;
    private LocationInfo info;
    private HashMap<String, Object> fieldData;
    private HashMap<String, Object> exceptionInformation;

    private JSONObject logstashEvent;

    public static String dateFormat(long timestamp) {
	Date date = new Date(timestamp);
	/*
	 * SimpleDateFormat isn't thread safe so I need one 
	 * instance per call, otherwise I'd have to pull in
	 * joda time.
	 */
	SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
	String formatted = format.format(date);

	/* 
	 * No native support for ISO8601 woo!
	 */
	return formatted.substring(0,22) + ":" + formatted.substring(22);
    }

    public String format(LoggingEvent loggingEvent) {
        hostname = new HostData().getHostName();
        timestamp = loggingEvent.getTimeStamp();
        info = loggingEvent.getLocationInformation();
        fieldData = new HashMap<String, Object>();
        exceptionInformation = new HashMap<String, Object>();
        mdc = loggingEvent.getProperties();
        ndc = loggingEvent.getNDC();

        logstashEvent = new JSONObject();

        logstashEvent.put("@source_host",hostname);
        logstashEvent.put("@message",loggingEvent.getRenderedMessage());

        if(loggingEvent.getThrowableInformation() != null) {
            final ThrowableInformation throwableInformation = loggingEvent.getThrowableInformation();
            if(throwableInformation.getThrowable().getClass().getCanonicalName() != null){
                exceptionInformation.put("exception_class",throwableInformation.getThrowable().getClass().getCanonicalName());
            }
            if(throwableInformation.getThrowable().getMessage() != null) {
                exceptionInformation.put("exception_message",throwableInformation.getThrowable().getMessage());
            }
            if( throwableInformation.getThrowableStrRep() != null) {
                String stackTrace = StringUtils.join(throwableInformation.getThrowableStrRep(),"\n");
                exceptionInformation.put("stacktrace",stackTrace);
            }
            addFieldData("exception",exceptionInformation);
        }


        if(loggingEvent.locationInformationExists()) {
            info = loggingEvent.getLocationInformation();
            addFieldData("file",info.getFileName());
            addFieldData("line_number",info.getLineNumber());
            addFieldData("class",info.getClassName());
            addFieldData("method",info.getMethodName());
        }

        addFieldData("mdc",mdc);
        addFieldData("ndc",ndc);
        addFieldData("level",loggingEvent.getLevel().toString());
        addFieldData("timestamp",dateFormat(timestamp));

        logstashEvent.put("@fields",fieldData);
        return logstashEvent.toString() + "\n";
    }

    public boolean ignoresThrowable() {
        return ignoreThrowable;
    }

    public void activateOptions() {
        activeIgnoreThrowable = ignoreThrowable;
    }

    private void addFieldData(String keyname, Object keyval){
        if(null != keyval){
            fieldData.put(keyname, keyval);
        }
    }
}
