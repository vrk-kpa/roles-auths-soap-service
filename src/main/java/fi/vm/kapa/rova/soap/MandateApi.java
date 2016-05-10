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

import javax.jws.WebService;
import javax.xml.ws.Holder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fi.vm.kapa.rova.logging.Logger;
import fi.vm.kapa.rova.soap.providers.VareDataProvider;
import fi.vm.kapa.xml.vare.api.mandate.ObjectFactory;
import fi.vm.kapa.xml.vare.api.mandate.Request;
import fi.vm.kapa.xml.vare.api.mandate.VareMandatePortType;
import fi.vm.kapa.xml.vare.api.mandate.VareMandateResponse;

@WebService(endpointInterface = "fi.vm.kapa.xml.vare.api.mandate.VareMandatePortType")
@Component("vareMandateService")
public class MandateApi extends AbstractSoapService implements VareMandatePortType {
    private static final Logger LOG = Logger.getLogger(MandateApi.class);

    private ObjectFactory factory = new ObjectFactory();

    @Autowired
    private VareDataProvider vareDataProvider;

    @Override
    public void vareMandateService(Holder<Request> request, Holder<VareMandateResponse> response) {
        LOG.info("Mandate request received");

        long startTime = System.currentTimeMillis();
        
        vareDataProvider.handleMandate(request, getEndUserId(), getRequestId(), response);
        
        logMandateRequest(request, response, startTime, System.currentTimeMillis());
        
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

    private void logMandateRequest(Holder<Request> request,
            Holder<VareMandateResponse> response, long startTime, long endTime) {
        
        Logger.LogMap logmap =  LOG.infoMap();

        String endUserId = getEndUserId();
        if (endUserId.length() == 11) {
            String birthDayPart = endUserId.substring(0, 6);
            if (birthDayPart.matches("^\\d+$")) {
                endUserId = birthDayPart;
            }
        }
        logmap.set(END_USER, endUserId);
        
        logmap.set(SERVICE_ID, getService());
        
        logmap.set(SERVICE_REQUEST_IDENTIFIER, getRequestId());
        
        if (response.value != null) {
            logmap.set(RESULT, response.value.getMandate().toString());

        } else {
            logmap.set(RESULT, "no_valid_response");
        }

        logmap.set(DURATION, endTime - startTime);

        logmap.log();
    }
}
