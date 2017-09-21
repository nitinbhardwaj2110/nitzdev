package com.nit.rest;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import com.nit.entity.Department;
import com.nit.entity.Employee;
import com.nit.entity.Gender;

public class TrickOrTreatWSImpl implements TrickOrTreatWS {

	public String reverseString(String word) {
		if(word!=null && word.trim().length()>0){
			final int length = word.length();
			StringBuffer sb = new StringBuffer(length);
			for(int i=length-1; i>=0; i--){
				sb.append(word.charAt(i));
			}
			return "Here's your TREAT (REVERSED STRING)="+sb.toString().toLowerCase();
		}			
		return "TRICKING ME ..... TICKLE TICKLE TICKLE";
	}

	public Employee getSampleEmployee() {
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
		//DateTime jodaDateOfJoining = new DateTime(2015, 02, 28, 2, 0);
		//employee.setJodaDateOfJoining(jodaDateOfJoining);
		return employee;
	}
	
	private List<Employee> getDummyReportingEmployees(){
		List<Employee> empList = new ArrayList<Employee>();
		Employee e1 = getDummyEmployee("Chikky", "Delhi", "E004", 4, Gender.Female, 70f);
		Employee e2 = getDummyEmployee("Tikka", "Gurgaon", "E005", 5, Gender.Male, 72f);
		empList.add(e1);
		empList.add(e2);
		return empList;
	}
	
	private Employee getDummyEmployee(String name, String city, String empCode, long empId, Gender gender, float percentage){
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
	
	
	public String getInvestedAndPaidBackAmountForNDepthTree(Integer depth){
		System.out.println((2^depth)+" is the jhampigola");
		int factor = (int) Math.pow(2, depth);
		System.out.println(factor+" is the new jhampigola");
		int totalNodesExceptRootInBinaryTree = -2*(1-factor);
		System.out.println("Total Nodes="+totalNodesExceptRootInBinaryTree);
		long totalAmountInvested = totalNodesExceptRootInBinaryTree*57500;
		long totalAmountPaidBack = 162500*totalNodesExceptRootInBinaryTree;
		
		return "[Total_Amount_Invested = "+totalAmountInvested+ "] [TotalAmountPaidBack = "+totalAmountPaidBack+"]";
	}

	public String getInvestedAndPaidBackAmountForNDepthTree(final Integer depth, final Long amountInvestedPerNode, final Long amountPaidPerNode){
		System.out.println((2^depth)+" is the jhampigola");
		int factor = (int) Math.pow(2, depth);
		System.out.println(factor+" is the new jhampigola");
		int totalNodesExceptRootInBinaryTree = -2*(1-factor);
		System.out.println("Total Nodes="+totalNodesExceptRootInBinaryTree);
		long totalAmountInvested = totalNodesExceptRootInBinaryTree * amountInvestedPerNode;
		long totalAmountPaidBack = totalNodesExceptRootInBinaryTree * amountPaidPerNode;
		
		return "[Total_Amount_Invested = "+totalAmountInvested+ "] [TotalAmountPaidBack = "+totalAmountPaidBack+"]"+" Loss="+(totalAmountPaidBack-totalAmountInvested);
	}

    @Override
    public Employee echoEmployee(Employee employee)
    {
        System.out.println("TrickOrTreatWSImpl::echoEmployee:: Got Employee:"+employee.getFirstName());
        System.out.println("TrickOrTreatWSImpl::echoEmployee:: Returning the same Employee:");
        return employee;
    }
	
	
	
}
