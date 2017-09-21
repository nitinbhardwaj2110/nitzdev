package com.egi.ericsson.eitaas.services.deprovision;


import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.net.ssl.SSLContext;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.SslConfigurator;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.egi.ericsson.eitaas.api.Event;
import com.egi.ericsson.eitaas.dao.AppDeprovConfigRepository;
import com.egi.ericsson.eitaas.dao.DeprovAppRequestRepository;
import com.egi.ericsson.eitaas.dao.DeprovisioningReqRepository;
import com.egi.ericsson.eitaas.dao.EndUserAccessRepository;
import com.egi.ericsson.eitaas.entity.AppDeprovisionConfig;
import com.egi.ericsson.eitaas.entity.DeprovAppRequest;
import com.egi.ericsson.eitaas.entity.DeprovisioningRequest;
import com.egi.ericsson.eitaas.entity.EndUserAccess;
import com.egi.ericsson.eitaas.enums.DeprovisionEventEnum;
import com.egi.ericsson.eitaas.enums.DeprovisionStatusEnum;
import com.egi.ericsson.eitaas.exceptions.EMECOAuthException;
import com.egi.ericsson.eitaas.ldap.entity.LDAPUser;
import com.egi.ericsson.eitaas.oauth.EMECOAuthUtils;
import com.egi.ericsson.eitaas.services.CustomerService;
import com.egi.ericsson.eitaas.services.openDJ.LDAPEmployeeService;
import com.egi.ericsson.eitaas.utils.JsonUtil;

/**
 * 
 * @author ezcfghn
 *
 */
@Component(value="deprovisioningInitiatorService")
public class DeprovisioningInitiatorServiceImpl implements DeprovisioningInitiatorService {
	
	@Autowired
	private DeprovisioningReqRepository deprovRequestRepository;
	
	@Autowired
	private DeprovAppRequestRepository deprovAppRequestRepository;	
	
	@Autowired
	@Qualifier("ldapEmployeeService")
	private LDAPEmployeeService ldapEmployeeService;
	
	@Autowired
	private CustomerService customerService;
	
	@Autowired
	private EndUserAccessRepository endUserAccessRepo;
	
	@Autowired
	private AppDeprovConfigRepository appDeprovConfigRepo;
	
	private static final Client client = JerseyClientBuilder.newClient();
    private static Client sslClient = null;
	
	@Value("${trust.store.file}")
	private String trustStoreFile;
	@Value("${trust.store.password}")
	private String trustStorePassword;
	@Value("${key.store.file}")
	private String keyStoreFile;
	@Value("${key.password}")
	private String keyPassword;
	
	//private LDAPEmployeeService ldapEmployeeService	 = getBean("ldapEmployeeService", LDAPEmployeeService.class);
    
	/*
	@Autowired
	private SubscriptionService subscriptionService;
	*/
	
	public DeprovisioningInitiatorServiceImpl(){
		super();
	}
	
	private static final Logger logger = LoggerFactory.getLogger(DeprovisioningInitiatorServiceImpl.class);
	
	@Override
	@Transactional
	public void registerDeprovisioningRequest(Event event){
		boolean handledEvent = this.handleNonDeprovisioningEvents(event);
		if(handledEvent){
			logger.debug("Event Received is not a De-Provisioning event. Handled the business case.");			
			return;
			//throw new IllegalArgumentException("Event's name doesn't match with any of the DeprovisionEventEnum");
		}
		
		DeprovisionEventEnum deprovisionEvent = DeprovisionEventEnum.valueOf(event.getEventName());
		boolean enterpriseDeprovisioned = handleImmediateEnterpriseDeletion(event, deprovisionEvent);
		if(enterpriseDeprovisioned){
			logger.info("Probably Enterprise immediately deprovisioned because of its DELETION. Rightly Caught....Sahi Pakde hain !!");
			return;
		}
		Map<String, Object> eventParams = event.getEventParameters();
		DeprovisioningRequest deprovRequest = new DeprovisioningRequest();
		deprovRequest.setCustomerId(parseID(eventParams.get("CUSTOMER_ID")));
		deprovRequest.setDomainName((String) eventParams.get("DOMAIN_NAME"));
		deprovRequest.setEvent(deprovisionEvent);
		deprovRequest.setProductId(parseID(eventParams.get("PRODUCT_ID")));
		deprovRequest.setStatus(DeprovisionStatusEnum.RECEIVED);
		deprovRequest.setSubscriptionId(parseID(eventParams.get("SUBSCRIPTION_ID")));
		deprovRequest.setTenantId((String) eventParams.get("TENANT_ID"));
		deprovRequest.setSubmissionDate(DateTime.now());
		
		if(deprovisionEvent.isEnterpriseType()){
			LDAPUser customerAdmin = ldapEmployeeService.getAnActiveCustomerAdmin((String) eventParams.get("CUSTOMER_NAME"));
			deprovRequest.setTargetId(customerAdmin.getAliasName());
			List<DeprovAppRequest> deproAppList = new ArrayList<>();
			List<String> appCodeList = (List<String>) eventParams.get("PRODUCT_APP_CODE_LIST");
			for(String appCode: appCodeList){
				DeprovAppRequest deprovAppRequest = new DeprovAppRequest();
				deprovAppRequest.setEvent(deprovisionEvent);
				deprovAppRequest.setProductId(deprovRequest.getProductId());
				deprovAppRequest.setStatus(DeprovisionStatusEnum.RECEIVED);
				//if PRODUCT_UNASSIGNED_FROM_ENTERPRISE and choose to delete data immediately then
				//submission date should be less then x days (as per appconfig) + 10 minutes from current date .
				deprovAppRequest.setSubmissionDate( calculateSubmissionDate((Boolean)eventParams.get("IMMEDIATE_DELETE"), appCode, deprovisionEvent) );				
				deprovAppRequest.setTargetId(deprovRequest.getTargetId());
				deprovAppRequest.setAppCode(appCode);
				deproAppList.add(deprovAppRequest);
			}
			deprovRequest.setAppRequests(deproAppList);			
		}else{
			deprovRequest.setTargetId((String) eventParams.get("TARGET_ID"));
			List<DeprovAppRequest> deproAppList = new ArrayList<>();
			DeprovAppRequest deprovAppRequest = new DeprovAppRequest();
			deprovAppRequest.setEvent(deprovisionEvent);
			deprovAppRequest.setProductId(deprovRequest.getProductId());
			deprovAppRequest.setStatus(DeprovisionStatusEnum.RECEIVED);
			deprovAppRequest.setSubmissionDate(deprovRequest.getSubmissionDate());
			deprovAppRequest.setTargetId(deprovRequest.getTargetId());
			deprovAppRequest.setAppCode((String) eventParams.get("DEPROV_APP_CODE"));
			deproAppList.add(deprovAppRequest);
			deprovRequest.setAppRequests(deproAppList);
		}
		
		DeprovisioningRequest savedDeprovisioningRequest = deprovRequestRepository.save(deprovRequest);
		handleImmediateExecutionEvents((String) eventParams.get("CUSTOMER_NAME"), deprovisionEvent, savedDeprovisioningRequest);
	}	
	
	
	private DateTime calculateSubmissionDate(Boolean immediateDelete, String appCode, DeprovisionEventEnum deprovisionEvent) {
		//if product has been unsubscribed by SME (PRODUCT_UNASSIGNED_FROM_ENTERPRISE event) and SME has choosen to delete the data immediately then reduce the submissiondate 
		//of scheduled task (select * from EIT_MT_DEPROV_APP_CONFIG where IMMEDIATE_EXECFLAG='N' and ApplicationCode in (appcodes to be deprovisioned) and EVENT_NAME='PRODUCT_UNASSIGNED_FROM_ENTERPRISE')
		//, so that scheduler should execute it immediately.

		//Non-scheduled task (select * from EIT_MT_DEPROV_APP_CONFIG where IMMEDIATE_EXECFLAG='Y' and ApplicationCode in (appcodes to be deprovisioned) and EVENT_NAME='PRODUCT_UNASSIGNED_FROM_ENTERPRISE') 
		//will be executed as it is by handleImmediateExecutionEvents irrespective of whatever value we have in submissiondate field.
		
		//For Events other then PRODUCT_UNASSIGNED_FROM_ENTERPRISE, task will be executed as default implementations.
		
		//Scheduler executes the event if  (submitondatetime + days limit in EIT_MT_DEPROV_APP_CONFIG) > current date
		if(isProductUnAssignedFromEnterprise(deprovisionEvent) && immediateDelete) {
			AppDeprovisionConfig appDeprovConfig = getAppDeprovisionConfigMapByAppCodeAndEventKey("N").get(appCode+deprovisionEvent.name());
			if(appDeprovConfig!=null)
				//current time - dayslimit (so that scheduler should execute it now) + 10 minutes delay buffer
				return DateTime.now().minusDays(appDeprovConfig.daysLimitInt()).plusMinutes(10);
		} 
		return DateTime.now(); //default implementations;
				
	}
	
	private Boolean isProductUnAssignedFromEnterprise(DeprovisionEventEnum deprovisionEvent) {
		return DeprovisionEventEnum.PRODUCT_UNASSIGNED_FROM_ENTERPRISE.equals(deprovisionEvent);
	}
	
	private Long parseID(Object id){
		String strId = (String) id;
		return Long.parseLong(strId);
	}
	
	/**
	 * Returns true if the event has been handled else returns false
	 * @param event
	 * @return
	 */
	@Transactional
	private boolean handleNonDeprovisioningEvents(Event event){
		if(event.getEventName().equals("SUSPENDED_ENTERPRISE_REACTIVATED") ||
				event.getEventName().equals("ENTERPRISE_NEW_SUBSCRIPTION")){
			logger.info("Deprovisioning Cancellation Request received for Enterprise.");
			this.checkAndUnblockAppForEnterprise(event);
			this.cancelDeprovisioningRequest(event.getEventParameters());			
			return true;
		}
		if(event.getEventName().equals("PRODUCT_REASSIGNED_TO_USER")){
			logger.info("Deprovisioning Cancellation Request received for End-User(s).");
			this.checkAndUnblockAppForUser(event);
			this.cancelDeprovisionRequestsForEndUser(event.getEventParameters());			
			return true;
		}
		return false;
	}
	
	@Transactional
	private void cancelDeprovisioningRequest(Map<String, Object> eventParams){		 
		Long customerId = parseID(eventParams.get("CUSTOMER_ID"));
		Long productId = parseID(eventParams.get("PRODUCT_ID"));
		// FIND BY customerId, productId, EventName(PRODUCT_UNASSIGNED_FROM_ENTERPRISE), Status(RECEIVED)
		List<DeprovisioningRequest>  existingDeprovRequests = deprovRequestRepository.findByCustomerIdAndProductIdAndStatus(
				customerId, productId, DeprovisionStatusEnum.RECEIVED);
		if(CollectionUtils.isEmpty(existingDeprovRequests)){
			logger.info("No De-Provisioning requests were found for Customer ID:"+customerId+" and Product ID:"+productId+ " for cancellation.");
			return;
		}
		
		for(DeprovisioningRequest deproRequest: existingDeprovRequests){
			deproRequest.setStatus(DeprovisionStatusEnum.CANCELLED);
			deproRequest.setLastModifiedDate(DateTime.now());
			deproRequest.setLastModifiedBy("SYSTEM");
			List<DeprovAppRequest> deprovAppRequestList =  deproRequest.getAppRequests();
			for(DeprovAppRequest appDeprovRequest: deprovAppRequestList){
				appDeprovRequest.setStatus(DeprovisionStatusEnum.CANCELLED);
				appDeprovRequest.setLastModifiedDate(deproRequest.getLastModifiedDate());
				appDeprovRequest.setLastModifiedBy(deproRequest.getLastModifiedBy());
			}
		}
		
		deprovRequestRepository.save(existingDeprovRequests);
	}
	
	@Transactional
	private void cancelDeprovisionRequestsForEndUser(Map<String, Object> eventParams){
		System.out.println("cancelAndPersistDeprovisionRequests(): Is Transaction Running-->"+TransactionSynchronizationManager.isActualTransactionActive());
		
		Long productId = parseID(eventParams.get("PRODUCT_ID"));
		Long subscriptionId = parseID(eventParams.get("SUBSCRIPTION_ID"));
		String targetId = (String)eventParams.get("TARGET_ID");
		List<DeprovisioningRequest> deprovReqList = deprovRequestRepository.findBySubscriptionIdAndProductIdAndTargetIdAndStatus(
				subscriptionId, productId, targetId, DeprovisionStatusEnum.RECEIVED);		
		for(DeprovisioningRequest deprovReq: deprovReqList){
			deprovReq.setStatus(DeprovisionStatusEnum.CANCELLED);
			deprovReq.setLastModifiedDate(DateTime.now());
			List<DeprovAppRequest> deproAppReqList = deprovReq.getAppRequests();
			for(DeprovAppRequest deproAppReq: deproAppReqList){
				deproAppReq.setStatus(DeprovisionStatusEnum.CANCELLED);
				deproAppReq.setLastModifiedDate(deprovReq.getLastModifiedDate());
			}
		}
		deprovRequestRepository.save(deprovReqList);
	}

	
	
	@Transactional
	public void initiateDeprovisioning(){
		
		//List<DeprovisioningRequest> deprovRequests = deprovRequestRepository.findByStatus(DeprovisionStatusEnum.RECEIVED);
		List<DeprovisionStatusEnum> statusList = new ArrayList<>();
		statusList.add(DeprovisionStatusEnum.RECEIVED);statusList.add(DeprovisionStatusEnum.IN_PROCESS);
		List<DeprovisioningRequest> deprovRequests = deprovRequestRepository.findByMultipleStatus(statusList);
		if(CollectionUtils.isEmpty(deprovRequests)){
			System.out.println("No De-Provisioning requests found for Processing....Exiting....");
			return;
		}		
		
		long keepAliveTime = 10;
		int maxAllowedTasks = 50;
		
		ThreadFactory threadFactory = Executors.defaultThreadFactory();
		ThreadPoolExecutor executorPool = new ThreadPoolExecutor(2, 4, keepAliveTime, TimeUnit.SECONDS,
				new ArrayBlockingQueue<Runnable>(maxAllowedTasks), threadFactory, new RejectedExecutionHandler() {
			
			@Override
			public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
				logger.debug(r.toString()+" Inside Execution Handler....");		
			}
		});		
		
		Map<String, AppDeprovisionConfig> appDeprovConfigMap = getAppDeprovisionConfigMapByAppCodeAndEventKey("N");
		for(DeprovisioningRequest deprovRequest: deprovRequests){
			Event event = this.getDeprovisioningEvent(deprovRequest);
			List<DeprovAppRequest> deprovAppRequestList =  deprovRequest.getAppRequests();
			for(DeprovAppRequest deprovAppRequest: deprovAppRequestList){
				AppDeprovisionConfig appDeprovConfig = appDeprovConfigMap.get(deprovAppRequest.getAppCode()+deprovAppRequest.getEvent().name());
				if(appDeprovConfig!=null){
					if(deprovAppRequest.getAppCode().equals("MAIL")){
						addAllActiveEmployeesMails(event.getEventParameters());
					}
					AppDeprovTask task = new AppDeprovTask(deprovAppRequest, appDeprovConfig, event, true);
					executorPool.execute(task);				
				}
			}
		}
		
		executorPool.shutdown();
        while (!executorPool.isTerminated()) {
        }

        syncAndSaveDeprovisioningFinalStatus(deprovRequests);
        
        
        System.out.println("Completed De-Provisioning on:"+DateTime.now());
	}	
	
	/*@Transactional
	private void syncAndSaveDeprovisioningFinalStatus(List<DeprovisioningRequest> deprovRequests){
		for(DeprovisioningRequest deprovRequest: deprovRequests){
			if(deprovRequest.getStatus().equals(DeprovisionStatusEnum.IN_PROCESS)){
				List<DeprovAppRequest> deprovAppRequestList =  deprovRequest.getAppRequests();
				boolean allRequestsSuccessful = true;
				boolean allRequestsUnSuccessful = true;
				boolean partiallyCompleted = false;
				for(DeprovAppRequest deprovAppRequest: deprovAppRequestList){
					if(!deprovAppRequest.getStatus().equals(DeprovisionStatusEnum.COMPLETED)){
						allRequestsSuccessful = false;
						break;
					}
				}
				for(DeprovAppRequest deprovAppRequest: deprovAppRequestList){
					if(!deprovAppRequest.getStatus().equals(DeprovisionStatusEnum.EMC_ERROR)){
						if(deprovAppRequest.getStatus().equals(DeprovisionStatusEnum.COMPLETED) && !partiallyCompleted){
							partiallyCompleted = true;
						}
						allRequestsUnSuccessful = false;
						break;
					}
				}
				if(allRequestsSuccessful){
					deprovRequest.setStatus(DeprovisionStatusEnum.COMPLETED);
				}else if(allRequestsUnSuccessful){
					deprovRequest.setStatus(DeprovisionStatusEnum.EMC_ERROR);
				}else if(partiallyCompleted){
					deprovRequest.setStatus(DeprovisionStatusEnum.PARTIALLY_COMPLETED);
				}else if(){
					// Do Nothing as Status will remain IN_PROCESS for deprovRequest.
				}
				deprovRequest.setProcessingDate(DateTime.now());
				deprovRequestRepository.save(deprovRequest);
				if(deprovRequest.getEvent().equals(DeprovisionEventEnum.ENTERPRISE_SUSPENDED) && deprovRequest.getStatus().equals(DeprovisionStatusEnum.COMPLETED)){
					try{
						customerService.delete(deprovRequest.getCustomerId());
					}catch(Exception ex){
						logger.info("Error happened while Deleting(Inactivating) the Customer with ID:"+deprovRequest.getCustomerId().toString()
								+" 60 days post its suspension. Deprovisioning Request ID="+deprovRequest.getId().toString());
						ex.printStackTrace();
					}
				}
			}	
		}
	}*/
	
	@Transactional
	private void syncAndSaveDeprovisioningFinalStatus(List<DeprovisioningRequest> deprovRequests){
		for(DeprovisioningRequest deprovRequest: deprovRequests){
			syncAndSaveDeprovisioningFinalStatus(deprovRequest);
		}
	}
	
	@Transactional
	private void syncAndSaveDeprovisioningFinalStatus(DeprovisioningRequest deprovRequest){
		if(deprovRequest.getStatus().equals(DeprovisionStatusEnum.RECEIVED) || 
				deprovRequest.getStatus().equals(DeprovisionStatusEnum.IN_PROCESS)){
			List<DeprovAppRequest> deprovAppRequestList =  deprovRequest.getAppRequests();
			int completedCount = 0;
			int errorCount = 0;
			int notProcessedCount = 0;
			final int totalRequests = deprovAppRequestList.size();
			for(DeprovAppRequest deprovAppRequest: deprovAppRequestList){
				if(deprovAppRequest.getStatus().equals(DeprovisionStatusEnum.COMPLETED)){
					++completedCount;
				}
				else if(deprovAppRequest.getStatus().equals(DeprovisionStatusEnum.EMC_ERROR)){
					++errorCount;
				}else if(deprovAppRequest.getStatus().equals(DeprovisionStatusEnum.RECEIVED)){
					++notProcessedCount;
				}
			}
			
			if(completedCount==totalRequests){
				deprovRequest.setStatus(DeprovisionStatusEnum.COMPLETED);				
			}else if(errorCount==totalRequests){
				deprovRequest.setStatus(DeprovisionStatusEnum.EMC_ERROR);
			}else if(notProcessedCount==totalRequests){
				deprovRequest.setStatus(DeprovisionStatusEnum.RECEIVED);
			}else if((completedCount+errorCount)==totalRequests){
				deprovRequest.setStatus(DeprovisionStatusEnum.PARTIALLY_COMPLETED);
			}else if((completedCount+errorCount+notProcessedCount)==totalRequests){
				deprovRequest.setStatus(DeprovisionStatusEnum.IN_PROCESS);
			}else{
				logger.info("Execution shouldn't reach here inside Ghuiyon ke khet me !");
			}

			if(!deprovRequest.getStatus().equals(DeprovisionStatusEnum.RECEIVED)){
				deprovRequest.setProcessingDate(DateTime.now());
				deprovRequest.setLastModifiedDate(deprovRequest.getProcessingDate());				
			}
			deprovRequestRepository.save(deprovRequest);
			
			if(deprovRequest.getEvent().equals(DeprovisionEventEnum.ENTERPRISE_SUSPENDED) && deprovRequest.getStatus().equals(DeprovisionStatusEnum.COMPLETED)){
				try{
					customerService.delete(deprovRequest.getCustomerId());
				}catch(Exception ex){
					logger.info("Error happened while Deleting(Inactivating) the Customer with ID:"+deprovRequest.getCustomerId().toString()
							+" 60 days post its suspension. Deprovisioning Request ID="+deprovRequest.getId().toString());
					ex.printStackTrace();
					logger.info("Aapki request gayi Ghuiyon ke khet me ! Sahi pakde hain !");
				}
			}
		}	
	}

	
	
	private class AppDeprovTask implements Runnable{

		AppDeprovTask(DeprovAppRequest deprovAppRequest, AppDeprovisionConfig appDeprovConfig, Event event, boolean completeEventProcessing){
			this.deprovAppRequest = deprovAppRequest;
			this.appDeprovConfig = appDeprovConfig;
			this.event = event;
			this.completeEventProcessing = completeEventProcessing;
		}

		private DeprovAppRequest deprovAppRequest;
		private AppDeprovisionConfig appDeprovConfig;
		private Event event;
		private boolean completeEventProcessing;
		
		@Override 
		public void run() {
			if(deprovAppRequest.getStatus().equals(DeprovisionStatusEnum.RECEIVED) && 
					(hasWaitLimitPassed() || appDeprovConfig.getImmediateExecutionFlag().equals("Y"))){
				deprovAppRequest.setLastModifiedDate(DateTime.now());
				if(completeEventProcessing)
					deprovAppRequest.setProcessingDate(DateTime.now());
				
				String link =  getLink(appDeprovConfig.getCallback(), event.getEventParameters());
				String token = null;
				Response response = null;
				try {
					token = EMECOAuthUtils.getAccessToken();
				} catch (EMECOAuthException e) {
					e.printStackTrace();
				}
		        if(StringUtils.isEmpty(token)){
		        	if(completeEventProcessing)
		        		deprovAppRequest.setStatus(DeprovisionStatusEnum.EMC_ERROR);
		        	System.out.println("Access token is not received from authorization server. Returning without issuing call to EMEC Provisioning Service.");
		        	return;
		        }
		        
		        String isSecuredUrl = appDeprovConfig.getIsSecuredUrl();
		        try {
			        if(null != isSecuredUrl && isSecuredUrl.equals("Y")){
			        	
							response = sslClient.target(link).request().header("Authorization", "bearer "+token)
									.accept(MediaType.APPLICATION_JSON)
									.post(Entity.entity(JsonUtil.toJSON(event),MediaType.APPLICATION_JSON));
						}
			        else{
			        	response = client.target(link).request().header("Authorization", "bearer "+token)
								.accept(MediaType.APPLICATION_JSON)
								.post(Entity.entity(JsonUtil.toJSON(event),MediaType.APPLICATION_JSON));
			        }
			        System.out.println("De-Provision: Response returned -->"+response);
		        } catch (IOException e) {
		        	if(completeEventProcessing)
		        		deprovAppRequest.setStatus(DeprovisionStatusEnum.EMC_ERROR);
		        	System.out.println("De-Provision: Exception thrown while hitting REST -->"+response);
					e.printStackTrace();
					return;
				}
		        
		        if(null == response || 	response.getStatus() != Response.Status.OK.getStatusCode()){
		        	System.out.println("Failed to De-Provision application :: "+deprovAppRequest.getAppCode());
		        	if(completeEventProcessing)
		        		deprovAppRequest.setStatus(DeprovisionStatusEnum.EMC_ERROR);
		        	return;
		        }
		        if(completeEventProcessing)
		        	deprovAppRequest.setStatus(DeprovisionStatusEnum.COMPLETED);
			}
		}
		
		private boolean hasWaitLimitPassed(){
			//logger.info("Processing DeprovAppRequest#"+deprovAppRequest.getId()+" App Code="+deprovAppRequest.getAppCode());
			DateTime submittedDate = deprovAppRequest.getSubmissionDate();
			DateTime todayDate = DateTime.now();
			if(appDeprovConfig!=null){
				return todayDate.isAfter(submittedDate.plusDays(appDeprovConfig.daysLimitInt()));
			}
			return false;
		}
		
		private String getLink(String link, Map<String,Object> eventParameters) {
			/* For De-Provisioning we don't need this substitution otherwise the requested HTTP resource is not found
			StrSubstitutor strSub = new StrSubstitutor(eventParameters);
			return strSub.replace(link);
			*/
			return link;
		}
		
	}
	
	private Map<String, AppDeprovisionConfig> getAppDeprovisionConfigMapByAppCodeAndEventKey(String immediateExecutionFlag){
		List<AppDeprovisionConfig> appDeprovConfigList = appDeprovConfigRepo.findByImmediateExecutionFlag(immediateExecutionFlag);
		Map<String, AppDeprovisionConfig> appDeprovConfigMap = new HashMap<>();
		for(AppDeprovisionConfig appDeprovConfig : appDeprovConfigList){
			appDeprovConfigMap.put(appDeprovConfig.getAppCode()+appDeprovConfig.getEvent().name(), appDeprovConfig);
		}
		return appDeprovConfigMap;
	}
	
	private Event getDeprovisioningEvent(DeprovisioningRequest deproRequest){
		Map<String,Object> eventParameters = new HashMap<>();
		Event event = new Event(deproRequest.getEvent().name(),deproRequest.getEvent().name());
		
		eventParameters.put("SUBSCRIPTION_ID", deproRequest.getSubscriptionId().toString());
		eventParameters.put("TENANT_ID", deproRequest.getTenantId());
		eventParameters.put("DOMAIN_NAME", deproRequest.getDomainName());
		eventParameters.put("PRODUCT_ID", deproRequest.getProductId().toString());
		eventParameters.put("CUSTOMER_ID", deproRequest.getCustomerId().toString());
		eventParameters.put("TARGET_ID", deproRequest.getTargetId());
		eventParameters.put("SME_LOGIN_ID", "admin@"+deproRequest.getDomainName());
		event.setEventParameters(eventParameters);	
		
		return event;
	}
	
	@Transactional
	private void handleImmediateExecutionEvents(String customerName, DeprovisionEventEnum deprovisionEvent, DeprovisioningRequest deprovRequest){
		try{
			Map<String, AppDeprovisionConfig> appDeprovConfigMap = getAppDeprovisionConfigMapByAppCodeAndEventKey("Y");
			Event event = getDeprovisioningEvent(deprovRequest);
			List<String> allowedAppCodeList = new ArrayList<>();
			event.getEventParameters().put("CUSTOMER_NAME", customerName);
			
			switch(deprovisionEvent){
			case  END_USER_DELETED:
				allowedAppCodeList.add("MAIL");
				executeDeprovisioningRequest(deprovRequest, event, appDeprovConfigMap, allowedAppCodeList, false);		
				break;
			case  PRODUCT_UNASSIGNED_FROM_USER:
				allowedAppCodeList.add("MAIL");
				executeDeprovisioningRequest(deprovRequest, event, appDeprovConfigMap, allowedAppCodeList, false);		
				break;
			case ENTERPRISE_DELETED:
				addAllActiveEmployeesMails(event.getEventParameters());
				executeDeprovisioningRequest(deprovRequest, event, appDeprovConfigMap, allowedAppCodeList, true);
				break;
			case ENTERPRISE_SUSPENDED:
				allowedAppCodeList.add("MAIL");allowedAppCodeList.add("WEBUILD");
				addAllActiveEmployeesMails(event.getEventParameters());	
				executeDeprovisioningRequest(deprovRequest, event, appDeprovConfigMap, allowedAppCodeList, false);
				break;
			case PRODUCT_UNASSIGNED_FROM_ENTERPRISE:
				allowedAppCodeList.add("MAIL");allowedAppCodeList.add("WEBUILD");
				addAllActiveEmployeesMails(event.getEventParameters());	
				executeDeprovisioningRequest(deprovRequest, event, appDeprovConfigMap, allowedAppCodeList, false);
				break;
			case TRIAL_EXPIRED_CANCELLED:
				addAllActiveEmployeesMails(event.getEventParameters());
				executeDeprovisioningRequest(deprovRequest, event, appDeprovConfigMap, allowedAppCodeList, true);
				break;
			default : 
				System.out.println("No Matching Case for Immediate Execution");
			
			}
		}catch(Exception ex){
			logger.info("Error occured while handling the Immediate Event:"+deprovisionEvent.toString()+" for Customer ID="+deprovRequest.getCustomerId()+" and TARGET_ID="+deprovRequest.getTargetId());
			ex.printStackTrace();
		}
	}
	
	@Transactional(propagation=Propagation.REQUIRES_NEW)
	private boolean handleImmediateEnterpriseDeletion(Event event, DeprovisionEventEnum deprovisionEvent){
		if(DeprovisionEventEnum.ENTERPRISE_DELETED != deprovisionEvent){
			return false;
		}
		String customerId = (String) event.getEventParameters().get("CUSTOMER_ID");
		String customerName = (String) event.getEventParameters().get("CUSTOMER_NAME");
		try{
			syncEventForEnterpriseDeletion(customerId);
			List<DeprovisionEventEnum> eventList = new ArrayList<>();
			eventList.add(DeprovisionEventEnum.ENTERPRISE_DELETED);
			
			List<DeprovisioningRequest> deprovRequestList = deprovRequestRepository.findByCustomerIdAndStatusAndEvent(parseID(customerId), DeprovisionStatusEnum.RECEIVED, eventList);
			Map<String, AppDeprovisionConfig> appDeprovConfigMap = getAppDeprovisionConfigMapByAppCodeAndEventKey("Y");
			
			switch(deprovisionEvent){
				case ENTERPRISE_DELETED:					
					for(DeprovisioningRequest deprovRequest : deprovRequestList){
						event = getDeprovisioningEvent(deprovRequest);
						event.getEventParameters().put("CUSTOMER_NAME", customerName);
						addAllActiveEmployeesMails(event.getEventParameters());
						executeDeprovisioningRequest(deprovRequest, event, appDeprovConfigMap, null, true);
					}
					break;
				default : 
					logger.info("No Matching Case for Immediate Delete Execution");	
			}
		}catch(Exception ex){
			logger.info("Error occurred during Immediate Deprovisioning of Enterprise ("+customerName+") with ID="+customerId+" Stacktrace:");
			ex.printStackTrace();			
		}
		return true;
	}
	
	@Transactional
	private void syncEventForEnterpriseDeletion(String customerId){
		List<DeprovisionEventEnum> eventList = new ArrayList<>();
		eventList.add(DeprovisionEventEnum.PRODUCT_UNASSIGNED_FROM_ENTERPRISE);		
		List<DeprovisioningRequest> deprovRequestList = deprovRequestRepository.findByCustomerIdAndStatusAndEvent(parseID(customerId), DeprovisionStatusEnum.RECEIVED, eventList);
		for(DeprovisioningRequest deprovRequest : deprovRequestList){
			for(DeprovAppRequest deprovAppRequest: deprovRequest.getAppRequests()){
				deprovAppRequest.setEvent(DeprovisionEventEnum.ENTERPRISE_DELETED);
				deprovAppRequestRepository.save(deprovAppRequest);
			}
			deprovRequest.setEvent(DeprovisionEventEnum.ENTERPRISE_DELETED);
			deprovRequestRepository.save(deprovRequest);
		}
	}
	
	@Transactional
	private void executeDeprovisioningRequest(DeprovisioningRequest deprovRequest, Event event, Map<String, 
			AppDeprovisionConfig> appDeprovConfigMap, List<String> allowedAppCodeList, boolean completeEventProcessing){
		boolean bypassCheck = false;
		if(CollectionUtils.isEmpty(allowedAppCodeList)){
			bypassCheck = true;
		}
		for(DeprovAppRequest deprovAppRequest: deprovRequest.getAppRequests()){
			if(bypassCheck || allowedAppCodeList.contains(deprovAppRequest.getAppCode())){
				AppDeprovisionConfig appDeprovConfig = appDeprovConfigMap.get(deprovAppRequest.getAppCode()+deprovAppRequest.getEvent().name());
				AppDeprovTask task = new AppDeprovTask(deprovAppRequest, appDeprovConfig, event, completeEventProcessing);
				task.run();
			}
		}
		if(completeEventProcessing){
			this.syncAndSaveDeprovisioningFinalStatus(deprovRequest);
		}
	}

	@Transactional
	private void checkAndUnblockAppForEnterprise(Event event){
		Map<String, Object> eventParams = event.getEventParameters();
		LDAPUser customerAdmin = ldapEmployeeService.getAnActiveCustomerAdmin((String) eventParams.get("CUSTOMER_NAME"));
		String targetId = customerAdmin.getAliasName();
		String productId = (String)eventParams.get("PRODUCT_ID");
		List<DeprovisionEventEnum> eventList = new ArrayList<>();
		eventList.add(DeprovisionEventEnum.PRODUCT_UNASSIGNED_FROM_ENTERPRISE);
		eventList.add(DeprovisionEventEnum.ENTERPRISE_SUSPENDED);
		List<DeprovAppRequest> deprovAppRequests = deprovAppRequestRepository.findByTargetIdAndProductIdAndStatusAndEventList(targetId, 
				parseID(productId),	DeprovisionStatusEnum.RECEIVED, eventList);
		event.getEventParameters().put("TARGET_ID", targetId);
		startUnblocking(event, deprovAppRequests, true);
	}
	
	@Transactional
	private void checkAndUnblockAppForUser(Event event){
		Map<String, Object> eventParams = event.getEventParameters();
		String targetId = (String)eventParams.get("TARGET_ID");
		String productId = (String)eventParams.get("PRODUCT_ID");
		List<DeprovAppRequest> deprovAppRequests = deprovAppRequestRepository.findByTargetIdAndProductIdAndEventAndStatus(targetId, 
				parseID(productId), DeprovisionEventEnum.PRODUCT_UNASSIGNED_FROM_USER, DeprovisionStatusEnum.RECEIVED);
		startUnblocking(event, deprovAppRequests, false);
	}
	
	@Transactional
	private void startUnblocking(Event event, List<DeprovAppRequest> deprovAppRequests, boolean isEnterpriseTypeEvent){
		if(CollectionUtils.isNotEmpty(deprovAppRequests)){
			Map<String, AppDeprovisionConfig> appDeprovConfigMap = getAppDeprovisionConfigMapByAppCodeAndEventKey("Y");
			for(DeprovAppRequest deprovAppRequest: deprovAppRequests){
				if(deprovAppRequest.getAppCode().equals("MAIL")){
					if(isEnterpriseTypeEvent)
						addAllActiveEmployeesMails(event.getEventParameters());
					issueUnblockingProvisionRequest(event, deprovAppRequest, appDeprovConfigMap);
				}else if(deprovAppRequest.getAppCode().equals("WEBUILD") && isEnterpriseTypeEvent){
					event.getEventParameters().put("SME_LOGIN_ID", "admin@"+(String) event.getEventParameters().get("DOMAIN_NAME"));
					issueUnblockingProvisionRequest(event, deprovAppRequest, appDeprovConfigMap);
				}
			}
		}
	}
	
	@Transactional
	private void issueUnblockingProvisionRequest(Event event, DeprovAppRequest deprovAppRequest, Map<String, AppDeprovisionConfig> appDeprovConfigMap){
		AppDeprovisionConfig appDeprovConfig = appDeprovConfigMap.get(deprovAppRequest.getAppCode()+event.getEventName());
		if(appDeprovConfig!=null)
		{
			Provision provisionMail = new Provision(event, appDeprovConfig);
			Thread t1 = new Thread(provisionMail);
			t1.start();
		}
	}
	
	private boolean wasImmediatelyExecuted(DeprovAppRequest deprovAppRequest){
		DateTime submittedDate = deprovAppRequest.getSubmissionDate();
		DateTime processingDate = deprovAppRequest.getProcessingDate();
		return processingDate.isBefore(submittedDate.plusDays(1));
	}
	
	private void addAllActiveEmployeesMails(Map<String, Object> eventParams){
		//String customerName = (String) eventParams.get("CUSTOMER_NAME");
		String tenantId = (String) eventParams.get("TENANT_ID");
		Long productId = parseID(eventParams.get("PRODUCT_ID"));
		Long subscriptionId = parseID(eventParams.get("SUBSCRIPTION_ID"));
		
		/*
		if(StringUtils.isEmpty(customerName)){
			customerName = customerService.getCustomerByTenantId(tenantId).getName();
		}
		List<String> emailList = ldapEmployeeService.findAllEmployeesEmails(customerName); */
		//List<EndUserAccess> endUserAccessList = endUserAccessRepo.getUsersByCustomerandproductid(productId, tenantId);
		List<EndUserAccess> endUserAccessList = endUserAccessRepo.findByTenantIdAndProductidAndSubscriptionid(tenantId, productId, subscriptionId);
		List<String> emailList = new ArrayList<>();
		for(EndUserAccess ea: endUserAccessList)
		{
			emailList.add(ea.getEmailId());
		}
		eventParams.put("EMP_MAIL_LIST", emailList);
	}
	
	private class Provision implements Runnable{

		Provision(Event event, AppDeprovisionConfig appDeprovConfig){
			this.event = event;
			this.appDeprovConfig = appDeprovConfig;
		}
		
		private AppDeprovisionConfig appDeprovConfig;
		private Event event;

		private String getLink(String link, Map<String,Object> eventParameters) {
			/* For De-Provisioning we don't need this substitution otherwise the requested HTTP resource is not found
			StrSubstitutor strSub = new StrSubstitutor(eventParameters);
			return strSub.replace(link);
			*/
			return link;
		}
		
		@Override
		public void run() {
			String link =  getLink(appDeprovConfig.getCallback(), event.getEventParameters());
			String token = null;
			Response response = null;
			try {
				token = EMECOAuthUtils.getAccessToken();
			} catch (EMECOAuthException e) {
				e.printStackTrace();
			}
	        if(StringUtils.isEmpty(token)){
	        	System.out.println("Access token is not received from authorization server. Returning without issuing call to EMEC Provisioning Service.");
	        	return;
	        }
	        
	        String isSecuredUrl = appDeprovConfig.getIsSecuredUrl();
	        try {
		        if(null != isSecuredUrl && isSecuredUrl.equals("Y")){
		        	
						response = sslClient.target(link).request().header("Authorization", "bearer "+token)
								.accept(MediaType.APPLICATION_JSON)
								.post(Entity.entity(JsonUtil.toJSON(event),MediaType.APPLICATION_JSON));
					}
		        else{
		        	response = client.target(link).request().header("Authorization", "bearer "+token)
							.accept(MediaType.APPLICATION_JSON)
							.post(Entity.entity(JsonUtil.toJSON(event),MediaType.APPLICATION_JSON));
		        }
		        System.out.println("Re-Provision: Response returned -->"+response);
	        } catch (IOException e) {
	        	System.out.println("Re-Provision: Exception thrown while hitting REST -->"+response);
				e.printStackTrace();
				return;
			}
	        
	        if(null == response || 	response.getStatus() != Response.Status.OK.getStatusCode()){
	        	System.out.println("Re-Provisioning Request Failed :: ");
	        	return;
	        }
		}
		
	}
	
	
	@PostConstruct
	public void initialize(){
    	SslConfigurator sslConfig = SslConfigurator.newInstance()
		        .trustStoreFile(trustStoreFile)
		        .trustStorePassword(trustStorePassword)
		        .keyStoreFile(keyStoreFile)
		        .keyPassword(keyPassword);
    	
    	SSLContext sslContext = sslConfig.createSSLContext();
    	sslClient = ClientBuilder.newBuilder().sslContext(sslContext).build();	
		
	}
	
	@Override
	public Set<String> getTenantIdsForDeprovisionedSuspendedCustomers(){
		Set<String> tenantSet = new HashSet<>();
		List<DeprovisioningRequest> deprovRequests = deprovRequestRepository.findByEventAndStatus(DeprovisionEventEnum.ENTERPRISE_SUSPENDED, DeprovisionStatusEnum.COMPLETED);
		for(DeprovisioningRequest deprovReq: deprovRequests){
			tenantSet.add(deprovReq.getTenantId());
		}
		return tenantSet;
	}
}