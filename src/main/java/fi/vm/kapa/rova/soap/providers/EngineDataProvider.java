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

import fi.vm.kapa.rova.config.SpringProperties;
import fi.vm.kapa.rova.engine.HpaClient;
import fi.vm.kapa.rova.engine.YpaClient;
import fi.vm.kapa.rova.engine.model.hpa.Authorization;
import fi.vm.kapa.rova.engine.model.hpa.DecisionReason;
import fi.vm.kapa.rova.engine.model.hpa.HpaDelegate;
import fi.vm.kapa.rova.engine.model.ypa.OrganizationResult;
import fi.vm.kapa.rova.external.model.IResultType;
import fi.vm.kapa.rova.external.model.ServiceIdType;
import fi.vm.kapa.rova.logging.Logger;
import fi.vm.kapa.rova.utils.HetuUtils;
import fi.vm.kapa.xml.rova.api.authorization.AuthorizationType;
import fi.vm.kapa.xml.rova.api.authorization.DecisionReasonType;
import fi.vm.kapa.xml.rova.api.authorization.RovaAuthorizationResponse;
import fi.vm.kapa.xml.rova.api.delegate.Principal;
import fi.vm.kapa.xml.rova.api.delegate.PrincipalType;
import fi.vm.kapa.xml.rova.api.orgroles.OrganizationListType;
import fi.vm.kapa.xml.rova.api.orgroles.OrganizationalRolesType;
import fi.vm.kapa.xml.rova.api.orgroles.RoleList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.Response.Status;
import javax.xml.ws.Holder;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Component
public class EngineDataProvider implements DataProvider, SpringProperties {

    private static final String INCOMPLETE = "incomplete";
    public static final String INVALID_HETU_MSG = "Invalid hetu.";

    Logger LOG = Logger.getLogger(EngineDataProvider.class);

    private fi.vm.kapa.xml.rova.api.authorization.ObjectFactory authorizationFactory = new fi.vm.kapa.xml.rova.api.authorization.ObjectFactory();
    private fi.vm.kapa.xml.rova.api.delegate.ObjectFactory delegateFactory = new fi.vm.kapa.xml.rova.api.delegate.ObjectFactory();
    private fi.vm.kapa.xml.rova.api.orgroles.ObjectFactory organizationalRolesFactory = new fi.vm.kapa.xml.rova.api.orgroles.ObjectFactory();

    @Autowired
    HpaClient hpaClient;

    @Autowired
    YpaClient ypaClient;

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

        ResponseEntity<HpaDelegate> response = hpaClient.getDelegate(ServiceIdType.XROAD.toString(), personId, service);

        if (response.getStatusCode() == HttpStatus.OK) {
            HpaDelegate delegate = response.getBody();
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

            if (delegate.getAuthorizationType() != null &&
                    delegate.getAuthorizationType() != fi.vm.kapa.rova.external.model.AuthorizationType.UNKNOWN) {
                delegateResponse.value.setAuthorization(fi.vm.kapa.xml.rova.api.delegate.AuthorizationType.valueOf(
                        delegate.getAuthorizationType().toString()));
            }
        } else {
            delegateResponse.value.setExceptionMessage(delegateFactory.createResponseExceptionMessage(createExceptionMessage(response)));
            LOG.error("Got error response from engine: " + response.getStatusCodeValue());
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

        Set<String> issueSet = (issues != null) ? new HashSet<>(issues) : null;

        ResponseEntity<Authorization> response = hpaClient.getAuthorization(ServiceIdType.XROAD.toString(), service, delegateId, principalId, issueSet);

        if (response.getStatusCode() == HttpStatus.OK) {
            Authorization auth = response.getBody();

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
            LOG.error("Got error response from engine: " + response.getStatusCodeValue());
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

        ResponseEntity<List<OrganizationResult>> response = ypaClient.getRoles(personId, ServiceIdType.XROAD.toString(), service, organizationIds);

        if (response.getStatusCode() == HttpStatus.OK) {
            List<OrganizationResult> roles = response.getBody();

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
                for (IResultType rt : organizationResult.getRoles()) {
                    roleList.getRole().add(rt.getResult());
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
            LOG.error("Got error response from engine: " + response.getStatusCodeValue());
        }
    }

    private String createExceptionMessage(ResponseEntity<?> response) {
        Map<String, String> attributes = getAttributes(response);
        HttpHeaders headers = response.getHeaders();
        HttpStatus status = response.getStatusCode();
        Date date = new Date(headers.getDate());

        return String.format("RequestId: %s, Date: %s, Status: %d %s, Message: %s", valueOrDefault(attributes.get(Logger.REQUEST_ID),
                "NO_SESSION"), date.toString(), status.value(), status.getReasonPhrase(),
                valueOrDefault(attributes.get("errorMessage"), "(none)"));
    }

    private String valueOrDefault(String value, String defaultValue) {
        return isNotBlank(value) ? value : defaultValue;
    }

    private <T> Map<String, String> getAttributes(ResponseEntity<T> response) {
        T entity = null;
        if (response.hasBody()) {
            MediaType mediaType = response.getHeaders().getContentType();
            LOG.error("Response mediatype: " + (mediaType != null ? mediaType.toString() : "mediatype unavailable!"));
            try {
                entity = response.getBody();
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

    void setHpaClient(HpaClient hpaClient) {
        this.hpaClient = hpaClient;
    }

    void setYpaClient(YpaClient ypaClient) {
        this.ypaClient = ypaClient;
    }
}
