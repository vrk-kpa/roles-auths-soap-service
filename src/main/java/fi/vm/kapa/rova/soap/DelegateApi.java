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
import fi.vm.kapa.xml.rova.api.delegate.*;
import org.springframework.stereotype.Component;

import javax.jws.WebService;
import javax.xml.bind.JAXBElement;
import javax.xml.ws.Holder;
import java.util.Iterator;

import static fi.vm.kapa.rova.logging.Logger.Field.*;
import static fi.vm.kapa.rova.logging.Logger.Level.ERROR;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@WebService(endpointInterface = "fi.vm.kapa.xml.rova.api.delegate.RovaDelegatePortType")
@Component("rovaDelegateService")
public class DelegateApi extends AbstractSoapService implements RovaDelegatePortType {

    Logger LOG = Logger.getLogger(DelegateApi.class);

    ObjectFactory factory = new ObjectFactory();

    @Override
    public void rovaDelegateService(Holder<Request> request, Holder<Response> response) {
        LOG.info("Delegate request received");

        long startTime = System.currentTimeMillis();

        String serviceUuid = dataProvider.handleDelegate(request.value.getDelegateIdentifier(),
                getService(), getEndUserId(), getRequestId(), response);

        logDelegateRequest(request, response, serviceUuid, startTime, System.currentTimeMillis());
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

    private void logDelegateRequest(Holder<Request> request,
            Holder<Response> response, String serviceUuid, long startTime, long endTime) {

        Logger.LogMap logMap = LOG.infoMap();

        logMap.add(END_USER, getEndUserId());
        logMap.add(ACTION, Hpa.ACTION_DELEGATE);
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

            logMap.add(RESULT, response.value.getAuthorization() != null ? response.value.getAuthorization().toString() : "null");

            if (response.value.getPrincipalList() != null && response.value.getPrincipalList().getPrincipal() != null) {
                logMap.add(PRINCIPAL_COUNT, response.value.getPrincipalList().getPrincipal().size());
            } else {
                logMap.add(PRINCIPAL_COUNT, "-1");
            }

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
