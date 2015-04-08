package fi.vm.kapa.rova.soap.providers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;

import fi.vm.kapa.rova.config.SpringProperties;
import fi.vm.kapa.rova.engine.model.Delegate;
import fi.vm.kapa.rova.engine.resources.EngineResource;
import fi.vm.kapa.rova.rest.validation.ValidationClientRequestFilter;
import fi.vm.kapa.xml.rova.api.AuthorizationType;
import fi.vm.kapa.xml.rova.api.DecisionReasonType;
import fi.vm.kapa.xml.rova.api.ObjectFactory;
import fi.vm.kapa.xml.rova.api.Principal;
import fi.vm.kapa.xml.rova.api.PrincipalType;

@Component
public class EngineDataProvider implements DataProvider, SpringProperties {
	Logger LOG = Logger.getLogger(EngineDataProvider.class.toString());

	EngineResource engineResource = null;
	private ObjectFactory factory = new ObjectFactory();
	private ObjectMapper mapper = new ObjectMapper();
	
	@Value(ENGINE_URL)
	private String engineUrl;

	@Value(ENGINE_API_KEY)
	private String engineApiKey;

	@PostConstruct
	public void init(){
		ClientConfig cc = new ClientConfig().register(JacksonFeature.class);
		Client resource = ClientBuilder.newClient(cc);
		engineResource = WebResourceFactory.newResource(EngineResource.class,
				resource.target(engineUrl));
		mapper.configure(JsonGenerator.Feature.QUOTE_FIELD_NAMES, false);
		mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
	}
	
	public AuthorizationType getAuthorizationTypeResponse(String delegateId,
			String principalId, String industry, String service, String issue, 
			String endUserId, Holder<List<DecisionReasonType>> reason) {
		WebTarget webTarget = getClient().target(
				engineUrl + "authorization" + "/" + service + "/"  +endUserId 
					+ "/" + delegateId +"/"+ principalId);
		Invocation.Builder invocationBuilder = webTarget.request(MediaType.APPLICATION_JSON);
		Response response = invocationBuilder.get();
//		Authorization auth = response.readEntity(Authorization.class);
		@SuppressWarnings("rawtypes")
		Map responseMap = response.readEntity(Map.class);
		
//		AuthorizationType result = AuthorizationType.fromValue(auth.getResult().toString());
		// haetaan ja konvertoidaan authorization map-entrystä  
		AuthorizationType result = AuthorizationType.fromValue(responseMap.get("result").toString());
		
		// haetaan enginen palauttamat syyt ja lisätään holderiin
		addReasons(responseMap, reason); // TODO lisäämislogiikka industry/service/issue/endUserId-perusteella
		
		return result;
	}

	public Principal getPrincipalResponse(String personId, String industry,
			String service, String issue, String endUserId, Holder<List<DecisionReasonType>> reason) {
		WebTarget webTarget = getClient().target(
				engineUrl + "delegate" + "/" + service + "/"  +endUserId 
				+"/"+ personId + "/" + endUserId);
		Invocation.Builder invocationBuilder = webTarget.request(MediaType.APPLICATION_JSON);
		Response response = invocationBuilder.get();
//		Delegate delegate = response.readEntity(Delegate.class);
		@SuppressWarnings("rawtypes")
		Map responseMap = response.readEntity(Map.class);

		// haetaan ja konvertoidaan delegate map-entrystä  
		Object resultObj = responseMap.get("result");
		Delegate delegate = null;
		try {
			if (resultObj != null) {
				String resp = resultObj.toString();
				resp = resp.replaceAll("[^\\{\\[\\]\\},=]+", "\"$0\""); // lisätään lainausmerkit kaikkien kenttien ja arvojen ympärille
				resp = resp.replaceAll("\" ", "\""); // poistetaan ylimääräinen välilyönti kenttien alusta
				resp = resp.replaceAll("\"\"", ""); // poistetaan tyhjät lainausmerkit
				resp = resp.replaceAll("=", ":"); // korvataan sijoitus (=) kaksoispisteellä (:)
				delegate = mapper.readValue(resp, Delegate.class);
			}
		} catch (IOException e) {
			LOG.severe("delegate conversion failed: "+ e.getMessage());
		}
		
		// haetaan enginen palauttamat syyt ja lisätään holderiin
		addReasons(responseMap, reason); // TODO lisäämislogiikka industry/service/issue/endUserId-perusteella

		Principal principal = factory.createPrincipal();
		if (delegate != null) {
			List<PrincipalType> principals = principal.getPrincipalElem();
			for (fi.vm.kapa.rova.engine.model.Principal modelP : delegate.getPrincipal()) {
				PrincipalType current = factory.createPrincipalType();
				current.setTargetIdentifier(modelP.getPersonId());
				current.setTargetName(modelP.getName());
				principals.add(current);
			}
		}
		return principal;
	}

	private Client getClient() {
		ClientConfig clientConfig = new ClientConfig();
		Client client = ClientBuilder.newClient(clientConfig);
		client.register(JacksonFeature.class);
		client.register(new ValidationClientRequestFilter(engineApiKey));
		return client;
	}
	
	@SuppressWarnings("rawtypes")
	private void addReasons(Map reasonMap, Holder<List<DecisionReasonType>> reason) {
		List reasons = (List)reasonMap.get("reason");
		if (reasons != null) {
			if (reason.value == null) {
				reason.value = new ArrayList<DecisionReasonType>();
			}
			for (Object object : reasons) {
				Map mapObj = (Map)object;
				DecisionReasonType drt = new DecisionReasonType();
				drt.setReasonRule((String)mapObj.get("reasonRule"));
				drt.setReasonValue((String)mapObj.get("reasonValue"));
				reason.value.add(drt);
			}
		}
	}

	@Override
	public String toString() {
		return "EngineDataProvider engine url: " + engineUrl;
	}
}
