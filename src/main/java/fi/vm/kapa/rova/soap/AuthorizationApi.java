package fi.vm.kapa.rova.soap;

import java.util.List;
import java.util.logging.Logger;

import javax.jws.WebService;
import javax.xml.ws.Holder;

import org.springframework.stereotype.Component;

import fi.vm.kapa.xml.rova.api.authorization.AuthorizationType;
import fi.vm.kapa.xml.rova.api.authorization.DecisionReasonType;
import fi.vm.kapa.xml.rova.api.authorization.ObjectFactory;
import fi.vm.kapa.xml.rova.api.authorization.Request;
import fi.vm.kapa.xml.rova.api.authorization.RovaAuthorizationPortType;

@WebService(endpointInterface = "fi.vm.kapa.xml.rova.api.authorization.RovaAuthorizationPortType")
@Component("rovaAuthorizationService")
public class AuthorizationApi extends AbstractSoapService implements
		RovaAuthorizationPortType {
	Logger LOG = Logger.getLogger(AuthorizationApi.class.toString());

	private ObjectFactory factory = new ObjectFactory();

	@Override
	public void rovaAuthorizationService(String delegateIdentifier,
			String principalIdentifier,
			Holder<Request> rovaAuthorizationServiceRequest,
			Holder<AuthorizationType> authorizationResponse,
			Holder<List<DecisionReasonType>> reason) {

		rovaAuthorizationServiceRequest.value = factory.createRequest();
		rovaAuthorizationServiceRequest.value.setDelegateIdentifier(delegateIdentifier);
		rovaAuthorizationServiceRequest.value.setPrincipalIdentifier(principalIdentifier);

		dataProvider.handleAuthorizationTypeResponse(delegateIdentifier, principalIdentifier, getService(), getEndUserId(), 
				getRequestId(), authorizationResponse, reason);
	}

	private String getEndUserId() {
		return getHeaderValue(factory.createUserId("").getName());
	}
	
	private String getRequestId() {
		return getHeaderValue(factory.createId("").getName());
	}
	
	private String getService() {
		return getClientHeaderValue(factory.createClient(factory.createSdsbClientIdentifierType()).getName());
	}
}
