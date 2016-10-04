/**
 * The MIT License
 * Copyright (c) 2016 Population Register Centre
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package fi.vm.kapa.rova.soap.providers;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.text.SimpleDateFormat;
import java.util.*;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.xml.ws.Holder;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import fi.vm.kapa.rova.config.SpringProperties;
import fi.vm.kapa.rova.engine.model.hpa.Authorization;
import fi.vm.kapa.rova.engine.model.hpa.DecisionReason;
import fi.vm.kapa.rova.engine.model.hpa.Delegate;
import fi.vm.kapa.rova.engine.model.hpa.HpaDelegate;
import fi.vm.kapa.rova.engine.model.ypa.OrganizationResult;
import fi.vm.kapa.rova.engine.model.ypa.ResultRoleType;
import fi.vm.kapa.rova.external.model.ServiceIdType;
import fi.vm.kapa.rova.logging.Logger;
import fi.vm.kapa.rova.logging.LoggingClientRequestFilter;
import fi.vm.kapa.rova.rest.identification.RequestIdentificationFilter;
import fi.vm.kapa.rova.rest.validation.ValidationClientRequestFilter;
import fi.vm.kapa.rova.utils.HetuUtils;
import fi.vm.kapa.xml.rova.api.authorization.AuthorizationType;
import fi.vm.kapa.xml.rova.api.authorization.DecisionReasonType;
import fi.vm.kapa.xml.rova.api.authorization.RovaAuthorizationResponse;
import fi.vm.kapa.xml.rova.api.delegate.Principal;
import fi.vm.kapa.xml.rova.api.delegate.PrincipalType;
import fi.vm.kapa.xml.rova.api.orgroles.OrganizationListType;
import fi.vm.kapa.xml.rova.api.orgroles.OrganizationalRolesType;
import fi.vm.kapa.xml.rova.api.orgroles.RoleList;

import static fi.vm.kapa.rova.utils.EncodingUtils.encodePathParam;

@Component
public class EngineDataProvider implements DataProvider, SpringProperties {

    private static final String INCOMPLETE = "incomplete";
    public static final String INVALID_HETU_MSG = "Invalid hetu.";

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

        if (delegateResponse.value == null) {
            delegateResponse.value = delegateFactory.createResponse();
        }

        if (!HetuUtils.isHetuValid(personId)) {
            delegateResponse.value.setExceptionMessage(
                    delegateFactory.createResponseExceptionMessage(String.format("RequestId: NO_SESSION, Date: %s, Status: %d, Message: %s",
                            new SimpleDateFormat("dd.MM.yyyyy hh:mm:ss").format(new Date()), Status.BAD_REQUEST.getStatusCode(), INVALID_HETU_MSG)));
            LOG.error("Got invalid handleDelegate request: " + INVALID_HETU_MSG + " " + personId);
            return;
        }

        WebTarget webTarget = getClient().target(engineUrl + "hpa/delegate/xroad/" + service + "/" +
                encodePathParam(personId)).queryParam("requestId", requestId);

        webTarget.register(new RequestIdentificationFilter(requestId, endUserId));

        Invocation.Builder invocationBuilder = webTarget.request(MediaType.APPLICATION_JSON);

        Response response = invocationBuilder.get();

        if (response.getStatus() == HttpStatus.OK.value()) {
            HpaDelegate delegate = response.readEntity(Delegate.class);
            Principal principal = delegateFactory.createPrincipal();

            List<PrincipalType> principals = principal.getPrincipal();
            for (fi.vm.kapa.rova.engine.model.hpa.Principal modelP : delegate.getPrincipal()) {
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
                delegateResponse.value.setAuthorization(fi.vm.kapa.xml.rova.api.delegate.AuthorizationType.valueOf(delegate.getAuthorizationType().toString()));
            }
        } else {
            delegateResponse.value.setExceptionMessage(delegateFactory.createResponseExceptionMessage(createExceptionMessage(response)));
            LOG.error("Got error response from engine: " + response.getStatus());
        }
    }

    @Override
    public void handleAuthorization(String delegateId, String principalId, List<String> issues, String service, String endUserId, String requestId,
            Holder<RovaAuthorizationResponse> authorizationResponseHolder) {

        authorizationResponseHolder.value = authorizationFactory.createRovaAuthorizationResponse();

        if (!HetuUtils.isHetuValid(delegateId) || !HetuUtils.isHetuValid(principalId)) {
            authorizationResponseHolder.value.setExceptionMessage(authorizationFactory
                    .createRovaAuthorizationResponseExceptionMessage(String.format("RequestId: NO_SESSION, Date: %s, Status: %d, Message: %s",
                            new SimpleDateFormat("dd.MM.yyyyy hh:mm:ss").format(new Date()), Status.BAD_REQUEST.getStatusCode(), INVALID_HETU_MSG)));
            LOG.error("Got invalid handleAuthorization request: " + INVALID_HETU_MSG + " " + delegateId + "/" + principalId);
            return;
        }

        WebTarget webTarget = getClient().target(engineUrl + "hpa/authorization/xroad/" + service + "/" +
                encodePathParam(delegateId) + "/" + encodePathParam(principalId)).queryParam("requestId", requestId);

        if (issues != null) {
            for (String issue : issues) {
                webTarget = webTarget.queryParam("issue", issue);
            }
        }

        webTarget.register(new RequestIdentificationFilter(requestId, endUserId));
        Invocation.Builder invocationBuilder = webTarget.request(MediaType.APPLICATION_JSON);

        Response response = invocationBuilder.get();

        if (response.getStatus() == HttpStatus.OK.value()) {
            Authorization auth = response.readEntity(Authorization.class);

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
            authorizationResponseHolder.value
                    .setExceptionMessage(authorizationFactory.createRovaAuthorizationResponseExceptionMessage(createExceptionMessage(response)));
            LOG.error("Got error response from engine: " + response.getStatus());
        }
    }

    @Override
    public void handleOrganizationalRoles(String personId, List<String> organizationIds, String service, String endUserId, String requestId,
            Holder<fi.vm.kapa.xml.rova.api.orgroles.Response> rolesResponseHolder) {
        rolesResponseHolder.value = organizationalRolesFactory.createResponse();
        boolean complete = true;

        if (!HetuUtils.isHetuValid(personId)) {
            rolesResponseHolder.value.setExceptionMessage(
                    organizationalRolesFactory.createResponseExceptionMessage(String.format("RequestId: NO_SESSION, Date: %s, Status: %d, Message: %s",
                            new SimpleDateFormat("dd.MM.yyyyy hh:mm:ss").format(new Date()), Status.BAD_REQUEST.getStatusCode(), INVALID_HETU_MSG)));
            LOG.error("Got invalid handleOrganizationalRoles request: " + INVALID_HETU_MSG + " " + personId);
            return;
        }

        WebTarget webTarget = getClient().target(engineUrl + "ypa/roles/" + ServiceIdType.XROAD.getText() + "/" +
                service + "/" + encodePathParam(personId));
        webTarget.queryParam("requestId", requestId);
        if (organizationIds != null) {
            for (Iterator<String> iterator = organizationIds.iterator(); iterator.hasNext();) {
                webTarget = webTarget.queryParam("organizationId", iterator.next());
            }
        }
        webTarget.register(new RequestIdentificationFilter(requestId, endUserId));
        Invocation.Builder invocationBuilder = webTarget.request(MediaType.APPLICATION_JSON);

        Response response = invocationBuilder.get();

        if (response.getStatus() == HttpStatus.OK.value()) {
            List<OrganizationResult> roles = response.readEntity(new GenericType<List<OrganizationResult>>() {
            });

            OrganizationListType organizationListType = organizationalRolesFactory.createOrganizationListType();
            List<OrganizationalRolesType> organizationalRoles = organizationListType.getOrganization();

            for (OrganizationResult organizationResult : roles) {
                OrganizationalRolesType ort = organizationalRolesFactory.createOrganizationalRolesType();

                if (!organizationResult.isComplete()) {
                    complete = false;
                }

                ort.setName(organizationResult.getName());
                ort.setOrganizationIdentifier(organizationResult.getIdentifier());
                RoleList roleList = organizationalRolesFactory.createRoleList();
                for (ResultRoleType rt : organizationResult.getRoles()) {
                    roleList.getRole().add(rt.toString());
                }
                ort.setRoles(roleList);

                organizationalRoles.add(ort);
            }

            rolesResponseHolder.value.setOrganizationList(organizationListType);

            if (!complete) {
                rolesResponseHolder.value.setExceptionMessage(organizationalRolesFactory.createResponseExceptionMessage(INCOMPLETE));
                LOG.info("Result is not complete");
            }

        } else {
            rolesResponseHolder.value.setExceptionMessage(organizationalRolesFactory.createResponseExceptionMessage(createExceptionMessage(response)));
            LOG.error("Got error response from engine: " + response.getStatus());
        }
    }

    void setEngineUrl(String engineUrl) {
        this.engineUrl = engineUrl;
    }

    void setEngineApiKey(String engineApiKey) {
        this.engineApiKey = engineApiKey;
    }

    void setRequestAliveSeconds(Integer requestAlive) {
        this.requestAliveSeconds = requestAlive;
    }

    private String createExceptionMessage(Response response) {
        Map<String, String> attributes = getAttributes(response);

        return String.format("RequestId: %s, Date: %s, Status: %d %s, Message: %s", valueOrDefault(attributes.get(Logger.REQUEST_ID), "NO_SESSION"),
                response.getDate().toString(), response.getStatusInfo().getStatusCode(), response.getStatusInfo().getReasonPhrase(),
                valueOrDefault(attributes.get("errorMessage"), "(none)"));
    }

    private String valueOrDefault(String value, String defaultValue) {
        return isNotBlank(value) ? value : defaultValue;
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> getAttributes(Response response) {
        Object entity = null;
        if (response.hasEntity()) {
            LOG.error("Response mediatype: " + (response.getMediaType() != null ? response.getMediaType().toString() : "mediatype unavailable!"));
            try {
                entity = response.readEntity(Object.class);
            } catch (RuntimeException t) {
                LOG.error("Response mediatype: " + t);
                // eat
            }
        }
        if (entity != null && Map.class.isAssignableFrom(entity.getClass())) {
            return (Map<String, String>) entity;
        }

        return Collections.emptyMap();
    }

    protected Client getClient() {
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
