package net.logstash.log4j;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.Layout;

public class MockAppenderV0 extends AppenderSkeleton {

    private static List messages = new ArrayList();

    public MockAppenderV0(Layout layout){
        this.layout = layout;
    }
    @Override
    protected void append(LoggingEvent event){
        messages.add(layout.format(event));
    }

    public void close(){
        messages.clear();
    }

    public boolean requiresLayout(){
        return true;
    }

    public static String[] getMessages() {
        return (String[]) messages.toArray(new String[messages.size()]);
    }

    public void clear() {
        messages.clear();
    }
}
