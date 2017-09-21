package com.nit.entity;

public enum Department {

	HR("Human Resource"), IT("Information Technology"), Finance("Corporate Finance"),
	Admin("Administration"), Software("Software Development");
	
	private String fullName;
	
	private Department(String fullName){
		this.fullName = fullName;
	}
	
	public String getFullName(){
		return this.fullName;
	}
}
