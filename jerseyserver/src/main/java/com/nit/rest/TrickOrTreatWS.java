package com.nit.rest;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.nit.entity.Employee;

@Path("/TrickOrTreat")
public interface TrickOrTreatWS {
	
	@GET
	@Path("/reverse/{word}")
	public String reverseString(@PathParam("word") String word);
	
	@GET
	@Path("/sampleEmployee")
	@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
	//@Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})       // GET request: doesn't consume anything
	public Employee getSampleEmployee();

	@GET
	@Path("/socialTradeFinance/{binaryTreeDepth}")
	public String getInvestedAndPaidBackAmountForNDepthTree(@PathParam("binaryTreeDepth") Integer depth);
	
	@GET
	@Path("/socialTradeFinance/{binaryTreeDepth}/{amountInvestedPerNode}/{amountPaidPerNode}")
	public String getInvestedAndPaidBackAmountForNDepthTree(@PathParam("binaryTreeDepth")  final Integer depth, 
			@PathParam("amountInvestedPerNode")  final Long amountPerNode,
			@PathParam("amountPaidPerNode") final Long amountPaidPerNode);
	
	@POST
    @Path("/echoEmployee")
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})   
	public Employee echoEmployee(Employee employee);
	
}
