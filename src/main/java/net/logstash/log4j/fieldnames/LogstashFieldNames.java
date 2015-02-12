/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.logstash.log4j.fieldnames;

import java.util.ArrayList;
import java.util.List;

/**
 * Names of standard fields that appear in the JSON output.
 *
 * Based upn the similar solution for logback
 * https://github.com/logstash/logstash-logback-encoder/blob/master/src/main/java/net/logstash/logback/fieldnames/LogstashFieldNames.java
 */
public class LogstashFieldNames extends LogstashCommonFieldNames {

    private String logger = "loggername";
    private String thread = "threadname";
    private String level = "level";
    //private String levelValue = "levelvalue";

    private String callerClass = "classname";
    private String callerMethod = "methodname";
    private String callerFile = "filename";
    private String callerLine = "linenumber";
    private String stackTrace = "stacktrace";
    private String tags = "tags";
    private String ndc = "ndc";

    private String hostName = "hostname";
    private String exceptionClass = "exceptionclass";
    private String exceptionMessage = "exceptionmessage";

    //IF we populate these, the output will create nested data for these names
    private String exception;
    private String caller;
    private String mdc;
    private String context;

    public static final String EXCEPTION_DEFAULT = "exception";
    public static final String CALLER_DEFAULT = "caller";
    public static final String MDC_DEFAULT = "mdc";
    public static final String CONTEXT_DEFAULT = "context";


    public void setFlattenOutput(Boolean isFlatten) {
        if (isFlatten) {
            setException(null);
            setCaller(null);
            setMdc(null);
            setContext(null);
        } else {
            String exception = getException() != null ? getException() : EXCEPTION_DEFAULT;
            String caller = getCaller() != null ? getCaller() : CALLER_DEFAULT;
            String mdc = getMdc() != null ? getMdc() : MDC_DEFAULT;
            String context = getContext() != null ? getContext() : CONTEXT_DEFAULT;
            setException(exception);
            setCaller(caller);
            setMdc(mdc);
            setContext(context);
        }
    }

    public String getLogger() {
        return logger;
    }

    public void setLogger(String logger) {
        this.logger = logger;
    }

    public String getThread() {
        return thread;
    }

    public void setThread(String thread) {
        this.thread = thread;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

   /**
    public String getLevelValue() {
        return levelValue;
    }

    public void setLevelValue(String levelValue) {
        this.levelValue = levelValue;
    }
    **/
    /**
     * The name of the caller object field.
     * <p/>
     * If this returns null, then the caller data fields will be written inline at the root level of the JSON event output (e.g. as a sibling to all the other fields in this class).
     * <p/>
     * If this returns non-null, then the caller data fields will be written inside an object with field name returned by this method
     */
    public String getCaller() {
        return caller;
    }

    public void setCaller(String caller) {
        this.caller = caller;
    }

    public String getCallerClass() {
        return callerClass;
    }

    public void setCallerClass(String callerClass) {
        this.callerClass = callerClass;
    }

    public String getCallerMethod() {
        return callerMethod;
    }

    public void setCallerMethod(String callerMethod) {
        this.callerMethod = callerMethod;
    }

    public String getCallerFile() {
        return callerFile;
    }

    public void setCallerFile(String callerFile) {
        this.callerFile = callerFile;
    }

    public String getCallerLine() {
        return callerLine;
    }

    public void setCallerLine(String callerLine) {
        this.callerLine = callerLine;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    /**
     * The name of the mdc object field.
     * <p/>
     * If this returns null, then the mdc fields will be written inline at the root level of the JSON event output (e.g. as a sibling to all the other fields in this class).
     * <p/>
     * If this returns non-null, then the mdc fields will be written inside an object with field name returned by this method
     */
    public String getMdc() {
        return mdc;
    }

    public void setMdc(String mdc) {
        this.mdc = mdc;
    }

    /**
     * The name of the context object field.
     * <p/>
     * If this returns null, then the context fields will be written inline at the root level of the JSON event output (e.g. as a sibling to all the other fields in this class).
     * <p/>
     * If this returns non-null, then the context fields will be written inside an object with field name returned by this method
     */
    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public String getExceptionClass() {
        return exceptionClass;
    }

    public void setExceptionClass(String exceptionClass) {
        this.exceptionClass = exceptionClass;
    }

    public String getExceptionMessage() {
        return exceptionMessage;
    }

    public void setExceptionMessage(String exceptionMessage) {
        this.exceptionMessage = exceptionMessage;
    }

    /**
     * The name of the exception object field.
     * <p/>
     * If this returns null, then the context fields will be written inline at the root level of the JSON event output (e.g. as a sibling to all the other fields in this class).
     * <p/>
     * If this returns non-null, then the context fields will be written inside an object with field name returned by this method
     */
    public String getException() {
        return exception;
    }

    public void setException(String exception) {
        this.exception = exception;
    }


    public String getNdc() {
        return ndc;
    }

    public void setNdc(String ndc) {
        this.ndc = ndc;
    }


    public List<String> listNames() {
        List<String> namesList = new ArrayList<>();

        namesList.addAll(super.listCommonNames());
        namesList.add(getLogger());
        namesList.add(getThread());
        namesList.add(getLevel());
        namesList.add(getCallerClass());
        namesList.add(getCallerMethod());
        namesList.add(getCallerFile());
        namesList.add(getCallerLine());
        namesList.add(getStackTrace());
        namesList.add(getTags());
        namesList.add(getNdc());

        return  namesList;
    }
}
