package fi.vm.kapa.rova.soap.providers;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.ws.Holder;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.proxy.WebResourceFactory;
import org.glassfish.jersey.jackson.JacksonFeature;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import fi.vm.kapa.rova.config.SpringProperties;
import fi.vm.kapa.rova.engine.model.Authorization;
import fi.vm.kapa.rova.engine.model.DecisionReason;
import fi.vm.kapa.rova.engine.model.Delegate;
import fi.vm.kapa.rova.engine.resources.EngineResource;
import fi.vm.kapa.rova.logging.Logger;
import fi.vm.kapa.rova.logging.LoggingClientRequestFilter;
import fi.vm.kapa.rova.rest.validation.ValidationClientRequestFilter;
import fi.vm.kapa.xml.rova.api.authorization.AuthorizationType;
import fi.vm.kapa.xml.rova.api.authorization.DecisionReasonType;
import fi.vm.kapa.xml.rova.api.authorization.RovaAuthorizationResponse;
import fi.vm.kapa.xml.rova.api.delegate.Principal;
import fi.vm.kapa.xml.rova.api.delegate.PrincipalType;

@Component
public class EngineDataProvider implements DataProvider, SpringProperties {
	Logger LOG = Logger.getLogger(EngineDataProvider.class, Logger.SOAP_SERVICE);

	EngineResource engineResource = null;
	private fi.vm.kapa.xml.rova.api.authorization.ObjectFactory authorizationFactory = new fi.vm.kapa.xml.rova.api.authorization.ObjectFactory();
	private fi.vm.kapa.xml.rova.api.delegate.ObjectFactory delegateFactory = new fi.vm.kapa.xml.rova.api.delegate.ObjectFactory();

	@Value(ENGINE_URL)
	private String engineUrl;

	@Value(ENGINE_API_KEY)
	private String engineApiKey;

	@Value(REQUEST_ALIVE_SECONDS)
	private Integer requestAliveSeconds;

	@PostConstruct
	public void init() {
		ClientConfig cc = new ClientConfig().register(JacksonFeature.class);
		Client resource = ClientBuilder.newClient(cc);
		engineResource = WebResourceFactory.newResource(EngineResource.class, resource.target(engineUrl));
	}

	@Override
	public void handleDelegate(String personId, String service, String endUserId, String requestId,
			Holder<fi.vm.kapa.xml.rova.api.delegate.Response> delegateResponse) {
		LOG.debug("handleDelegate("+ personId +", "+ service +", "+ endUserId +", "+ requestId +")");

		WebTarget webTarget = getClient().target(engineUrl + "delegate" + "/" + service + "/" 
				+ endUserId + "/" + personId).queryParam("requestId", requestId);

		Invocation.Builder invocationBuilder = webTarget.request(MediaType.APPLICATION_JSON);
		
		Response response = invocationBuilder.get();

		if (response.getStatus() == HttpStatus.OK.value()) {
			Delegate delegate = response.readEntity(Delegate.class);
			if (delegateResponse.value == null) {
				delegateResponse.value = delegateFactory.createResponse();
			}
			Principal principal = delegateFactory.createPrincipal();

			List<PrincipalType> principals = principal.getPrincipal();
			for (fi.vm.kapa.rova.engine.model.Principal modelP : delegate.getPrincipal()) {
				PrincipalType current = delegateFactory.createPrincipalType();
				current.setIdentifier(modelP.getPersonId());
				current.setName(modelP.getName());
				principals.add(current);
			}
			delegateResponse.value.setPrincipalList(principal);
			
			List<DecisionReason> reasons = delegate.getReasons();
			if (reasons != null) {
				List<fi.vm.kapa.xml.rova.api.delegate.DecisionReasonType> reason = delegateResponse.value.getReason();
				for (DecisionReason dr : reasons) {
					fi.vm.kapa.xml.rova.api.delegate.DecisionReasonType drt = new fi.vm.kapa.xml.rova.api.delegate.DecisionReasonType();
					drt.setRule(dr.getReasonRule());
					drt.setValue(dr.getReasonValue());
					reason.add(drt);
				}
			}

			if (delegate.getAuthorizationType() != null) {
				delegateResponse.value.setAuthorization(
						fi.vm.kapa.xml.rova.api.delegate.AuthorizationType.valueOf(delegate.getAuthorizationType().toString()));
			}
		} else {
			// TODO handle error response
			LOG.error("Got error response from engine");
		}

	}

	public void handleAuthorization(String delegateId, String principalId, String service, 
			String endUserId, String requestId, Holder<RovaAuthorizationResponse> authorizationResponseHolder) {
		LOG.debug("handleAuthorization("+ delegateId +", "+ principalId +", "+ service +", "+ endUserId +", "+ requestId +")");

		WebTarget webTarget = getClient().target(engineUrl + "authorization" + "/" + service 
				+ "/" + endUserId + "/" + delegateId + "/" + principalId).queryParam("requestId", requestId);

		Invocation.Builder invocationBuilder = webTarget.request(MediaType.APPLICATION_JSON);

		Response response = invocationBuilder.get();

		if (response.getStatus() == HttpStatus.OK.value()) {
			Authorization auth = response.readEntity(Authorization.class);

			authorizationResponseHolder.value = authorizationFactory.createRovaAuthorizationResponse();
			authorizationResponseHolder.value.setAuthorization(AuthorizationType.fromValue(auth.getResult().toString()));
	
			if (auth.getReasons() != null) {
				for (DecisionReason dr : auth.getReasons()) {
					DecisionReasonType drt = new DecisionReasonType();
					drt.setRule(dr.getReasonRule());
					drt.setValue(dr.getReasonValue());
					authorizationResponseHolder.value.getReason().add(drt);
				}
			}
		} else {
			// TODO handle error response
			LOG.error("Got error response from engine");
		}
	}

	private Client getClient() {
		ClientConfig clientConfig = new ClientConfig();
		Client client = ClientBuilder.newClient(clientConfig);
		client.register(JacksonFeature.class);
		client.register(new ValidationClientRequestFilter(engineApiKey, requestAliveSeconds, null));
		client.register(new LoggingClientRequestFilter());
		return client;
	}

	@Override
	public String toString() {
		return "EngineDataProvider engine url: " + engineUrl;
	}

}
