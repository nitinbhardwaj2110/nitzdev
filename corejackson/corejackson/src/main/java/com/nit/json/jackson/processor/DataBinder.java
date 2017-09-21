package com.nit.json.jackson.processor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nit.entity.Department;
import com.nit.entity.Employee;
import com.nit.entity.Gender;

/**
 * This class just tests the JSON-Java Data Binding concept using Jackson's databinding API.
 * https://github.com/FasterXML/jackson-databind/
 * http://wiki.fasterxml.com/JacksonDataBinding
 * @author ezcfghn
 *
 */
public class DataBinder {

	public static void main(String[] args) throws JsonGenerationException, JsonMappingException, IOException {
		//convertBottleJavaObjectToJSON();
		//createJSONObjectManually();
		//convertJavaObjectToJSON();
		convertJSONToJavaObject();
	}
	
	public static void convertJavaObjectToJSON() throws JsonGenerationException, JsonMappingException, IOException{
		Employee employee = new Employee();
		employee.setCity("Noida");
		employee.setCountry("India");
		GregorianCalendar gc = new GregorianCalendar(1985, 10, 20);
		employee.setDateOfBirth(gc.getTime());
		employee.setEmployeeCode("E002");
		employee.setFirstName("Chikoo");
		employee.setLastName("Shake");
		employee.setDepartment(Department.Software);
		employee.setGender(Gender.Male);
		employee.setId(2L);
		Map<String, Float> academicPercentages = new HashMap<String, Float>();
		academicPercentages.put("High School", 76.5f);
		academicPercentages.put("Intermediate", 81.25f);
		academicPercentages.put("Engineering", 70f);
		employee.setAcademicPercentages(academicPercentages);
		List<String> hobbies = new ArrayList<String>();
		hobbies.add("Cricket");
		hobbies.add("Chess");
		hobbies.add("Traveling");
		hobbies.add("Reading");
		employee.setHobbies(hobbies);
		employee.setReportingEmployees(getDummyReportingEmployees());
		DateTime jodaDateOfJoining = new DateTime(2015, 02, 28, 2, 0);
		employee.setJodaDateOfJoining(jodaDateOfJoining);
		ObjectMapper mapper = new ObjectMapper();
		//JaxbAnnotationModule module = new JaxbAnnotationModule();
		mapper.configure(SerializationFeature.WRAP_ROOT_VALUE,true);      
		//objectMapper.configure(DeserializationFeature.UNWRAP_ROOT_VALUE,true);
		mapper.writeValue(System.out, employee);
	}
	
	// We've saved the output from above to the file used below.
	public static Employee convertJSONToJavaObject() throws JsonParseException, JsonMappingException, IOException{
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.UNWRAP_ROOT_VALUE,true);      
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		Employee employee = mapper.readValue(new File("/home/ezcfghn/Technical Documents/REST_JAVA/EmployeeUpdated.json"), Employee.class);
		System.out.println("Name:"+employee.getFirstName()+" "+employee.getLastName());
		System.out.println("Department:"+employee.getDepartment().toString());
		System.out.println("Academic Percentages:"+employee.getAcademicPercentages().toString());
		System.out.println("Date Of Joining:"+employee.getJodaDateOfJoining().toString());
		System.out.flush();
		System.out.close();
		return employee;
	}

	
	private static List<Employee> getDummyReportingEmployees(){
		List<Employee> empList = new ArrayList<Employee>();
		Employee e1 = getDummyEmployee("Chikky", "Delhi", "E004", 4, Gender.Female, 70f);
		Employee e2 = getDummyEmployee("Tikka", "Gurgaon", "E005", 5, Gender.Male, 72f);
		empList.add(e1);
		empList.add(e2);
		return empList;
	}
	
	private static Employee getDummyEmployee(String name, String city, String empCode, long empId, Gender gender, float percentage){
		Employee employee = new Employee();
		employee.setCity(city);
		employee.setCountry("India");
		GregorianCalendar gc = new GregorianCalendar(1985, 10, 20);
		employee.setDateOfBirth(gc.getTime());
		employee.setEmployeeCode(empCode);
		employee.setFirstName(name);
		employee.setLastName("L"+name);
		employee.setDepartment(Department.Software);
		employee.setGender(gender);
		employee.setId(empId);
		Map<String, Float> academicPercentages = new HashMap<String, Float>();
		academicPercentages.put("High School", percentage);
		academicPercentages.put("Intermediate", percentage+2);
		academicPercentages.put("Engineering", percentage-2);
		employee.setAcademicPercentages(academicPercentages);
		List<String> hobbies = new ArrayList<String>();
		hobbies.add("Cricket");
		hobbies.add("Chess");
		hobbies.add("Traveling");
		hobbies.add("Reading");
		employee.setHobbies(hobbies);
		return employee;
	}
	
	/**
	 * To depict how can we instantiate an inner class whose sole purpose is to provide a JSON String.
	 * @throws JsonGenerationException
	 * @throws JsonMappingException
	 * @throws IOException
	 */
	public static void convertBottleJavaObjectToJSON() throws JsonGenerationException, JsonMappingException, IOException{
		
		DataBinder binder = new DataBinder();
		Bottle bottle = binder.new Bottle("Cello Traveller","Silver","91828283732");
		
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(SerializationFeature.WRAP_ROOT_VALUE,true);      
		mapper.writeValue(System.out, bottle);
	}

	@JsonRootName("Bottle")
	private class Bottle{
		
		private String make;
		private String color;
		private String barCode;
		
		public Bottle(String make, String color, String barCode){
			this.make=make;
			this.color=color;
			this.barCode=barCode;
		}
		
		@JsonProperty("Make")
		public String getMake() {
			return make;
		}
		public void setMake(String make) {
			this.make = make;
		}
		
		@JsonProperty("Color")
		public String getColor() {
			return color;
		}
		public void setColor(String color) {
			this.color = color;
		}
		
		@JsonProperty("BarCode")
		public String getBarCode() {
			return barCode;
		}
		public void setBarCode(String barCode) {
			this.barCode = barCode;
		}
		
	}
	
	public static void createJSONObjectManually() throws JsonGenerationException, JsonMappingException, IOException{
		
		final JsonNodeFactory factory = JsonNodeFactory.instance;
		ObjectNode rootObjectNode = new ObjectNode(factory);
		ObjectNode containerNode = rootObjectNode.objectNode();
		containerNode.put("Field1", "Value1");
		containerNode.put("Field2", "Value2");
		containerNode.put("Field3", "Value3");
		containerNode.put("Field4", "Value4");
		rootObjectNode.set("Class", containerNode);
		System.out.println("Nitin JSON="+rootObjectNode.toString());
	}
	
}
