# Logstash `json_event` pattern for log4j

[![Build Status](https://travis-ci.org/lusis/log4j-jsonevent-layout.png)](https://travis-ci.org/lusis/log4j-jsonevent-layout)

## What is it?
If you've used log4j, you know that certain appenders support things called _"Layouts"_. These are basically ways for you to control the formatting/rendering of a log event.

There's a full list of formatting macros [here](http://logging.apache.org/log4j/1.2/apidocs/org/apache/log4j/PatternLayout.html) if you're curious.

The PatternLayout an organization uses is pretty much begging to be bikeshedded. This library is essentially a "preformatted" layout that just happens to be the exact format that Logstash uses for `json_event`.

## JSONEventLayout
I recently came to a situation where I needed consider parsing some log4j-generated log events with Logstash. I had [another appender already](https://github.com/lusis/zmq-appender) but unfortunately the external dependency on ZeroMQ itself was a no go. So there were two options:

- The experimental log4j input
- file input

The `log4j` input is actually pretty cool but needs to be refactored a little bit. It's based around log4j's SocketAppender which actually sends a serialized version of the LoggingEvent object over the wire. This has a few issues that are easily worked out but it's also experimental. I needed something a bit more stable. I also wanted to avoid any filtering if at all possible.

Then I remembered the "hack" that would let you dump your [apache](http://cookbook.logstash.net/recipes/apache-json-logs/) or [nginx](http://blog.pkhamre.com/2012/08/23/logging-to-logstash-json-format-in-nginx/) logs in json_event format.

I probably could have pulled this off using a complicated PatternLayout but decided I wanted a turnkey "soluton" that did the work for you. That's what this library is.

# Usage
This is just a quick snippit of a `log4j.properties` file:

```
log4j.rootCategory=WARN, RollingLog
log4j.appender.RollingLog=org.apache.log4j.DailyRollingFileAppender
log4j.appender.RollingLog.Threshold=TRACE
log4j.appender.RollingLog.File=api.log
log4j.appender.RollingLog.DatePattern=.yyyy-MM-dd
log4j.appender.RollingLog.layout=net.logstash.log4j.JSONEventLayoutV1
```

If you use this, your logfile will now have one line per event and it will look something like this:

```json
{
  "mdc":{},
  "line_number":"29",
  "class":"org.eclipse.jetty.examples.logging.EchoFormServlet",
  "@version":1,
  "source_host":"jvstratusmbp.local",
  "thread_name":"qtp513694835-14",
  "message":"Got request from 0:0:0:0:0:0:0:1%0 using Mozilla\/5.0 (Macintosh; Intel Mac OS X 10_9_1) AppleWebKit\/537.36 (KHTML, like Gecko) Chrome\/32.0.1700.77 Safari\/537.36",
  "@timestamp":"2014-01-27T19:52:35.738Z",
  "level":"INFO",
  "file":"EchoFormServlet.java",
  "method":"doPost",
  "logger_name":"org.eclipse.jetty.examples.logging.EchoFormServlet"
}
```

If you point logstash to this file and set the format to `json`, you'll basically get the same thing in your output because no filtering was needed to get this data into the right places.

Nothing really groundbreaking here. However you can now use this same prefab PatternLayout with ANY appender that supports layouts. If that appender matches up with a logstash input, you've now got flexibility in your transport with reduced filtering impact (since you don't need to parse the logs as much). In fact, if you want to use RabbitMQ, you could use this layout with [@jbrisbin's amqp-appender](https://github.com/jbrisbin/vcloud/tree/master/amqp-appender) or [Ryan Tenney's redis appender](https://github.com/ryantenney/log4j-redis-appender)

# Exceptions

If there is throwable information available in your event, it will be populated under the key `exception` like so:

```json
{
  "mdc":{},
  "exception":{
    "exception_class":"java.lang.IllegalArgumentException",
    "exception_message":"Something broke!",
    "stacktrace":"java.lang.IllegalArgumentException: Something broke!\n\tat org.eclipse.jetty.examples.logging.EchoFormServlet.doPost(EchoFormServlet.java:64)\n\tat javax.servlet.http.HttpServlet.service(HttpServlet.java:755)\n\tat javax.servlet.http.HttpServlet.service(HttpServlet.java:848)\n\tat org.eclipse.jetty.servlet.ServletHolder.handle(ServletHolder.java:684)\n\tat org.eclipse.jetty.servlet.ServletHandler.doHandle(ServletHandler.java:501)\n\tat org.eclipse.jetty.server.handler.ScopedHandler.handle(ScopedHandler.java:137)\n\tat org.eclipse.jetty.security.SecurityHandler.handle(SecurityHandler.java:533)\n\tat org.eclipse.jetty.server.session.SessionHandler.doHandle(SessionHandler.java:231)\n\tat org.eclipse.jetty.server.handler.ContextHandler.doHandle(ContextHandler.java:1086)\n\tat org.eclipse.jetty.servlet.ServletHandler.doScope(ServletHandler.java:428)\n\tat org.eclipse.jetty.server.session.SessionHandler.doScope(SessionHandler.java:193)\n\tat org.eclipse.jetty.server.handler.ContextHandler.doScope(ContextHandler.java:1020)\n\tat org.eclipse.jetty.server.handler.ScopedHandler.handle(ScopedHandler.java:135)\n\tat org.eclipse.jetty.server.handler.ContextHandlerCollection.handle(ContextHandlerCollection.java:255)\n\tat org.eclipse.jetty.server.handler.HandlerCollection.handle(HandlerCollection.java:154)\n\tat org.eclipse.jetty.server.handler.HandlerWrapper.handle(HandlerWrapper.java:116)\n\tat org.eclipse.jetty.server.Server.handle(Server.java:370)\n\tat org.eclipse.jetty.server.AbstractHttpConnection.handleRequest(AbstractHttpConnection.java:494)\n\tat org.eclipse.jetty.server.AbstractHttpConnection.content(AbstractHttpConnection.java:982)\n\tat org.eclipse.jetty.server.AbstractHttpConnection$RequestHandler.content(AbstractHttpConnection.java:1043)\n\tat org.eclipse.jetty.http.HttpParser.parseNext(HttpParser.java:865)\n\tat org.eclipse.jetty.http.HttpParser.parseAvailable(HttpParser.java:240)\n\tat org.eclipse.jetty.server.AsyncHttpConnection.handle(AsyncHttpConnection.java:82)\n\tat org.eclipse.jetty.io.nio.SelectChannelEndPoint.handle(SelectChannelEndPoint.java:667)\n\tat org.eclipse.jetty.io.nio.SelectChannelEndPoint$1.run(SelectChannelEndPoint.java:52)\n\tat org.eclipse.jetty.util.thread.QueuedThreadPool.runJob(QueuedThreadPool.java:608)\n\tat org.eclipse.jetty.util.thread.QueuedThreadPool$3.run(QueuedThreadPool.java:543)\n\tat java.lang.Thread.run(Thread.java:695)"
  },
  "line_number":"64",
  "class":"org.eclipse.jetty.examples.logging.EchoFormServlet",
  "@version":1,
  "source_host":"jvstratusmbp.local",
  "message":"[exception] description = \"kaboom\"",
  "thread_name":"qtp1787577195-18",
  "@timestamp":"2014-01-27T20:11:36.006Z",
  "level":"FATAL",
  "file":"EchoFormServlet.java",
  "method":"doPost",
  "logger_name":"org.eclipse.jetty.examples.logging.EchoFormServlet"
}
```

Easy access to the exception class and exception message let's you work with those....easier.

# Sample XML configuration
If you use the XML format for your log4j configuration (and there are valid reasons thanks to AsyncAppender - fml), changing your layout class for your appender would look like this

## Old
```xml
   <appender name="Console" class="org.apache.log4j.ConsoleAppender">
     <param name="Threshold" value="TRACE" />
     <layout class="org.apache.log4j.PatternLayout">
       <param name="ConversionPattern" value="[%d{dd MMM yyyy HH:mm:ss.SSS}] [%p.%c] %m%n" />
     </layout>
   </appender>
```

## New
```xml
   <appender name="Console" class="org.apache.log4j.ConsoleAppender">
     <param name="Threshold" value="TRACE" />
     <layout class="net.logstash.log4j.JSONEventLayoutV1" />
   </appender>
```

Any appender that supports defining the layout can use this.

# V0/V1/Vnothing
Originally the layout class was called `JSONEventLayout`. This was originally written back when there was no versioned event format for logstash. As of Logstash 1.2 and forward, the event format is now versioned. The current version is `1` and defined the following required fields:

- `@version`
- `@timestamp` (optional - will be inferred from event receipt time

Because of this, when adding support for the new format, `JSONEventLayoutV1` was used to allow backwards compatibility. As of `1.6` of the jsonevent-layout library, we've now gone to fully versioned appenders. There is no longer a `JSONEventLayout`. Instead there is:

- `JSONEventLayoutV0`
- `JSONEventLayoutV1`

Work has stopped on V0 but it won't be removed. No new features are added to V0 (custom UserFields for instance).

# Custom User Fields
As of version 1.6, you can now add your own metadata to the event in the form of comma-separated key:value pairs. This can be set in either the log4jconfig OR set on the java command-line:

## log4j config
```xml
<layout class="net.logstash.log4j.JSONEventLayoutV1" >
  <param name="UserFields" value="foo:bar,baz:quz" />
</layout>
```

or

```
log4j.appender.RollingLog.layout=net.logstash.log4j.JSONEventLayoutV1
log4j.appender.RollingLog.layout.UserFields=foo:bar,baz:qux
```

## Command-line
*Note that the command-line version will OVERRIDE any values specified in the config file should there be a key conflict!*

`java -Dnet.logstash.log4j.JSONEventLayoutV1.UserFields="field3:prop3,field4:prop4" -jar .....`

A warning will be logged should you attempt to set values in both places.

# Pull Requests
Pull requests are welcome for any and all things - documentation, bug fixes...whatever.
