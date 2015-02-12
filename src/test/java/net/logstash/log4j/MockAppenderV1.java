package net.logstash.log4j;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.Layout;

public class MockAppenderV1 extends AppenderSkeleton {

    private List messages = new ArrayList();

    public MockAppenderV1(Layout layout){
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

    public String[] getMessages() {
        return (String[]) messages.toArray(new String[messages.size()]);
    }

    public void clear() {
        messages.clear();
    }
}
