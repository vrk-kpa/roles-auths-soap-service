package fi.vm.kapa.rova.soap.providers;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

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
import org.springframework.stereotype.Component;

import fi.vm.kapa.rova.config.SpringProperties;
import fi.vm.kapa.rova.engine.model.Authorization;
import fi.vm.kapa.rova.engine.model.DecisionReason;
import fi.vm.kapa.rova.engine.model.Delegate;
import fi.vm.kapa.rova.engine.resources.EngineResource;
import fi.vm.kapa.rova.rest.validation.ValidationClientRequestFilter;
import fi.vm.kapa.xml.rova.api.authorization.AuthorizationType;
import fi.vm.kapa.xml.rova.api.authorization.DecisionReasonType;
import fi.vm.kapa.xml.rova.api.delegate.AuthorizationResponseType;
import fi.vm.kapa.xml.rova.api.delegate.Principal;
import fi.vm.kapa.xml.rova.api.delegate.PrincipalType;

@Component
public class EngineDataProvider implements DataProvider, SpringProperties {
	Logger LOG = Logger.getLogger(EngineDataProvider.class.toString());

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
		engineResource = WebResourceFactory.newResource(EngineResource.class,
				resource.target(engineUrl));
	}

	public void handleAuthorizationTypeResponse(String delegateId,
			String principalId, String service, String endUserId,
			String requestId,
			Holder<AuthorizationType> authorizationTypeResponse,
			Holder<List<DecisionReasonType>> reason) {

		WebTarget webTarget = getClient().target(
				engineUrl + "authorization" + "/" + service + "/" + endUserId
						+ "/" + delegateId + "/" + principalId);

		Invocation.Builder invocationBuilder = webTarget
				.request(MediaType.APPLICATION_JSON);
		Response response = invocationBuilder.get();
		Authorization auth = response.readEntity(Authorization.class);

		authorizationTypeResponse.value =   AuthorizationType.fromValue(auth.getResult().toString());

		if (auth.getReasons() != null) {
			addReasons(auth.getReasons(), reason);
		}
	}

	public void handlePrincipalResponse(String personId, String service,
			String endUserId, String requestId, Holder<Principal> principal,
			Holder<AuthorizationResponseType> authorizationResponseType) {

		WebTarget webTarget = getClient().target(
				engineUrl + "delegate" + "/" + service + "/" + endUserId + "/"
						+ personId);

		Invocation.Builder invocationBuilder = webTarget
				.request(MediaType.APPLICATION_JSON);
		Response response = invocationBuilder.get();
		Delegate delegate = response.readEntity(Delegate.class);

		principal.value = delegateFactory.createPrincipal();
		List<PrincipalType> principals = principal.value.getPrincipalElem();
		for (fi.vm.kapa.rova.engine.model.Principal modelP : delegate.getPrincipal()) {
			PrincipalType current = delegateFactory.createPrincipalType();
			current.setTargetIdentifier(modelP.getPersonId());
			current.setTargetName(modelP.getName());
			principals.add(current);
		}
		
		addAuthorizationType(delegate, authorizationResponseType);
	}

	private Client getClient() {
		ClientConfig clientConfig = new ClientConfig();
		Client client = ClientBuilder.newClient(clientConfig);
		client.register(JacksonFeature.class);
		client.register(new ValidationClientRequestFilter(engineApiKey,
				requestAliveSeconds, null));
		return client;
	}

	private void addReasons(List<DecisionReason> reasons,
			Holder<List<DecisionReasonType>> reason) {
		if (reasons != null) {
			if (reason.value == null) {
				reason.value = new ArrayList<DecisionReasonType>();
			}
			for (DecisionReason dr : reasons) {
				DecisionReasonType drt = new DecisionReasonType();
				drt.setReasonRule(dr.getReasonRule());
				drt.setReasonValue(dr.getReasonValue());
				reason.value.add(drt);
			}
		}
	}

	private void addAuthorizationType(Delegate delegate,
			Holder<AuthorizationResponseType> authorization) {
		if (delegate.getAuthorizationType() != null) {
			if (authorization.value == null) {
				authorization.value = delegateFactory
						.createAuthorizationResponseType();
			}
			fi.vm.kapa.xml.rova.api.delegate.AuthorizationType aType = fi.vm.kapa.xml.rova.api.delegate.AuthorizationType
					.fromValue(delegate.getAuthorizationType().toString());
			authorization.value.setAuthorizationResponse(aType);

			List<DecisionReason> reasons = delegate.getReasons();
			for (DecisionReason dr : reasons) {
				fi.vm.kapa.xml.rova.api.delegate.DecisionReasonType drt = new fi.vm.kapa.xml.rova.api.delegate.DecisionReasonType();
				drt.setReasonRule(dr.getReasonRule());
				drt.setReasonValue(dr.getReasonValue());
				authorization.value.getReason().add(drt);
			}
		}
	}

	@Override
	public String toString() {
		return "EngineDataProvider engine url: " + engineUrl;
	}

}
