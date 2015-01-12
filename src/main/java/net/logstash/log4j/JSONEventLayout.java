package net.logstash.log4j;

import net.logstash.log4j.JSONEventLayoutV0;
import org.apache.log4j.helpers.LogLog;

public class JSONEventLayout extends JSONEventLayoutV0 {

    public JSONEventLayout() {
        this(true);
    }

    public JSONEventLayout(boolean locationInfo) {
        super(locationInfo);
        String whoami = this.getClass().getSimpleName();
        LogLog.warn("["+whoami+"] JSONEventLayout use is discouraged, use JSONEventLayoutV0 instead");
    }

}
