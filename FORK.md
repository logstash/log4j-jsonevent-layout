Simple refactoring of the JSONEventLayoutV1 format method to separate the creation of the Logstash event and the formatting operation.
The benefit is subclasses of JSONEventLayoutV1 can create the Logstash event, add any specific details to the event, and
then get the formatted string.

Also, added the ability to customize the output field names.