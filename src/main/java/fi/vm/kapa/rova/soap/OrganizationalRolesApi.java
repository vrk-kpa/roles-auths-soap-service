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

import static fi.vm.kapa.rova.logging.Logger.Field.DURATION;
import static fi.vm.kapa.rova.logging.Logger.Field.END_USER;
import static fi.vm.kapa.rova.logging.Logger.Field.RESULT;
import static fi.vm.kapa.rova.logging.Logger.Field.SERVICE_ID;
import static fi.vm.kapa.rova.logging.Logger.Field.SERVICE_REQUEST_IDENTIFIER;
import static fi.vm.kapa.rova.logging.Logger.Level.ERROR;

import javax.jws.WebService;
import javax.xml.ws.Holder;

import org.springframework.stereotype.Component;

import fi.vm.kapa.rova.logging.Logger;
import fi.vm.kapa.xml.rova.api.orgroles.ObjectFactory;
import fi.vm.kapa.xml.rova.api.orgroles.Request;
import fi.vm.kapa.xml.rova.api.orgroles.Response;
import fi.vm.kapa.xml.rova.api.orgroles.RovaOrganizationalRolesPortType;

@WebService(endpointInterface = "fi.vm.kapa.xml.rova.api.orgroles.RovaOrganizationalRolesPortType")
@Component("rovaOrganizationalRolesService")
public class OrganizationalRolesApi extends AbstractSoapService implements RovaOrganizationalRolesPortType {

    Logger LOG = Logger.getLogger(OrganizationalRolesApi.class);

    ObjectFactory factory = new ObjectFactory();

    @Override
    public void rovaOrganizationalRolesService(Holder<Request> request, Holder<Response> response) {
        LOG.info("Organizational Roles request received");

        long startTime = System.currentTimeMillis();

        dataProvider.handleOrganizationalRoles(request.value.getDelegateIdentifier(),
                request.value.getOrganizationIdentifier(),
                getService(), getEndUserId(),
                getRequestId(), response);

        logRolesRequest(request, response, startTime, System.currentTimeMillis());
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
            Holder<Response> response, long startTime, long endTime) {

        Logger.LogMap logMap = LOG.infoMap();

        logMap.add(END_USER, getEndUserId());
        logMap.add(SERVICE_ID, getService());
        logMap.add(SERVICE_REQUEST_IDENTIFIER, getRequestId());
        logMap.add(DURATION, Long.toString(endTime - startTime));

        if (response.value != null) {
            logMap.add(RESULT, response.value.getOrganizationList());
        } else {
            logMap.add(RESULT, "no_valid_response");
            logMap.level(ERROR);
        }
        logMap.log();
    }

}
