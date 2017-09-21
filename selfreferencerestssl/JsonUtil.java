package com.egi.ericsson.eitaas.utils;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;

public class JsonUtil {
	private static ObjectMapper jsonObjectMapper;
	
	private static ObjectMapper getMapper(){
		if(jsonObjectMapper == null){
			jsonObjectMapper = new ObjectMapper();
			jsonObjectMapper.enable(SerializationFeature.WRAP_ROOT_VALUE).
			enable(DeserializationFeature.UNWRAP_ROOT_VALUE).
			configure(MapperFeature.USE_ANNOTATIONS, true);
			AnnotationIntrospector primary = new JacksonAnnotationIntrospector();
			AnnotationIntrospector secondary = new JaxbAnnotationIntrospector();
			AnnotationIntrospector pair = AnnotationIntrospector.pair(primary, secondary);
			jsonObjectMapper.setAnnotationIntrospector(pair);
		}
		return jsonObjectMapper;
	}
	
	public static String toJSON(Object object) 
			throws JsonGenerationException, JsonMappingException, IOException{
       	return getMapper().writeValueAsString(object);
	}
	
	public static <T> T toObject(String jsonRequest, Class<T> clazz) 
			throws JsonParseException, JsonMappingException, IOException{
		return  getMapper().readValue(jsonRequest, clazz);
	 }
	
	public static <T> T toObject(String jsonRequest, TypeReference<T> type) 
			throws JsonParseException, JsonMappingException, IOException{
		return  getMapper().readValue(jsonRequest, type);
	 }
}
