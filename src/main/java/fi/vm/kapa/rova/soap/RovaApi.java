package fi.vm.kapa.rova.soap;

import java.util.List;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.jws.WebService;
import javax.xml.ws.Holder;
import javax.xml.ws.WebServiceContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;

import fi.vm.kapa.rova.soap.providers.DataProvider;
import fi.vm.kapa.xml.rova.api.AuthorizationType;
import fi.vm.kapa.xml.rova.api.DecisionReasonType;
import fi.vm.kapa.xml.rova.api.ObjectFactory;
import fi.vm.kapa.xml.rova.api.Principal;
import fi.vm.kapa.xml.rova.api.Request;
import fi.vm.kapa.xml.rova.api.RovaPortType;

@WebService(endpointInterface = "fi.vm.kapa.xml.rova.api.RovaPortType")
@Component("rovaService")
public class RovaApi extends SpringBeanAutowiringSupport implements RovaPortType {
	Logger LOG = Logger.getLogger(RovaApi.class.toString());
	
	@Resource
	WebServiceContext context;
	
	private ObjectFactory factory = new ObjectFactory();
	
	@Autowired
	private DataProvider dataProvider;
	
	@PostConstruct
	public void init() {
		SpringBeanAutowiringSupport.processInjectionBasedOnCurrentContext(this);
//		LOG.info("Rova api using " + dataProvider);
	}
	
	public void rovaService(String delegateIdentifier, String industry,
			String service, String issue, String principalIdentifier, String endUserIdentifier,
			Holder<Request> rovaServiceRequest,
			Holder<Principal> principalResponse,
			Holder<AuthorizationType> delegationResponse,
			Holder<List<DecisionReasonType>> reason) {
	
		rovaServiceRequest.value = factory.createRequest();
		rovaServiceRequest.value.setDelegateIdentifier(delegateIdentifier);
		rovaServiceRequest.value.setIndustry(industry);
		rovaServiceRequest.value.setIssue(issue);
		rovaServiceRequest.value.setPrincipalIdentifier(principalIdentifier);
		rovaServiceRequest.value.setService(service);
		rovaServiceRequest.value.setEndUserIdentifier(endUserIdentifier);
		if (delegateIdentifier != null && principalIdentifier == null) {
			principalResponse.value = getPrincipalResponse(delegateIdentifier, service, reason);
		} else if (principalIdentifier != null) {
			delegationResponse.value = getAuthResponse(delegateIdentifier, principalIdentifier, service, reason);
		}
	}
	
	private Principal getPrincipalResponse(String hetu, String service, Holder<List<DecisionReasonType>> reason) {
		Principal p = dataProvider.getPrincipalResponse(hetu, null, service, null, null, reason);
//		LOG.info("response="+ p);
		return p;
	}

	private AuthorizationType getAuthResponse(String hetu, String target, String service, Holder<List<DecisionReasonType>> reason) {
		AuthorizationType response = dataProvider.getAuthorizationTypeResponse(hetu, target, null, service, null, null, reason);
//		LOG.info("response="+ response.value());
		return response;
	}

}
