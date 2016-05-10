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

import java.util.List;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.ws.Holder;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import fi.vm.kapa.rova.config.SpringProperties;
import fi.vm.kapa.rova.logging.Logger;
import fi.vm.kapa.rova.logging.LoggingClientRequestFilter;
import fi.vm.kapa.rova.rest.identification.RequestIdentificationFilter;
import fi.vm.kapa.rova.rest.validation.ValidationClientRequestFilter;
import fi.vm.kapa.rova.vare.model.MandateResponse;
import fi.vm.kapa.xml.vare.api.mandate.IssueUri;
import fi.vm.kapa.xml.vare.api.mandate.MandateType;
import fi.vm.kapa.xml.vare.api.mandate.ObjectFactory;
import fi.vm.kapa.xml.vare.api.mandate.Request;
import fi.vm.kapa.xml.vare.api.mandate.VareMandateResponse;

@Component
public class VareDataProvider implements SpringProperties { // DataProvider, 
    private static final Logger LOG = Logger.getLogger(VareDataProvider.class);
    
    private ObjectFactory vareFactory = new ObjectFactory();

    @Value(VARE_URL)
    private String vareUrl;

    @Value(VARE_API_KEY)
    private String vareApiKey;

    @Value(REQUEST_ALIVE_SECONDS)
    private Integer requestAliveSeconds;

//    @Override
    public void handleMandate(Holder<Request> request,  String endUserId,
            String requestId, Holder<VareMandateResponse> mandateResponse) {
        
        String delegateId = request.value.getDelegateIdentifier();
        String principalId = request.value.getPrincipalIdentifier();
        List<IssueUri> issueUris = request.value.getIssues().getIssue();
        Object[] issues = new Object[issueUris.size()];
        int i = 0;
        StringBuilder sb = new StringBuilder();
        for (IssueUri issueUri : issueUris) { // issueList
            issues[i++] = issueUri.getUri();

            sb.append(issueUri.getUri());
            sb.append(";");
        }
        
        String subject = request.value.getSubject();
        
        LOG.info(": delegateId="+ delegateId +", principalId="+ principalId +", issues="+ sb.toString() +", subject="+ subject);
        
        WebTarget webTarget = getClient().target(vareUrl +"vare/checkmandate/"+ endUserId +"/"
        + delegateId +"/"+ principalId +"/"+ subject).queryParam("requestId", requestId) // TODO requestId necessary here ???
        .queryParam("issues", issues);
        
        webTarget.register(new RequestIdentificationFilter(requestId, endUserId));
       
        Invocation.Builder invocationBuilder = webTarget.request(MediaType.APPLICATION_JSON);

        mandateResponse.value = vareFactory.createVareMandateResponse();
        mandateResponse.value.setMandate(MandateType.DISALLOWED);
        
        Response restResponse = invocationBuilder.get();

        if (restResponse.getStatus() == HttpStatus.OK.value()) {
            MandateResponse manResponse = restResponse.readEntity(MandateResponse.class);
            mandateResponse.value.setMandate(manResponse.equals(MandateResponse.ALLOWED) ? MandateType.ALLOWED : MandateType.DISALLOWED);
            LOG.info("Restful success: resp="+ manResponse);
        } else {
            LOG.error("got "+ restResponse.getStatus() +" from rest");
        }
        
    }

    private Client getClient() {
        ClientConfig clientConfig = new ClientConfig();
        Client client = ClientBuilder.newClient(clientConfig);
        client.register(JacksonFeature.class);
        client.register(new ValidationClientRequestFilter(vareApiKey, requestAliveSeconds, null));
        client.register(new LoggingClientRequestFilter());
        return client;
    }

}
