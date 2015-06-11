package fi.vm.kapa.rova.soap;

import javax.jws.WebService;
import javax.xml.ws.Holder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import fi.vm.kapa.xml.rova.api.authorization.ObjectFactory;
import fi.vm.kapa.xml.rova.api.authorization.Request;
import fi.vm.kapa.xml.rova.api.authorization.RovaAuthorizationPortType;
import fi.vm.kapa.xml.rova.api.authorization.RovaAuthorizationResponse;

@WebService(endpointInterface = "fi.vm.kapa.xml.rova.api.authorization.RovaAuthorizationPortType")
@Component("rovaAuthorizationService")
public class AuthorizationApi extends AbstractSoapService implements
		RovaAuthorizationPortType {
	Logger LOG = LoggerFactory.getLogger(AuthorizationApi.class);

	private ObjectFactory factory = new ObjectFactory();

	@Override
	public void rovaAuthorizationService(Holder<Request> request,
			Holder<RovaAuthorizationResponse> response) {
		dataProvider.handleAuthorization(request.value.getDelegateIdentifier(),
				request.value.getPrincipalIdentifier(), getService(),
				getEndUserId(), getRequestId(), response);
	}

	private String getEndUserId() {
		return getHeaderValue(factory.createUserId("").getName());
	}

	private String getRequestId() {
		return getHeaderValue(factory.createId("").getName());
	}

	private String getService() {
		return getClientHeaderValue(factory.createClient(
				factory.createSdsbClientIdentifierType()).getName());
	}
}
