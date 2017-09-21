package com.nit.rest.config;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

import org.eclipse.persistence.jaxb.JAXBContextProperties;
import org.glassfish.jersey.moxy.json.MoxyJsonConfig;

//Register ContextResolver<MoxyJsonConfig> to override 
//default behavior of marshaling/un-marshaling attributes

@Provider
public class JsonMoxyConfigurationContextResolver implements ContextResolver<MoxyJsonConfig> 
{

  private final MoxyJsonConfig config;

  public JsonMoxyConfigurationContextResolver() 
  {
      final Map<String, String> namespacePrefixMapper = new HashMap<String, String>();
      namespacePrefixMapper.put("http://www.w3.org/2001/XMLSchema-instance", "xsi");

      config = new MoxyJsonConfig()
              .setNamespacePrefixMapper(namespacePrefixMapper)
              .setNamespaceSeparator(':')
              .setAttributePrefix("")
              .property(JAXBContextProperties.JSON_WRAPPER_AS_ARRAY_NAME, true)
              //.setFormattedOutput(true)
              .setIncludeRoot(true)
              .setMarshalEmptyCollections(true);      
  }

  @Override
  public MoxyJsonConfig getContext(Class<?> type){
      return config;
  }
  
  
  /*
   * Approach 1.
   * O/P When the property(JAXBContextProperties.JSON_WRAPPER_AS_ARRAY_NAME, true) is used:
   {
   "academicPercentages" : [ {
      "key" : "High School",
      "value" : 76.5
   }, {
      "key" : "Engineering",
      "value" : 70.0
   }, {
      "key" : "Intermediate",
      "value" : 81.25
   } ],
   "city" : "Noida",
   "country" : "India",
   "dateOfBirth" : "1985-11-20T00:00:00+05:30",
   "department" : "Software",
   "employeeCode" : "E002",
   "firstName" : "Chikoo",
   "gender" : "Male",
   "id" : 2,
   "lastName" : "Shake"
   }
    */
  
  /*
   * Approach 2.
   O/P When the property(JAXBContextProperties.JSON_WRAPPER_AS_ARRAY_NAME, true) is not used:
   {
   "academicPercentages" : {
      "entry" : [ {
         "key" : "High School",
         "value" : 76.5
      }, {
         "key" : "Engineering",
         "value" : 70.0
      }, {
         "key" : "Intermediate",
         "value" : 81.25
      } ]
   },
   "city" : "Noida",
   "country" : "India",
   "dateOfBirth" : "1985-11-20T00:00:00+05:30",
   "department" : "Software",
   "employeeCode" : "E002",
   "firstName" : "Chikoo",
   "gender" : "Male",
   "id" : 2,
   "lastName" : "Shake"
   }
   
   */
  
  /*
   Rest JSON is similar if we use JACKSON instead of MOXy but JSON for Map (like academicPercentages) using JACKSON is as follows:
   		"academicPercentages":{"High School":76.5,"Engineering":70.0,"Intermediate":81.25}
   whereas using MOXy the JSON comes out to be:
   		"academicPercentages":[{"key":"High School","value":76.5},{"key":"Engineering","value":70.0},{"key":"Intermediate","value":81.25}]
   */
 
}