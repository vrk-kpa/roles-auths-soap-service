package fi.vm.kapa.rova.soap.providers;

import fi.vm.kapa.rova.config.SpringProperties;
import fi.vm.kapa.rova.engine.model.*;
import fi.vm.kapa.rova.logging.Logger;
import fi.vm.kapa.rova.logging.LoggingClientRequestFilter;
import fi.vm.kapa.rova.rest.identification.RequestIdentificationFilter;
import fi.vm.kapa.rova.rest.validation.ValidationClientRequestFilter;
import fi.vm.kapa.xml.rova.api.authorization.AuthorizationType;
import fi.vm.kapa.xml.rova.api.authorization.DecisionReasonType;
import fi.vm.kapa.xml.rova.api.authorization.RovaAuthorizationResponse;
import fi.vm.kapa.xml.rova.api.delegate.Principal;
import fi.vm.kapa.xml.rova.api.delegate.PrincipalType;
import fi.vm.kapa.xml.rova.api.orgroles.OrganizationListType;
import fi.vm.kapa.xml.rova.api.orgroles.OrganizationalRolesType;
import fi.vm.kapa.xml.rova.api.orgroles.RoleList;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.ws.Holder;
import java.util.*;

@Component
public class EngineDataProvider implements DataProvider, SpringProperties {

    Logger LOG = Logger.getLogger(EngineDataProvider.class);

    private fi.vm.kapa.xml.rova.api.authorization.ObjectFactory authorizationFactory = new fi.vm.kapa.xml.rova.api.authorization.ObjectFactory();
    private fi.vm.kapa.xml.rova.api.delegate.ObjectFactory delegateFactory = new fi.vm.kapa.xml.rova.api.delegate.ObjectFactory();
    private fi.vm.kapa.xml.rova.api.orgroles.ObjectFactory organizationalRolesFactory = new fi.vm.kapa.xml.rova.api.orgroles.ObjectFactory();
   
    
    @Value(ENGINE_URL)
    private String engineUrl;

    @Value(ENGINE_API_KEY)
    private String engineApiKey;

    @Value(REQUEST_ALIVE_SECONDS)
    private Integer requestAliveSeconds;

    @Override
    public void handleDelegate(String personId, String service, String endUserId, String requestId,
            Holder<fi.vm.kapa.xml.rova.api.delegate.Response> delegateResponse) {

        WebTarget webTarget = getClient().target(engineUrl + "hpa/delegate/" + service + "/"
                + endUserId + "/" + personId).queryParam("requestId", requestId);
        
        webTarget.register(new RequestIdentificationFilter(requestId, endUserId));
       
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
            delegateResponse.value = delegateFactory.createResponse();
            delegateResponse.value.setExceptionMessage(delegateFactory
                    .createResponseExceptionMessage(createExceptionMessage(response)));
            LOG.error("Got error response from engine: " + response.getStatus());
        }

    }

    public void handleAuthorization(String delegateId, String principalId, String service,
            String endUserId, String requestId, Holder<RovaAuthorizationResponse> authorizationResponseHolder) {

        WebTarget webTarget = getClient().target(engineUrl + "hpa/authorization/" + service
                + "/" + endUserId + "/" + delegateId + "/" + principalId).queryParam("requestId", requestId);
        
        webTarget.register(new RequestIdentificationFilter(requestId, endUserId));
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
            authorizationResponseHolder.value = authorizationFactory.createRovaAuthorizationResponse();
            authorizationResponseHolder.value.setExceptionMessage(authorizationFactory
                    .createRovaAuthorizationResponseExceptionMessage(createExceptionMessage(response)));
            LOG.error("Got error response from engine: " + response.getStatus());
        }
    }

    @Override
    public void handleOrganizationalRoles(String personId, List<String> organizationIds, String service,
            String endUserId, String requestId,
            Holder<fi.vm.kapa.xml.rova.api.orgroles.Response> rolesResponseHolder) {
        
        String orgIds = "";
        if (organizationIds != null) {
            for (Iterator<String> iterator = organizationIds.iterator(); iterator.hasNext();) {
                orgIds += iterator.next();
                if (iterator.hasNext()) {
                    orgIds += ";";
                }
            }
        }

        WebTarget webTarget = getClient().target(engineUrl + "ypa/roles/" + service + "/"
                + endUserId + "/" + personId).queryParam("requestId", requestId).queryParam("organizationIds", orgIds);

        
        webTarget.register(new RequestIdentificationFilter(requestId, endUserId));
        Invocation.Builder invocationBuilder = webTarget.request(MediaType.APPLICATION_JSON);

        Response response = invocationBuilder.get();

        if (response.getStatus() == HttpStatus.OK.value()) {
            Set<OrganizationResult> roles = response.readEntity(new GenericType<Set<OrganizationResult>>() {});

            OrganizationListType organizationListType = organizationalRolesFactory.createOrganizationListType();
            List<OrganizationalRolesType> organizationalRoles = organizationListType.getOrganization();

            if (roles != null) {
                for (OrganizationResult organizationResult : roles) {
                    OrganizationalRolesType ort = organizationalRolesFactory.createOrganizationalRolesType(); 
                    fi.vm.kapa.xml.rova.api.orgroles.OrganizationType organizationType = organizationalRolesFactory.createOrganizationType();
                    organizationType.setName(organizationResult.getName());
                    organizationType.setOrganizationIdentifier(organizationResult.getIdentifier());
                    ort.setOrganization(organizationType);
                    RoleList roleList = organizationalRolesFactory.createRoleList();
                    for (ResultRoleType rt : organizationResult.getRoles()) {
                        roleList.getRole().add(rt.toString());
                    }
                    ort.setRoles(roleList);
                    organizationalRoles.add(ort);
                }
            } else {
                OrganizationalRolesType ort = organizationalRolesFactory.createOrganizationalRolesType(); 
                RoleList roleList = organizationalRolesFactory.createRoleList();
                ort.setRoles(roleList);
                organizationalRoles.add(ort);
            }
            
            rolesResponseHolder.value = organizationalRolesFactory.createResponse();
            rolesResponseHolder.value.setOrganizationList(organizationListType);
        } else {
            rolesResponseHolder.value = organizationalRolesFactory.createResponse();
            rolesResponseHolder.value.setExceptionMessage(organizationalRolesFactory
                    .createResponseExceptionMessage(createExceptionMessage(response)));
            LOG.error("Got error response from engine: " + response.getStatus());
        }
        
    }

    private String createExceptionMessage(Response response) {
        Object entity = response.readEntity(Object.class);
        String reqId = "NO_SESSION";
        if (entity != null && Map.class.isAssignableFrom(entity.getClass())) {
            reqId = (String) ((Map)entity).get("ReqID");
        }
        return new StringBuilder("RequestId: ").append(reqId)
                .append(", Date: ").append(response.getDate())
                .append(", Status: ").append(response.getStatusInfo().getStatusCode())
                .append(" ").append(response.getStatusInfo().getReasonPhrase()).toString();
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
