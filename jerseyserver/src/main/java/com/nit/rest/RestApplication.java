package com.nit.rest;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import org.glassfish.jersey.moxy.json.MoxyJsonFeature;

import com.nit.rest.config.JsonMoxyConfigurationContextResolver;

public class RestApplication extends Application {

	@Override
	public Set<Class<?>> getClasses() {
		final Set<Class<?>> classes = new HashSet<Class<?>>();
		System.out.println("Hi Nitin Initializing REST Webservices using JERSEY....");
		// register root resources
		classes.add(TrickOrTreatWSImpl.class);
		 //Manually adding MOXyJSONFeature....Although it is the JSON feature by default in Jersey 2.x
		classes.add(MoxyJsonFeature.class);
		classes.add(JsonMoxyConfigurationContextResolver.class);
		System.out.println("Initialized REST Webservices using JERSEY....COMPLETED");
		return classes;
	}
	
}
