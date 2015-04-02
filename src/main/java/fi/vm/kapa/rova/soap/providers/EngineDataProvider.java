package fi.vm.kapa.rova.soap.providers;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.proxy.WebResourceFactory;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import fi.vm.kapa.rova.config.SpringProperties;
import fi.vm.kapa.rova.engine.model.Authorization;
import fi.vm.kapa.rova.engine.model.Delegate;
import fi.vm.kapa.rova.engine.resources.EngineResource;
import fi.vm.kapa.xml.rova.api.AuthorizationType;
import fi.vm.kapa.xml.rova.api.ObjectFactory;
import fi.vm.kapa.xml.rova.api.Principal;
import fi.vm.kapa.xml.rova.api.PrincipalType;

@Component
public class EngineDataProvider implements DataProvider, SpringProperties {

	EngineResource engineResource = null;
	private ObjectFactory factory = new ObjectFactory();
	
	@Value(ENGINE_URL)
	private String engineUrl;

	@PostConstruct
	public void init(){
		ClientConfig cc = new ClientConfig().register(JacksonFeature.class);
		Client resource = ClientBuilder.newClient(cc);
		engineResource = WebResourceFactory.newResource(EngineResource.class,
				resource.target(engineUrl));
	}
	
	public AuthorizationType getAuthorizationTypeResponse(String delegateId,
			String principalId, String industry, String service, String issue, String endUserId) {
		WebTarget webTarget = getClient().target(
				engineUrl + "authorization" + "/" + delegateId +"/"+ principalId + "?service=" + service);
		Invocation.Builder invocationBuilder = webTarget.request(MediaType.APPLICATION_JSON);
		Response response = invocationBuilder.get();
		Authorization auth = response.readEntity(Authorization.class);

		AuthorizationType result = AuthorizationType.fromValue(auth.getResult().toString());

		return result;
	}

	public Principal getPrincipalResponse(String personId, String industry,
			String service, String issue, String endUserId) {
		WebTarget webTarget = getClient().target(
				engineUrl + "delegate" + "/" + personId + "?service=" + service);
		Invocation.Builder invocationBuilder = webTarget.request(MediaType.APPLICATION_JSON);
		Response response = invocationBuilder.get();
		Delegate delegate = response.readEntity(Delegate.class);

		Principal principal = factory.createPrincipal();
		List<PrincipalType> principals = principal.getPrincipalElem();
		for (fi.vm.kapa.rova.engine.model.Principal modelP : delegate.getPrincipal()) {
			PrincipalType current = factory.createPrincipalType();
			current.setTargetIdentifier(modelP.getPersonId());
			current.setTargetName(modelP.getName());
			principals.add(current);
		}
		return principal;
	}

	private Client getClient() {
		ClientConfig clientConfig = new ClientConfig();
		Client client = ClientBuilder.newClient(clientConfig);
		client.register(JacksonFeature.class);
		return client;
	}
	
	@Override
	public String toString() {
		return "EngineDataProvider engine url: " + engineUrl;
	}
}
