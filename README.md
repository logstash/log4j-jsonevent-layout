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
log4j.appender.RollingLog.layout=net.logstash.log4j.JSONEventLayout
```

If you use this, your logfile will now have one line per event and it will look something like this:

```
{"@fields":{"timestamp":1354696445564,"level":"INFO","mdc":{},"file":"Server.java","class":"org.eclipse.jetty.server.Server","line_number":"268","method":"doStart"},"@message":"jetty-8.1.7.v20120910","@source_host":"vagrant.vm"}
{"@fields":{"timestamp":1354696445591,"level":"INFO","mdc":{},"file":"NCSARequestLog.java","class":"org.eclipse.jetty.server.NCSARequestLog","line_number":"649","method":"doStart"},"@message":"Opened \/services\/api\/logs\/2012_12_05.request.log","@source_host":"vagrant.vm"}
{"@fields":{"timestamp":1354696445598,"level":"INFO","mdc":{},"file":"ScanningAppProvider.java","class":"org.eclipse.jetty.deploy.providers.ScanningAppProvider","line_number":"113","method":"doStart"},"@message":"Deployment monitor \/services\/api\/contexts at interval 1","@source_host":"vagrant.vm"}
{"@fields":{"timestamp":1354696445603,"level":"INFO","mdc":{},"file":"DeploymentManager.java","class":"org.eclipse.jetty.deploy.DeploymentManager","line_number":"132","method":"addApp"},"@message":"Deployable added: \/services\/api\/contexts\/enstratus-context.xml","@source_host":"vagrant.vm"}
{"@fields":{"timestamp":1354696447170,"level":"INFO","mdc":{},"file":"PlusConfiguration.java","class":"org.eclipse.jetty.plus.webapp.PlusConfiguration","line_number":"95","method":"bindUserTransaction"},"@message":"No Transaction manager found - if your webapp requires one, please configure one.","@source_host":"vagrant.vm"}
{"@fields":{"timestamp":1354696447493,"level":"INFO","mdc":{},"file":"ContextHandler.java","class":"org.eclipse.jetty.server.handler.ContextHandler","line_number":"772","method":"callContextInitialized"},"@message":"started o.e.j.w.WebAppContext{\/,file:\/services\/api\/webapps\/ROOT\/}","@source_host":"vagrant.vm"}
{"@fields":{"timestamp":1354696447500,"level":"INFO","mdc":{},"file":"ContextHandler.java","class":"org.eclipse.jetty.server.handler.ContextHandler","line_number":"772","method":"callContextInitialized"},"@message":"started o.e.j.w.WebAppContext{\/,file:\/services\/api\/webapps\/ROOT\/}","@source_host":"vagrant.vm"}
{"@fields":{"timestamp":1354696447501,"level":"INFO","mdc":{},"file":"ContextHandler.java","class":"org.eclipse.jetty.server.handler.ContextHandler","line_number":"772","method":"callContextInitialized"},"@message":"started o.e.j.w.WebAppContext{\/,file:\/services\/api\/webapps\/ROOT\/}","@source_host":"vagrant.vm"}
{"@fields":{"timestamp":1354696447587,"level":"INFO","mdc":{},"file":"AbstractConnector.java","class":"org.eclipse.jetty.server.AbstractConnector","line_number":"338","method":"doStart"},"@message":"Started SelectChannelConnector@0.0.0.0:15000","@source_host":"vagrant.vm"}
```

If you point logstash to this file and set the format to `json_event`, you'll basically get something like this (stdout debug json format):

```
{"@source":"file://vagrant/services/api/logs/api.log","@tags":[],"@fields":{"timestamp":1354697760477,"level":"INFO","mdc":{},"file":"DeploymentManager.java","class":"org.eclipse.jetty.deploy.DeploymentManager","line_number":"132","method":"addApp"},"@message":"Deployable added: /services/api/contexts/enstratus-context.xml","@source_host":"vagrant","@timestamp":"2012-12-05T08:56:14.031Z","@source_path":"/services/api/logs/api.log","@type":"apilog"}
{"@source":"file://vagrant/services/api/logs/api.log","@tags":[],"@fields":{"timestamp":1354697762807,"level":"INFO","mdc":{},"file":"PlusConfiguration.java","class":"org.eclipse.jetty.plus.webapp.PlusConfiguration","line_number":"95","method":"bindUserTransaction"},"@message":"No Transaction manager found - if your webapp requires one, please configure one.","@source_host":"vagrant","@timestamp":"2012-12-05T08:56:14.039Z","@source_path":"/services/api/logs/api.log","@type":"apilog"}
{"@source":"file://vagrant/services/api/logs/api.log","@tags":[],"@fields":{"timestamp":1354697763255,"level":"INFO","mdc":{},"file":"ContextHandler.java","class":"org.eclipse.jetty.server.handler.ContextHandler","line_number":"772","method":"callContextInitialized"},"@message":"started o.e.j.w.WebAppContext{/,file:/services/api/webapps/ROOT/}","@source_host":"vagrant","@timestamp":"2012-12-05T08:56:14.046Z","@source_path":"/services/api/logs/api.log","@type":"apilog"}
{"@source":"file://vagrant/services/api/logs/api.log","@tags":[],"@fields":{"timestamp":1354697763261,"level":"INFO","mdc":{},"file":"ContextHandler.java","class":"org.eclipse.jetty.server.handler.ContextHandler","line_number":"772","method":"callContextInitialized"},"@message":"started o.e.j.w.WebAppContext{/,file:/services/api/webapps/ROOT/}","@source_host":"vagrant","@timestamp":"2012-12-05T08:56:14.057Z","@source_path":"/services/api/logs/api.log","@type":"apilog"}
{"@source":"file://vagrant/services/api/logs/api.log","@tags":[],"@fields":{"timestamp":1354697763268,"level":"INFO","mdc":{},"file":"ContextHandler.java","class":"org.eclipse.jetty.server.handler.ContextHandler","line_number":"772","method":"callContextInitialized"},"@message":"started o.e.j.w.WebAppContext{/,file:/services/api/webapps/ROOT/}","@source_host":"vagrant","@timestamp":"2012-12-05T08:56:14.064Z","@source_path":"/services/api/logs/api.log","@type":"apilog"}
{"@source":"file://vagrant/services/api/logs/api.log","@tags":[],"@fields":{"timestamp":1354697763408,"level":"INFO","mdc":{},"file":"AbstractConnector.java","class":"org.eclipse.jetty.server.AbstractConnector","line_number":"338","method":"doStart"},"@message":"Started SelectChannelConnector@0.0.0.0:15000","@source_host":"vagrant","@timestamp":"2012-12-05T08:56:14.067Z","@source_path":"/services/api/logs/api.log","@type":"apilog"}
```

Nothing really groundbreaking here. However you can now use this same prefab PatternLayout with ANY appender that supports layouts. If that appender matches up with a logstash input, you've now got flexibility in your transport with reduced filtering impact (since you don't need to parse the logs as much). In fact, if you want to use RabbitMQ, you could use this layout with [@jbrisbin's amqp-appender](https://github.com/jbrisbin/vcloud/tree/master/amqp-appender).

I'll probably be migrating my zmq-appender over to use this as well. I've already started the test cases for this but they're still short in a few places.

# Exceptions

If there is throwable information available in your event, it will be populated under the key `exception` in `@fields` like so:

```json
{
    "@fields": {
        "timestamp": 1354713757564,
        "ndc": "json-layout-test",
        "level": "FATAL",
        "mdc": {},
        "file": "JSONEventLayoutTest.java",
        "exception": {
            "exception_class": "java.lang.IllegalArgumentException",
            "exception_message": "shits on fire, yo",
            "stacktrace": "java.lang.IllegalArgumentException: shits on fire, yo\n\tat net.logstash.log4j.JSONEventLayoutTest.testJSONEventLayoutExceptions(JSONEventLayoutTest.java:102)\n\tat sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)\n\tat sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:39)\n\tat sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:25)\n\tat java.lang.reflect.Method.invoke(Method.java:597)\n\tat org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:44)\n\tat org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:15)\n\tat org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:41)\n\tat org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:20)\n\tat org.junit.internal.runners.statements.RunAfters.evaluate(RunAfters.java:31)\n\tat org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:76)\n\tat org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:50)\n\tat org.junit.runners.ParentRunner$3.run(ParentRunner.java:193)\n\tat org.junit.runners.ParentRunner$1.schedule(ParentRunner.java:52)\n\tat org.junit.runners.ParentRunner.runChildren(ParentRunner.java:191)\n\tat org.junit.runners.ParentRunner.access$000(ParentRunner.java:42)\n\tat org.junit.runners.ParentRunner$2.evaluate(ParentRunner.java:184)\n\tat org.junit.internal.runners.statements.RunBefores.evaluate(RunBefores.java:28)\n\tat org.junit.runners.ParentRunner.run(ParentRunner.java:236)\n\tat org.apache.maven.surefire.junit4.JUnit4TestSet.execute(JUnit4TestSet.java:53)\n\tat org.apache.maven.surefire.junit4.JUnit4Provider.executeTestSet(JUnit4Provider.java:123)\n\tat org.apache.maven.surefire.junit4.JUnit4Provider.invoke(JUnit4Provider.java:104)\n\tat sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)\n\tat sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:39)\n\tat sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:25)\n\tat java.lang.reflect.Method.invoke(Method.java:597)\n\tat org.apache.maven.surefire.util.ReflectionUtils.invokeMethodWithArray(ReflectionUtils.java:164)\n\tat org.apache.maven.surefire.booter.ProviderFactory$ProviderProxy.invoke(ProviderFactory.java:110)\n\tat org.apache.maven.surefire.booter.SurefireStarter.invokeProvider(SurefireStarter.java:175)\n\tat org.apache.maven.surefire.booter.SurefireStarter.runSuitesInProcessWhenForked(SurefireStarter.java:107)\n\tat org.apache.maven.surefire.booter.ForkedBooter.main(ForkedBooter.java:68)"
        },
        "class": "net.logstash.log4j.JSONEventLayoutTest",
        "line_number": "102",
        "method": "testJSONEventLayoutExceptions"
    },
    "@message": "uh-oh",
    "@source_host": "jvstratusmbp.local"
}
```

Easy access to the exception class and exception message let's you work with those....easier.

# Pull Requests
Pull requests are welcome for any and all things - documentation, bug fixes...whatever.
