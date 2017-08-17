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
import fi.vm.kapa.rova.engine.model.hpa.AuthorizationInternal;
import fi.vm.kapa.rova.engine.model.hpa.AuthorizationListInternal;
import fi.vm.kapa.rova.engine.model.hpa.DecisionReason;
import fi.vm.kapa.rova.engine.model.hpa.HpaDelegate;
import fi.vm.kapa.rova.engine.model.ypa.OrganizationResult;
import fi.vm.kapa.rova.engine.model.ypa.YpaResult;
import fi.vm.kapa.rova.external.model.IResultType;
import fi.vm.kapa.rova.external.model.ServiceIdType;
import fi.vm.kapa.rova.rest.exception.HttpStatusException;
import fi.vm.kapa.rova.rest.identification.RequestIdentificationInterceptor;
import fi.vm.kapa.rova.utils.HetuUtils;
import fi.vm.kapa.xml.rova.api.authorization.AuthorizationType;
import fi.vm.kapa.xml.rova.api.authorization.DecisionReasonType;
import fi.vm.kapa.xml.rova.api.authorization.RovaAuthorizationResponse;
import fi.vm.kapa.xml.rova.api.authorization.list.RovaAuthorizationListResponse;
import fi.vm.kapa.xml.rova.api.delegate.Principal;
import fi.vm.kapa.xml.rova.api.delegate.PrincipalType;
import fi.vm.kapa.xml.rova.api.orgroles.OrganizationListType;
import fi.vm.kapa.xml.rova.api.orgroles.OrganizationalRolesType;
import fi.vm.kapa.xml.rova.api.orgroles.RoleList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import javax.ws.rs.core.Response.Status;
import javax.xml.ws.Holder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class EngineDataProvider implements DataProvider, SpringProperties {

    public static final String INCOMPLETE = "incomplete";
    public static final String INVALID_HETU_MSG = "Invalid hetu.";

    private fi.vm.kapa.xml.rova.api.authorization.ObjectFactory authorizationFactory = new fi.vm.kapa.xml.rova.api.authorization.ObjectFactory();
    private fi.vm.kapa.xml.rova.api.authorization.list.ObjectFactory authorizationListFactory = new fi.vm.kapa.xml.rova.api.authorization.list.ObjectFactory();
    private fi.vm.kapa.xml.rova.api.delegate.ObjectFactory delegateFactory = new fi.vm.kapa.xml.rova.api.delegate.ObjectFactory();
    private fi.vm.kapa.xml.rova.api.orgroles.ObjectFactory organizationalRolesFactory = new fi.vm.kapa.xml.rova.api.orgroles.ObjectFactory();

    @Autowired
    HpaClient hpaClient;

    @Autowired
    YpaClient ypaClient;

    @Override
    public String handleDelegate(String personId, String service, String endUserId, String requestId,
            Holder<fi.vm.kapa.xml.rova.api.delegate.Response> delegateResponse) {

        if (delegateResponse.value == null) {
            delegateResponse.value = delegateFactory.createResponse();
        }
        String serviceUuid = "";

        if (!HetuUtils.isHetuValid(personId)) {
            delegateResponse.value.setExceptionMessage(
                    delegateFactory.createResponseExceptionMessage(String.format("RequestId: NO_SESSION, Date: %s, Status: %d, Message: %s",
                            new SimpleDateFormat("dd.MM.yyyyy hh:mm:ss").format(new Date()), Status.BAD_REQUEST.getStatusCode(), INVALID_HETU_MSG)));
            return serviceUuid;
        }

        origEndUserToRequestContext(endUserId);
        origRequestIdToRequestContext(requestId);

        try {

            HpaDelegate delegate = hpaClient.getDelegate(ServiceIdType.XROAD.toString(), personId, service);

            if (delegate != null) {
                Principal principal = delegateFactory.createPrincipal();
                serviceUuid = delegate.getServiceUuid();

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
                String message = "Got empty delegate response from engine";
                delegateResponse.value.setExceptionMessage(delegateFactory.createResponseExceptionMessage(message));
            }

        } catch (RestClientException e) {
            Throwable reason = e.getRootCause();
            String message = "Got error response from engine: " + ((reason != null && (reason instanceof HttpStatusException)) ? reason : e);
            delegateResponse.value.setExceptionMessage(delegateFactory.createResponseExceptionMessage(message));
        } catch (Exception e) {
            String message = "Error occurred: " + e.getMessage();
            delegateResponse.value.setExceptionMessage(delegateFactory.createResponseExceptionMessage(message));
        }

        return serviceUuid;
    }

    @Override
    public String handleAuthorizationList(String delegateId, String principalId, String service, String endUserId, String requestId, Holder<RovaAuthorizationListResponse> authorizationListResponse) {
        authorizationListResponse.value = authorizationListFactory.createRovaAuthorizationListResponse();
        authorizationListResponse.value.setRoles(authorizationListFactory.createRoleList());
        String serviceUuid = "";

        if (!HetuUtils.isHetuValid(delegateId) || !HetuUtils.isHetuValid(principalId)) {
            authorizationListResponse.value.setExceptionMessage(authorizationFactory
                    .createRovaAuthorizationResponseExceptionMessage(String.format("RequestId: NO_SESSION, Date: %s, Status: %d, Message: %s",
                            new SimpleDateFormat("dd.MM.yyyyy hh:mm:ss").format(new Date()), Status.BAD_REQUEST.getStatusCode(), INVALID_HETU_MSG)));
            return serviceUuid;
        }

        origEndUserToRequestContext(endUserId);
        origRequestIdToRequestContext(requestId);

        AuthorizationListInternal auth = hpaClient.getAuthorizationList(ServiceIdType.XROAD.toString(), service, delegateId, principalId);

        try {

            if (auth != null) {
                serviceUuid = auth.getServiceUuid();
                if (auth.getRoles() != null) {
                    authorizationListResponse.value.getRoles().getRoles().addAll(auth.getRoles());
                }

                if (auth.getReasons() != null) {
                    for (DecisionReason dr : auth.getReasons()) {
                        fi.vm.kapa.xml.rova.api.authorization.list.DecisionReasonType drt =
                                new fi.vm.kapa.xml.rova.api.authorization.list.DecisionReasonType();
                        drt.setRule(dr.getReasonRule());
                        drt.setValue(dr.getReasonValue());
                        authorizationListResponse.value.getReason().add(drt);
                    }
                }
            } else {
                String message = "Got empty authorization response from engine";
                authorizationListResponse.value
                        .setExceptionMessage(authorizationListFactory.createRovaAuthorizationListResponseExceptionMessage(message));
            }

        } catch (RestClientException e) {
            Throwable reason = e.getRootCause();
            String message = "Got error response from engine: " + ((reason != null && (reason instanceof HttpStatusException)) ? reason : e);
            authorizationListResponse.value
                    .setExceptionMessage(authorizationListFactory.createRovaAuthorizationListResponseExceptionMessage(message));
        } catch (Exception e) {
            String message = "Error occurred: " + e.getMessage();
            authorizationListResponse.value
                    .setExceptionMessage(authorizationListFactory.createRovaAuthorizationListResponseExceptionMessage(message));
        }

        return serviceUuid;

    }

    @Override
    public String handleAuthorization(String delegateId, String principalId, List<String> issues, String service, String endUserId, String requestId,
            Holder<RovaAuthorizationResponse> authorizationResponse) {

        authorizationResponse.value = authorizationFactory.createRovaAuthorizationResponse();
        authorizationResponse.value.setAuthorization(AuthorizationType.DISALLOWED);
        String serviceUuid = "";

        if (!HetuUtils.isHetuValid(delegateId) || !HetuUtils.isHetuValid(principalId)) {
            authorizationResponse.value.setExceptionMessage(authorizationFactory
                    .createRovaAuthorizationResponseExceptionMessage(String.format("RequestId: NO_SESSION, Date: %s, Status: %d, Message: %s",
                            new SimpleDateFormat("dd.MM.yyyyy hh:mm:ss").format(new Date()), Status.BAD_REQUEST.getStatusCode(), INVALID_HETU_MSG)));
            return serviceUuid;
        }

        Set<String> issueSet = (issues != null) ? new HashSet<>(issues) : null;

        origEndUserToRequestContext(endUserId);
        origRequestIdToRequestContext(requestId);

        AuthorizationInternal auth = hpaClient.getAuthorization(ServiceIdType.XROAD.toString(), service, delegateId, principalId, issueSet);

        try {

            if (auth != null) {
                serviceUuid = auth.getServiceUuid();

                authorizationResponse.value.setAuthorization(AuthorizationType.fromValue(auth.getResult().toString()));

                if (auth.getReasons() != null) {
                    for (DecisionReason dr : auth.getReasons()) {
                        DecisionReasonType drt = new DecisionReasonType();
                        drt.setRule(dr.getReasonRule());
                        drt.setValue(dr.getReasonValue());
                        authorizationResponse.value.getReason().add(drt);
                    }
                }

            } else {
                String message = "Got empty authorization response from engine";
                authorizationResponse.value
                        .setExceptionMessage(authorizationFactory.createRovaAuthorizationResponseExceptionMessage(message));
            }

        } catch (RestClientException e) {
            Throwable reason = e.getRootCause();
            String message = "Got error response from engine: " + ((reason != null && (reason instanceof HttpStatusException)) ? reason : e);
            authorizationResponse.value
                    .setExceptionMessage(authorizationFactory.createRovaAuthorizationResponseExceptionMessage(message));
        } catch (Exception e) {
            String message = "Error occurred: " + e.getMessage();
            authorizationResponse.value
                    .setExceptionMessage(authorizationFactory.createRovaAuthorizationResponseExceptionMessage(message));
        }

        return serviceUuid;
    }

    @Override
    public String handleOrganizationalRoles(String personId, List<String> organizationIds, String service, String endUserId, String requestId,
            Holder<fi.vm.kapa.xml.rova.api.orgroles.Response> rolesResponseHolder) {
        rolesResponseHolder.value = organizationalRolesFactory.createResponse();
        String serviceUuid = "";
        boolean complete = true;

        if (!HetuUtils.isHetuValid(personId)) {
            rolesResponseHolder.value.setExceptionMessage(
                    organizationalRolesFactory.createResponseExceptionMessage(String.format("RequestId: NO_SESSION, Date: %s, Status: %d, Message: %s",
                            new SimpleDateFormat("dd.MM.yyyyy hh:mm:ss").format(new Date()), Status.BAD_REQUEST.getStatusCode(), INVALID_HETU_MSG)));
            return serviceUuid;
        }

        origEndUserToRequestContext(endUserId);
        origRequestIdToRequestContext(requestId);

        List<OrganizationResult> roles = null;

        try {

            YpaResult ypaResult = ypaClient.getRoles(personId, ServiceIdType.XROAD.toString(), service, organizationIds);

            if (ypaResult != null && (roles = ypaResult.getOrganizationResults()) != null) {

                serviceUuid = ypaResult.getServiceUuid();
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
                }

            } else {
                String message = "Got empty roles response from engine";
                rolesResponseHolder.value.setExceptionMessage(organizationalRolesFactory.createResponseExceptionMessage(message));
            }

        } catch (RestClientException e) {
            Throwable reason = e.getRootCause();
            String message = "Got error response from engine: " + ((reason != null && (reason instanceof HttpStatusException)) ? reason : e);
            rolesResponseHolder.value.setExceptionMessage(organizationalRolesFactory.createResponseExceptionMessage(message));
        } catch (Exception e) {
            String message = "Error occurred: " + e.getMessage();
            rolesResponseHolder.value.setExceptionMessage(organizationalRolesFactory.createResponseExceptionMessage(message));
        }

        return serviceUuid;
    }

    private void origEndUserToRequestContext(String endUserId) {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            attrs.setAttribute(RequestIdentificationInterceptor.ORIG_END_USER, endUserId,
                    RequestAttributes.SCOPE_REQUEST);
        }
    }

    private void origRequestIdToRequestContext(String origRequestId) {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            attrs.setAttribute(RequestIdentificationInterceptor.ORIG_REQUEST_IDENTIFIER, origRequestId,
                    RequestAttributes.SCOPE_REQUEST);
        }
    }

    void setHpaClient(HpaClient hpaClient) {
        this.hpaClient = hpaClient;
    }

    void setYpaClient(YpaClient ypaClient) {
        this.ypaClient = ypaClient;
    }
}
