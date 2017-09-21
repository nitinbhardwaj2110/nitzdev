package com.nit.json.jackson.processor;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Map;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nit.entity.Department;
import com.nit.entity.Employee;

/**
 * This class shows how to use Streaming API of Jackson to read JSON streams and to write JSON streams. Streaming API is a part of Jackson Core.
 * https://github.com/FasterXML/jackson-core/
 * Must Read: http://www.cowtowncoder.com/blog/archives/2009/01/entry_132.html
 * @author ezcfghn
 *
 */
public class ReadWriteStreamer {

	public static void main(String[] args) throws JsonParseException, IOException {
		Employee employee = readJsonStream();
	}
	
	public static Employee readJsonStream() throws JsonParseException, IOException{
		JsonFactory jsonFactory = new JsonFactory();
		JsonParser parser = jsonFactory.createParser(new File("/home/ezcfghn/Technical Documents/REST_JAVA/employee.json"));
		ObjectMapper mapper = new ObjectMapper(jsonFactory);
		mapper.readTree(parser);
		if(parser.nextToken()!= JsonToken.START_OBJECT){
			 throw new IOException("Expected data to start with an Object");
		}
		Employee employee = new Employee();
		
		while (parser.nextToken() != JsonToken.END_OBJECT){
			String fieldName = parser.getCurrentName();
			   // Let's move to value
			parser.nextToken();
			   if (fieldName.equals("Id")) {
				   employee.setId(parser.getLongValue());
			   } else if (fieldName.equals("employeeCode")) {
				   employee.setEmployeeCode(parser.getText());
			   }else if (fieldName.equals("firstName")) {
				   employee.setFirstName(parser.getText());
			   }else if (fieldName.equals("dateOfBirth")) {
				   employee.setDateOfBirth(new Date(parser.getLongValue()));
			   }else if (fieldName.equals("department")) {
				   employee.setDepartment(Department.valueOf(parser.getText()));
			   }else if (fieldName.equals("country")) {
				   employee.setCountry(parser.getText());
			   }else if (fieldName.equals("academicPercentages")) {
				   employee.setAcademicPercentages((Map<String, Float>)parser.readValueAs(new TypeReference<Map<String, Float>>(){})); 
			   }
		}
		System.out.println("Employee="+employee.getFirstName()+" "+employee.getDepartment().toString());
		//System.out.println("Employee's Academics="+employee.getAcademicPercentages().toString());
		return employee;
	}

}
