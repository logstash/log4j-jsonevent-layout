package net.logstash.log4j;


public interface IJSONEventLayout {

    public abstract String getUserFields();
    public abstract void  setUserFields(String userFields);
    public abstract boolean getLocationInfo();
    public abstract void setLocationInfo(boolean locationInfo);
}
