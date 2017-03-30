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

import fi.vm.kapa.rova.engine.Hpa;
import fi.vm.kapa.rova.logging.Logger;
import fi.vm.kapa.rova.soap.providers.EngineDataProvider;
import fi.vm.kapa.xml.rova.api.authorization.*;
import org.springframework.stereotype.Component;

import javax.jws.WebService;
import javax.xml.bind.JAXBElement;
import javax.xml.ws.Holder;
import java.util.Iterator;

import static fi.vm.kapa.rova.logging.Logger.Field.*;
import static fi.vm.kapa.rova.logging.Logger.Level.ERROR;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@WebService(endpointInterface = "fi.vm.kapa.xml.rova.api.authorization.RovaAuthorizationPortType")
@Component("rovaAuthorizationService")
public class AuthorizationApi extends AbstractSoapService implements RovaAuthorizationPortType {
    private final Logger LOG = Logger.getLogger(AuthorizationApi.class);

    private ObjectFactory factory = new ObjectFactory();

    @Override
    public void rovaAuthorizationService(Holder<Request> request, Holder<RovaAuthorizationResponse> response) {
        LOG.info("Authorization request received");
        long startTime = System.currentTimeMillis();

        String serviceUuid = dataProvider.handleAuthorization(request.value.getDelegateIdentifier(),
                request.value.getPrincipalIdentifier(), request.value.getIssue(), getService(),
                getEndUserId(), getRequestId(), response);

        logAuthorizationRequest(request, response, serviceUuid, startTime, System.currentTimeMillis());
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

    private void logAuthorizationRequest(Holder<Request> request,
            Holder<RovaAuthorizationResponse> response, String serviceUuid, long startTime, long endTime) {

        Logger.LogMap logMap = LOG.infoMap();

        logMap.add(END_USER, getEndUserId());
        logMap.add(ACTION, Hpa.ACTION_AUTHORIZATION);
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
                            .append("/")
                            .append(request.value.getPrincipalIdentifier())
                            .toString();
                }
                logMap.add(ERRORSTR, expMsg);
                logMap.level(ERROR);
            }

            String auth = response.value.getAuthorization() != null
                    ? response.value.getAuthorization().toString()
                    : "NA";
            logMap.add(RESULT, auth);

            if (response.value.getReason() != null) {
                StringBuilder rb = new StringBuilder();
                for (Iterator<DecisionReasonType> iter = response.value.getReason().iterator(); iter.hasNext();) {
                    DecisionReasonType drt = iter.next();
                    rb.append(drt.getValue());
                    if (iter.hasNext()) {
                        rb.append(",");
                    }
                }
                logMap.add(REASONS, rb.toString());
            }
        } else {
            logMap.add(RESULT, "no_valid_response");
            logMap.add(ERRORSTR, "Creating response.value failed");
            logMap.level(ERROR);
        }

        logMap.log();
    }
}
