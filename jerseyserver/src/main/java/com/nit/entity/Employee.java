package com.nit.entity;

import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="Employee")
public class Employee {
	private Long id;
	
	private String employeeCode;
	private String firstName;
	private String lastName;
	private Date dateOfBirth;
	private Gender gender;
	private Department department;
	private String city;
	private String country;
	private Map<String, Float> academicPercentages; 
	//private DateTime jodaDateOfJoining;
	private List<String> hobbies;
	private List<Employee> reportingEmployees;
	
	//@JsonProperty("Id")
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	
	//@JsonProperty
	@XmlElement(name="employeeId")     // To override the default name of the property after response generation.
	public String getEmployeeCode() {
		return employeeCode;
	}
	public void setEmployeeCode(String employeeCode) {
		this.employeeCode = employeeCode;
	}
	
	//@JsonProperty
	public String getFirstName() {
		return firstName;
	}
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}
	
	//@JsonProperty
	public String getLastName() {
		return lastName;
	}
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}
	
	//@JsonProperty
	public Date getDateOfBirth() {
		return dateOfBirth;
	}
	public void setDateOfBirth(Date dateOfBirth) {
		this.dateOfBirth = dateOfBirth;
	}
	
	//@JsonProperty
	public Gender getGender() {
		return gender;
	}
	public void setGender(Gender gender) {
		this.gender = gender;
	}
	
	//@JsonProperty
	public Department getDepartment() {
		return department;
	}
	public void setDepartment(Department department) {
		this.department = department;
	}
	
	//@JsonProperty
	public String getCity() {
		return city;
	}
	public void setCity(String city) {
		this.city = city;
	}
	
	
	public String getCountry() {
		return country;
	}
	public void setCountry(String country) {
		this.country = country;
	}
	
	
	public Map<String, Float> getAcademicPercentages() {
		return academicPercentages;
	}
	public void setAcademicPercentages(Map<String, Float> academicPercentages) {
		this.academicPercentages = academicPercentages;
	}
	/*
	public DateTime getJodaDateOfJoining() {
		return jodaDateOfJoining;
	}
	public void setJodaDateOfJoining(DateTime jodaDateOfJoining) {
		this.jodaDateOfJoining = jodaDateOfJoining;
	}
	*/
	public List<String> getHobbies() {
		return hobbies;
	}
	public void setHobbies(List<String> hobbies) {
		this.hobbies = hobbies;
	}
	public List<Employee> getReportingEmployees() {
		return reportingEmployees;
	}
	public void setReportingEmployees(List<Employee> reportingEmployees) {
		this.reportingEmployees = reportingEmployees;
	}
}
