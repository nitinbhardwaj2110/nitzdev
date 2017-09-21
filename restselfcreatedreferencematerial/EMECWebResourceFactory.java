/*
* DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
*
* Copyright (c) 2012-2013 Oracle and/or its affiliates. All rights reserved.
*
* The contents of this file are subject to the terms of either the GNU
* General Public License Version 2 only ("GPL") or the Common Development
* and Distribution License("CDDL") (collectively, the "License"). You
* may not use this file except in compliance with the License. You can
* obtain a copy of the License at
* http://glassfish.java.net/public/CDDL+GPL_1_1.html
* or packager/legal/LICENSE.txt. See the License for the specific
* language governing permissions and limitations under the License.
*
* When distributing the software, include this License Header Notice in each
* file and include the License file at packager/legal/LICENSE.txt.
*
* GPL Classpath Exception:
* Oracle designates this particular file as subject to the "Classpath"
* exception as provided by Oracle in the GPL Version 2 section of the License
* file that accompanied this code.
*
* Modifications:
* If applicable, add the following below the License Header, with the fields
* enclosed by brackets [] replaced by your own identifying information:
* "Portions Copyright [year] [name of copyright owner]"
*
* Contributor(s):
* If you wish your version of this file to be governed by only the CDDL or
* only the GPL Version 2, indicate your decision by adding "[Contributor]
* elects to include this software in this distribution under the [CDDL or GPL
* Version 2] license." If you don't indicate a single choice of license, a
* recipient has the option to distribute your version of this file under
* either the CDDL, the GPL Version 2 or to extend the choice of license to
* its licensees as provided above. However, if you add GPL Version 2 code
* and therefore, elected the GPL Version 2 license, then the option applies
* only if the new code is made subject to such option by the copyright
* holder.
*/
package com.egi.ericsson.eitaas.rest.client;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.validation.Valid;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.ResponseProcessingException;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.glassfish.jersey.server.validation.ValidationError;

import com.egi.ericsson.eitaas.exceptions.EMECOAuthException;
import com.egi.ericsson.eitaas.exceptions.ValidationFailExeception;
import com.egi.ericsson.eitaas.utils.JsonUtil;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.egi.ericsson.eitaas.oauth.EMECOAuthUtils;
/**
* Factory for client-side representation of a resource.
* See the <a href="package-summary.html">package overview</a>
* for an example on how to use this class.
*
* @author Martin Matula (martin.matula at oracle.com)
*/
public final class EMECWebResourceFactory implements InvocationHandler {
    private final WebTarget target;
    private final MultivaluedMap<String, Object> headers;
    private final List<Cookie> cookies;
    private final Form form;

    private static final MultivaluedMap<String, Object> EMPTY_HEADERS = new MultivaluedHashMap<String, Object>();
    private static final Form EMPTY_FORM = new Form();

    /**
* Creates a new client-side representation of a resource described by
* the interface passed in the first argument.
*
* Calling this method has the same effect as calling {@code WebResourceFactory.newResource(resourceInterface, rootTarget,
*false)}.
*
* @param <C> Type of the resource to be created.
* @param resourceInterface Interface describing the resource to be created.
* @param target WebTarget pointing to the resource or the parent of the resource.
* @return Instance of a class implementing the resource interface that can
* be used for making requests to the server.
*/
    public static <C> C newResource(Class<C> resourceInterface, WebTarget target) {
        return newResource(resourceInterface, target, false, EMPTY_HEADERS,
                Collections.<Cookie>emptyList(), EMPTY_FORM);
    }

    /**
* Creates a new client-side representation of a resource described by
* the interface passed in the first argument.
*
* @param <C> Type of the resource to be created.
* @param resourceInterface Interface describing the resource to be created.
* @param target WebTarget pointing to the resource or the parent of the resource.
* @param ignoreResourcePath If set to true, ignores path annotation on the resource interface (this is used when creating
* sub-resources)
* @param headers Header params collected from parent resources (used when creating a sub-resource)
* @param cookies Cookie params collected from parent resources (used when creating a sub-resource)
* @param form Form params collected from parent resources (used when creating a sub-resource)
* @return Instance of a class implementing the resource interface that can
* be used for making requests to the server.
*/
    @SuppressWarnings("unchecked")
    public static <C> C newResource(Class<C> resourceInterface, WebTarget target, boolean ignoreResourcePath,
                                    MultivaluedMap<String, Object> headers, List<Cookie> cookies, Form form) {
        return (C) Proxy.newProxyInstance(AccessController.doPrivileged(getClassLoaderPA(resourceInterface)),
                new Class[]{resourceInterface},
                new EMECWebResourceFactory(ignoreResourcePath ? target : addPathFromAnnotation(resourceInterface, target),
                        headers, cookies, form));
    }
    
    /**
    * Get privileged action to obtain class loader for given class.
    * If run using security manager, the returned privileged action
    * must be invoked within a doPrivileged block.
    *
    * @param clazz class for which to get class loader.
    * @return privileged action to obtain class loader for the {@code clazz} class.
    *
    * @see AccessController#doPrivileged(java.security.PrivilegedAction)
    */
        public static PrivilegedAction<ClassLoader> getClassLoaderPA(final Class<?> clazz) {
            return new PrivilegedAction<ClassLoader>() {
                @Override
                public ClassLoader run() {
                    return clazz.getClassLoader();
                }
            };
        }

    private EMECWebResourceFactory(WebTarget target, MultivaluedMap<String, Object> headers, List<Cookie> cookies,
                               Form form) {
        this.target = target;
        this.headers = headers;
        this.cookies = cookies;
        this.form = form;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // get the interface describing the resource
        Class<?> proxyIfc = proxy.getClass().getInterfaces()[0];

        // response type
        Class<?> responseType = method.getReturnType();

        // determine method name
        String httpMethod = getHttpMethodName(method);
        if (httpMethod == null) {
            for (Annotation ann : method.getAnnotations()) {
                httpMethod = getHttpMethodName(ann.annotationType());
                if (httpMethod != null) {
                    break;
                }
            }
        }

        // create a new UriBuilder appending the @Path attached to the method
        WebTarget newTarget = addPathFromAnnotation(method, target);

        if (httpMethod == null) {
            if (newTarget == target) {
                // no path annotation on the method -> fail
                throw new UnsupportedOperationException("Not a resource method.");
            } else if (!responseType.isInterface()) {
                // the method is a subresource locator, but returns class,
                // not interface - can't help here
                throw new UnsupportedOperationException("Return type not an interface");
            }
        }

        // process method params (build maps of (Path|Form|Cookie|Matrix|Header..)Params
        // and extract entity type
        MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<String, Object>(this.headers);
        LinkedList<Cookie> cookies = new LinkedList<Cookie>(this.cookies);
        Form form = new Form();
        form.asMap().putAll(this.form.asMap());
        Annotation[][] paramAnns = method.getParameterAnnotations();
        Object entity = null;
        Type entityType = null;
        for (int i = 0; i < paramAnns.length; i++) {
            Map<Class, Annotation> anns = new HashMap<Class, Annotation>();
            for (Annotation ann : paramAnns[i]) {
                anns.put(ann.annotationType(), ann);
            }
            Annotation ann;
            Object value = args[i];
            //if (anns.isEmpty()) {
            if(anns.isEmpty() || (anns.size()==1 && anns.get(Valid.class)!=null)) {
                entityType = method.getGenericParameterTypes()[i];
                entity = value;
            } else {
                if (value == null && (ann = anns.get(DefaultValue.class)) != null) {
                    value = ((DefaultValue) ann).value();
                }

                if (value != null) {
                    if ((ann = anns.get(PathParam.class)) != null) {
                        newTarget = newTarget.resolveTemplate(((PathParam) ann).value(), value);
                    } else if ((ann = anns.get((QueryParam.class))) != null) {
                        newTarget = newTarget.queryParam(((QueryParam) ann).value(), value);
                    } else if ((ann = anns.get((HeaderParam.class))) != null) {
                        headers.addAll(((HeaderParam) ann).value(), value);
                    } else if ((ann = anns.get((CookieParam.class))) != null) {
                        String name = ((CookieParam) ann).value();
                        Cookie c;
                        if (!(value instanceof Cookie)) {
                            c = new Cookie(name, value.toString());
                        } else {
                            c = (Cookie) value;
                            if (!name.equals(((Cookie) value).getName())) {
                                // is this the right thing to do? or should I fail? or ignore the difference?
                                c = new Cookie(name, c.getValue(), c.getPath(), c.getDomain(), c.getVersion());
                            }
                        }
                        cookies.add(c);
                    } else if ((ann = anns.get((MatrixParam.class))) != null) {
                        newTarget = newTarget.matrixParam(((MatrixParam) ann).value(), value);
                    } else if ((ann = anns.get((FormParam.class))) != null) {
                        form.param(((FormParam) ann).value(), value.toString());
                    }
                }
            }
        }
        
	    /*
	     * Start of Setting OAuth access token in the header.
	     */
        String token = EMECOAuthUtils.getAccessToken();
        if(StringUtils.isNotEmpty(token)){
        	headers.add("Authorization", "bearer "+token);
        }else{
        	throw new EMECOAuthException("Acess token is not received from authorization server");
        }
        /*
         * End of Setting OAuth access token in the header.
         */
        
        if (httpMethod == null) {
            // the method is a subresource locator
            return EMECWebResourceFactory.newResource(responseType, newTarget, true, headers, cookies, form);
        }

        // accepted media types
        Produces produces = method.getAnnotation(Produces.class);
        if (produces == null) {
            produces = proxyIfc.getAnnotation(Produces.class);
        }
        String[] accepts = produces == null ? null : produces.value();

        // determine content type
        String contentType = null;
        if (entity != null) {
            Consumes consumes = method.getAnnotation(Consumes.class);
            if (consumes == null) {
                consumes = proxyIfc.getAnnotation(Consumes.class);
            }
            if (consumes != null && consumes.value().length > 0) {
                // TODO: should consider q/qs instead of picking the first one
                contentType = consumes.value()[0];
            }
        }

        Invocation.Builder b;
        if (accepts != null) {
            b = newTarget.request(accepts);
        } else {
            b = newTarget.request();
        }

        // apply header params and cookies
        for (Cookie c : cookies) {
            b = b.cookie(c);
        }
        // TODO: change this to b.headers(headers) once we switch to the latest JAX-RS API
        for (Map.Entry<String, List<Object>> header : headers.entrySet()) {
            for (Object value : header.getValue()) {
                b = b.header(header.getKey(), value);
            }
        }
        //Ashok : Explicitly set it to null so that caller can check by it.
        Object result = null;

        if (entity == null && !form.asMap().isEmpty()) {
            entity = form;
            contentType = MediaType.APPLICATION_FORM_URLENCODED;
        } else {
            if (contentType == null) {
                contentType = MediaType.APPLICATION_OCTET_STREAM;
            }
            if (!form.asMap().isEmpty()) {
                if (entity instanceof Form) {
                    ((Form) entity).asMap().putAll(form.asMap());
                } else {
                    // TODO: should at least log some warning here
                }
            }
        }

        GenericType responseGenericType = new GenericType(method.getGenericReturnType());
        try{
        	if (entity != null) {
        		if (entityType instanceof ParameterizedType) {
        			entity = new GenericEntity(entity, entityType);
        		}
        		result = b.method(httpMethod, Entity.entity(entity, contentType), responseGenericType);
        	} else {
        		result = b.method(httpMethod, responseGenericType);
        	}
        	if(result instanceof Response){
        		if(handleErrorMessages((Response)result)){
        			result = null;
        		}
        	}
        	
        }catch(BadRequestException badRequest){
        	handleErrorMessages(badRequest.getResponse());
        }catch(WebApplicationException webException){
        	handleErrorMessages(webException.getResponse());	
        }catch(ResponseProcessingException responseException){
        	handleErrorMessages(responseException.getResponse());
        }catch(ProcessingException ex){
        	handleErrorMessages(null);
        }

        return result;
    }
    
    /**
     * Handle Exception returned from REST call and add to ThreadLocal.
     * @param response
     */
    private boolean handleErrorMessages(Response response){
    	if(null != response && response.getStatusInfo().getFamily().equals(
    			Response.Status.Family.CLIENT_ERROR)){
    		if(response.getEntity() instanceof List || response.getEntity() instanceof List<?>) {
    			List<ValidationError> validationErrorList = (List<ValidationError>) response.getEntity();
    				WebResourceContext.setValidationErrors(validationErrorList);
    		}else {
    			List<ValidationError> errors = new ArrayList<ValidationError>();
    			try {
    				String responseEntity = response.readEntity(String.class);
    				errors = JsonUtil.toObject(responseEntity, 
    						new TypeReference<List<ValidationError>>(){});
				} catch (JsonParseException e) {
					e.printStackTrace();
				} catch (JsonMappingException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
    			
    			if(errors != null && !errors.isEmpty()){
    				genericErrorhandler(null, errors);
    			}else{
    				genericErrorhandler(null, null);
    			}
    		}
    		return true;
    	}else if(null == response || response.getStatusInfo().getFamily().equals(
    			Response.Status.Family.SERVER_ERROR)){
    		// Provide Generic Client side error in case, Some strange error.
    		genericErrorhandler(null, null);
        	return true;
    	}
    	
    	return false;
    }
    
    private void genericErrorhandler(String reason, List<ValidationError> validationErrors){
    	List<ValidationError> errors = new ArrayList<ValidationError>();
    	ValidationError serverError = new ValidationError();
    	if(reason != null){
    		serverError.setMessage(reason);
    		errors.add(serverError);
    	}else if(validationErrors != null && !validationErrors.isEmpty()){
    		errors.addAll(validationErrors);
    	}else{
    		serverError.setMessage("GENERIC_ERROR_MSG");
    	}
    	
    	WebResourceContext.setValidationErrors(errors);
    }
    
    
    private static WebTarget addPathFromAnnotation(AnnotatedElement ae, WebTarget target) {
        Path p = ae.getAnnotation(Path.class);
        if (p != null) {
            target = target.path(p.value());
        }
        return target;
    }

    private static String getHttpMethodName(AnnotatedElement ae) {
        HttpMethod a = ae.getAnnotation(HttpMethod.class);
        return a == null ? null : a.value();
    }
}
