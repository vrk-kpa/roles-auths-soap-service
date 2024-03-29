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
package fi.vm.kapa.rova.soap;

import fi.vm.kapa.rova.engine.Ypa;
import fi.vm.kapa.rova.logging.Logger;
import fi.vm.kapa.rova.soap.providers.EngineDataProvider;
import fi.vm.kapa.xml.rova.api.orgroles.*;
import org.springframework.stereotype.Component;

import javax.jws.WebService;
import javax.xml.bind.JAXBElement;
import javax.xml.ws.Holder;
import java.util.List;

import static fi.vm.kapa.rova.logging.Logger.Field.*;
import static fi.vm.kapa.rova.logging.Logger.Level.ERROR;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@WebService(endpointInterface = "fi.vm.kapa.xml.rova.api.orgroles.RovaOrganizationalRolesPortType")
@Component("rovaOrganizationalRolesService")
public class OrganizationalRolesApi extends AbstractSoapService implements RovaOrganizationalRolesPortType {

    private Logger LOG = Logger.getLogger(OrganizationalRolesApi.class);

    private ObjectFactory factory = new ObjectFactory();

    @Override
    public void rovaOrganizationalRolesService(Holder<Request> request, Holder<Response> response) {
        LOG.info("Organizational Roles request received");

        long startTime = System.currentTimeMillis();

        String serviceUuid = dataProvider.handleOrganizationalRoles(request.value.getDelegateIdentifier(),
                request.value.getOrganizationIdentifier(),
                getService(), getEndUserId(),
                getRequestId(), response);

        logRolesRequest(request, response, serviceUuid, startTime, System.currentTimeMillis());
    }

    private String getEndUserId() {
        return getHeaderValue(factory.createUserId("").getName());
    }

    private String getRequestId() {
        String clientStr = getClientHeaderValue(factory.createClient(
                factory.createXRoadClientIdentifierType()).getName(), "/");

        return clientStr + ";" + getHeaderValue(factory.createId("").getName());
    }

    private String getService() {
        return getClientHeaderValue(factory.createClient(
                factory.createXRoadClientIdentifierType()).getName(), "_");
    }

    private void logRolesRequest(Holder<Request> request,
            Holder<Response> response, String serviceUuid, long startTime, long endTime) {

        Logger.LogMap logMap = LOG.infoMap();

        logMap.add(END_USER, getEndUserId());

        boolean organizationIdsPresent = false;
        if (request.value != null && request.value.getOrganizationIdentifier() != null) {
            organizationIdsPresent = !request.value.getOrganizationIdentifier().isEmpty();
        }
        logMap.add(ACTION, organizationIdsPresent ? Ypa.ACTION_ROLES_BY_ID : Ypa.ACTION_ROLES);

        logMap.add(SERVICE_ID, getService());
        logMap.add(SERVICE_UUID, serviceUuid);
        logMap.add(SERVICE_REQUEST_IDENTIFIER, getRequestId());
        logMap.add(DURATION, Long.toString(endTime - startTime));

        if (response.value != null) {
            JAXBElement<String> expMsgElement = response.value.getExceptionMessage();
            String expMsg;
            if (expMsgElement != null && isNotBlank(expMsg = expMsgElement.getValue())) {
                if (expMsg.contains(EngineDataProvider.INVALID_HETU_MSG)) {
                    expMsg = new StringBuilder(expMsg)
                            .append(" ")
                            .append(request.value.getDelegateIdentifier())
                            .toString();
                }
                logMap.add(ERRORSTR, expMsg);
                logMap.level(ERROR);
            }

            OrganizationListType orgListType = response.value.getOrganizationList();
            if (orgListType != null && orgListType.getOrganization() != null) {
                logMap.add(RESULT, convertToString(orgListType.getOrganization()));
            } else {
                logMap.add(RESULT, "no_valid_response");
                logMap.level(ERROR);
            }
        } else {
            logMap.add(RESULT, "no_valid_response");
            logMap.add(ERRORSTR, "Creating response.value failed");
            logMap.level(ERROR);
        }

        logMap.log();
    }

    private String convertToString(List<OrganizationalRolesType> organizations) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");

        for (OrganizationalRolesType orgRolesType : organizations) {
            sb.append("{");

            sb.append("organization_roles=[");
            if (orgRolesType.getRoles() != null && orgRolesType.getRoles().getRole() != null) {
                sb.append(String.join(", ", orgRolesType.getRoles().getRole()));
            }
            sb.append("], organization_name=");
            sb.append(orgRolesType.getName());
            sb.append(", organization_id=");
            sb.append(orgRolesType.getOrganizationIdentifier());

            sb.append("}, ");
        }

        if (sb.toString().endsWith(", ")) {
            sb.delete(sb.length() - 2, sb.length());
        }

        sb.append("]");

        return sb.toString();
    }

}
