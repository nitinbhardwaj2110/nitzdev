25th November 2016
This folder and document is intended to store the resources or their links to properly learn REST webservices using Java.

The Java EE 7 tutorial from Oracle is a good resource for reading about JAX-RS. Either you can read it from online link or from the PDF in this folder. The main chapters worth reading: Ch- 27, 29, 30, 31
Ch-29: Building RESTful services with JAX-RS
Ch-30: Accessing REST resources with REST Client API
Ch-31: JAX-RS: Advanced Topics

Additionally following chapters are worth reading as they are related to REST:
Ch-19: JSON Processing: API to parse, transform, and query JSON data using the object model or the streaming model.
Ch-21, 22: Bean Validation
Ch-27: Types of Webservices, Which to use?


https://jersey.java.net/


Difference between Jersey and Jackson:
Jersey is an implementation of JAX-RS. You can think of JAX-RS as an Common interface build for  RESTful Web Services. The implementation of this interface is provided by vendors.   There are many implementation of this interface available like JERSEY and Rest-Easy. On the other hand Jackson is a Json Processor. It helps you in converting your objects to json and vice versa.


Process JSON using Jackson
Jackson provides three different ways to process JSON −

1. Streaming API − It reads and writes JSON content as discrete events. JsonParser reads the data, whereas JsonGenerator writes the data. It is the most powerful approach among the three. It has the lowest overhead and it provides the fastest way to perform read/write operations. It is analogous to Stax parser for XML.

2. Tree Model − It prepares an in-memory tree representation of the JSON document. ObjectMapper build tree of JsonNode nodes. It is most flexible approach. It is analogous to DOM parser for XML.

3. Data Binding − It converts JSON to and from Plain Old Java Object (POJO) using property accessor or using annotations. ObjectMapper reads/writes JSON for both types of data bindings. Data binding is analogous to JAXB parser for XML. Data binding is of two types −

Simple Data Binding − It converts JSON to and from Java Maps, Lists, Strings, Numbers, Booleans, and null objects.

Full Data Binding − It converts JSON to and from any Java type.


Main Official Jackson reference page and download: https://github.com/FasterXML/jackson
Core Modules of JSON:
Core modules are the foundation on which extensions (modules) build upon. There are 3 such modules currently (as of Jackson 2.6):
1. Streaming (https://github.com/FasterXML/jackson-core) ("jackson-core") defines low-level streaming API, and includes JSON-specific implementations. This project contains core low-level incremental ("streaming") parser and generator abstractions used by Jackson Data Processor. It also includes the default implementation of handler types (parser, generator) that handle JSON format. This package is the base on which Jackson data-binding package builds on.
2. Annotations (https://github.com/FasterXML/jackson-annotations) ("jackson-annotations") contains standard Jackson annotations. This project contains general purpose annotations for Jackson Data Processor, used on value and handler types. The only annotations not included are ones that require dependency to the Databind package.
3. Databind (https://github.com/FasterXML/jackson-databind) ("jackson-databind") implements data-binding (and object serialization) support on streaming package; it depends both on streaming and annotations packages.This project contains the general-purpose data-binding functionality and tree-model for Jackson Data Processor. It builds on core streaming parser/generator package, and uses Jackson Annotations for configuration.

All the above links has some amount of useful documentation which can be read once you have read the following basic tutorial supplied officially:
Tutorials:
1. http://wiki.fasterxml.com/JacksonInFiveMinutes
2. https://github.com/FasterXML/jackson-datatype-joda
3. https://github.com/FasterXML/jackson-dataformat-avro
4. https://github.com/FasterXML/jackson-dataformats-binary	(3rd i.e. Avro has become a part of this from Jackson 2.8)
5. https://github.com/FasterXML/jackson-dataformats-binary/tree/master/avro
6. http://www.joda.org/joda-time/

https://www.copterlabs.com/json-what-it-is-how-it-works-how-to-use-it/
http://tutorials.jenkov.com/java-json/jackson-objectmapper.html
http://www.baeldung.com/jackson-deserialization

Download Jackson:
https://mvnrepository.com/search?q=jackson


JAVA REST WEBSERVICES (JAX-RS) using JERSEY implementation:
Once you are done with Exploring Jackon by following above text, it'll be time to explore REST Webservices in Java (JAX-RS). Jersey provides implementation for JAX-RS Specification APIs. Hence, we need both JAX-RS specs API jar and Jersey implementation JARS.
2 main set of dependencies are needed to start developing REST WS using Jersey:

1st set: A Jersey Container jar
We need atleast 1 Jersey container jar. It could be with the artifact ID (jersey-container-servlet) if you're using Jersey in a web-app and deploying your WEB-APP to a Servlet specification 3.x or higher.
You could use grizzly jars (Artifact IDs: jersey-container-grizzly2-http, jersey-container-grizzly2-servlet) if you use Grizzly container.

2nd Set: Jersey Core (Client or Server)
Artifact ID: jersey-client. You can use it if you are using Jersey Client APIs.
Artifact ID: jersey-server. You can use it if you are using Jersey Server APIs.

As an example for a web-app in which I use only Jersey Server side APIs, I use the following dependencies inside pom.xml:
    <dependency>
	<groupId>javax.servlet</groupId>
	<artifactId>javax.servlet-api</artifactId>
	<version>3.0.1</version>
	<scope>provided</scope>
    </dependency>
    <dependency>
         <groupId>javax.ws.rs</groupId>
         <artifactId>javax.ws.rs-api</artifactId>
         <version>2.0.1</version>
    </dependency>
    <dependency>
         <groupId>org.glassfish.jersey.containers</groupId>
         <artifactId>jersey-container-servlet</artifactId>
         <version>2.25</version>
    </dependency>
    <dependency>
         <groupId>org.glassfish.jersey.core</groupId>
         <artifactId>jersey-server</artifactId>
         <version>2.25</version>
    </dependency>

For more information on dependencies the following is very useful link:
https://jersey.java.net/documentation/latest/modules-and-dependencies.html

How to kickstart Jersey/REST in your application:
https://jersey.java.net/nonav/documentation/2.0/deployment.html
https://jersey.java.net/documentation/latest/deployment.html
https://jax-rs-spec.java.net/nonav/2.0/apidocs/javax/ws/rs/core/Application.html
http://tomcat.apache.org/whichversion.html

Tutorial:
http://howtodoinjava.com/jersey/jersey-2-hello-world-application-tutorial/
https://jersey.java.net/
https://jersey.java.net/documentation/latest/index.html
https://jersey.java.net/documentation/latest/getting-started.html



Using above you can design a simple Rest WS where it returns either Java Primitive types or Response status but you can't transfer back a custom POJO back using JSON because we have not enabled JSON yet.

There are multiple frameworks that provide support for JSON processing and/or JSON-to-Java binding. The modules listed below provide support for JSON representations by integrating the individual JSON frameworks into Jersey. At present, Jersey integrates with the following modules to provide JSON support:
1. MOXy - JSON binding support via MOXy is a default and preferred way of supporting JSON binding in your Jersey applications since           Jersey 2.0. When JSON MOXy module is on the class-path, Jersey will automatically discover the module and seamlessly enable JSON binding support via MOXy in your applications. (See Section 4.3, “Auto-Discoverable Features”.)
2. Java API for JSON Processing (JSON-P)
3. Jackson
4. Jettison

For detailed information read: https://jersey.java.net/documentation/latest/media.html

MOXy and Jackson are the best as of now in terms of JSON Processing support but Jersey 2.0 has decided to go with MOXy by default. Hence, I decided to try using MOXy instead of Jackson.
The difference between MOXy and Jackson can be read on: http://jersey.576304.n2.nabble.com/Jackson-vs-MOXy-td7581625.html
MOXy is the recommended default, because it can support both JSON and XML as well as full set of JAXB annotations and more goodies (e.g. externalized bindings etc.). You can code your data model POJOs once and have them exposed as both JSON and XML for free.
As for other goodies, MOXy has a lot of extra features. From those directly related to Jersey, for example we currently support entity filtering with MOXy JSON providers. (To be fair, this is something we would like to support with Jackson too, sometime soon.)
Performance-wise, our internal tests indicate that Jackson is slightly faster at the moment.

How to use MOXy?
http://howtodoinjava.com/jersey/jax-rs-jersey-moxy-json-example/

http://localhost:8080/jerseyserver/rest/TrickOrTreat/sampleEmployee
{"academicPercentages":{"entry":[{"key":"High School","value":76.5},{"key":"Engineering","value":70.0},{"key":"Intermediate","value":81.25}]},"city":"Noida","country":"India","dateOfBirth":"1985-11-20T00:00:00+05:30","department":"Software","employeeCode":"E002","firstName":"Chikoo","gender":"Male","id":2,"lastName":"Shake"}

http://localhost:8080/jerseyserver/rest/TrickOrTreat/reverse/nitin

Entity Filtering read:
https://jersey.java.net/documentation/2.4.1/entity-filtering.html
https://github.com/jersey/jersey/tree/2.4.1/examples/entity-filtering-security


http://localhost:8080/jerseyserver/rest/TrickOrTreat/socialTradeFinance/20
https://jersey.java.net/apidocs/2.0/jersey/org/glassfish/jersey/client/proxy/WebResourceFactory.html













































