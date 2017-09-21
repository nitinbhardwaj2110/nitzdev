package com.nit.rest.client;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.client.ClientConfig;

import com.nit.client.entity.Department;
import com.nit.client.entity.Employee;
import com.nit.client.entity.Gender;

public class RestTest {

	public static void main(String args[]){
		//test1("iGAyt tiKnA");
		test4();
	}
	
	/**
	 * This method is for hitting a REST service and getting it's response. The REST service being hit returns a String. The REST service
	 * expects a Path Parameter to be passed to it.
	 * @param pathParam
	 */
	public static void test1(String pathParam){
		Client client = getClient();
		//  http://localhost:8080/jerseyserver/rest/TrickOrTreat/reverse/nitin
		WebTarget webTarget = client.target("http://localhost:8080/jerseyserver/rest");
		WebTarget resourceWebTarget = webTarget.path("TrickOrTreat");		
		WebTarget helloworldWebTarget = resourceWebTarget.path("reverse").path(pathParam);
		
		// WebTarget helloworldWebTargetWithQueryParam = helloworldWebTarget.queryParam("greeting", "Hi World!");

		// An HTTP header Accept: application/json will be added to this HTTP REST Request.
		// The Accept HTTP Header defines which Media type the Client is requesting to the server to return. In this case it is JSON.
		Invocation.Builder invocationBuilder = helloworldWebTarget.request(MediaType.APPLICATION_JSON); 
		invocationBuilder.header("some-header", "true"); // You can set an HTTP Header explicitly like this.
		 
		Response response = invocationBuilder.get();
		System.out.println(response.getStatus());
		System.out.println(response.readEntity(String.class));
	}
	
	/**
	 * This method hits REST service which returns a JSON for an Employee type of Object. Note that we have not changed anything
	 * wrt to test1() method except that this REST call doesn't need any input parameter.
	 */
	public static void test2(){
		Client client = getClient();
		//  http://localhost:8080/jerseyserver/rest/TrickOrTreat/sampleEmployee
		WebTarget webTarget = client.target("http://localhost:8080/jerseyserver/rest");
		WebTarget resourceWebTarget = webTarget.path("TrickOrTreat");		
		WebTarget helloworldWebTarget = resourceWebTarget.path("sampleEmployee");

		// An HTTP header Accept: application/json will be added to this HTTP REST Request.
		// The Accept HTTP Header defines which Media type the Client is requesting to the server to return. In this case it is JSON.
		Invocation.Builder invocationBuilder = helloworldWebTarget.request(MediaType.APPLICATION_JSON);
		 
		Response response = invocationBuilder.get();
		System.out.println(response.getStatus());
		System.out.println(response.readEntity(String.class));
	}

	
	/**
	 * This method hits REST service which returns a JSON for an Employee type of Object. In test2() method we directly printed out the JSON.
	 * In this method we want to convert that JSON into Employee object but that is possible only when we implement following 2 things:
	 * 1. Include the Employee class (and its dependencies like Department, Gender) in this project either by directly copying or by
	 * including the jar which contains those model classes.
	 * 2. Register a JSON provider with Jersey to convert the JSON for Employee object (server) into an Employee object here on client side.
	 * Further Read the Introduction on the following link to be clear about what is Entity Provider or MessageBodyReader/Writer 
	 * and its relationship with Jersey (Very Nicely written):
	 * https://jersey.java.net/documentation/latest/message-body-workers.html
	 */
	public static void test3(){
		Client client = getClient();
		//  http://localhost:8080/jerseyserver/rest/TrickOrTreat/sampleEmployee
		WebTarget webTarget = client.target("http://localhost:8080/jerseyserver/rest");
		WebTarget resourceWebTarget = webTarget.path("TrickOrTreat");		
		WebTarget helloworldWebTarget = resourceWebTarget.path("sampleEmployee");

		// An HTTP header Accept: application/json will be added to this HTTP REST Request.
		// The Accept HTTP Header defines which Media type the Client is requesting to the server to return. In this case it is JSON.
		//Invocation.Builder invocationBuilder = helloworldWebTarget.request(MediaType.APPLICATION_JSON);
		Invocation.Builder invocationBuilder = helloworldWebTarget.request(MediaType.APPLICATION_XML);
		 
		Response response = invocationBuilder.get();
		System.out.println(response.getStatus());
		Employee employee = response.readEntity(Employee.class);
		/* The above will throw exception if there is no JSON provider registered with Jersey. By default Moxy is the JSON provider registered 
		 * in Jersey but only if you include its dependency into your pom or in other words include Moxy's JAR on your classpath.
		 * It threw the following exception until I included JERSEY MOXY in my pom.xml and clean build the project:
		 * Exception in thread "main" org.glassfish.jersey.message.internal.MessageBodyProviderNotFoundException: MessageBodyReader not found for media type=application/json, type=class com.nit.client.entity.Employee, genericType=class com.nit.client.entity.Employee
		 * 
		 * But once I included JERSEY MOXY in my pom.xml and clean build the project the above exception was gone.
		 */
		System.out.println(employee);
	}

	
	/**
	 * In this the client accepts the XML type of response.
	 */
	public static void test4(){
	       Client client = getClient();
	        //  http://localhost:8080/jerseyserver/rest/TrickOrTreat/echoEmployee
	        WebTarget webTarget = client.target("http://localhost:8080/jerseyserver/rest");
	        WebTarget resourceWebTarget = webTarget.path("TrickOrTreat");       
	        WebTarget helloworldWebTarget = resourceWebTarget.path("echoEmployee");

	        // An HTTP header Accept: application/xml will be added to this HTTP REST Request.
	        // The Accept HTTP Header defines which Media type the Client is requesting to the server to return. In this case it is XML.
	        Invocation.Builder invocationBuilder = helloworldWebTarget.request(MediaType.APPLICATION_XML);
	        Employee reqEmployee = getSampleEmployee();
	        Employee respEmployee = invocationBuilder.post(Entity.xml(reqEmployee), Employee.class);
	        System.out.println("RestTest::test4():: Got Employee Back::"+respEmployee.getFirstName());
	}
	
	/**
	 * To call the REST WS using Client Proxy mechanism.
	 */
	public static void test5(){
		
	}
	
	public static Client getClient(){
		ClientConfig clientConfig = new ClientConfig();
		Client client = ClientBuilder.newClient(clientConfig);
		return client;
	}
	
	/*
	private <C> C getWebResource(Class<C> resourceInterface) {
		
		//WebResourceFactory
	}
	*/
	
	
	   public static Employee getSampleEmployee() {
	        Employee employee = new Employee();
	        employee.setCity("Noida");
	        employee.setCountry("India");
	        GregorianCalendar gc = new GregorianCalendar(1985, 10, 20);
	        employee.setDateOfBirth(gc.getTime());
	        employee.setEmployeeCode("E002");
	        employee.setFirstName("Banana");
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
	        //DateTime jodaDateOfJoining = new DateTime(2015, 02, 28, 2, 0);
	        //employee.setJodaDateOfJoining(jodaDateOfJoining);
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

}
