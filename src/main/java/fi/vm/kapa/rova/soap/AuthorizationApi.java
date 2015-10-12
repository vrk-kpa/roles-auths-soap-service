package fi.vm.kapa.rova.soap;

import static fi.vm.kapa.rova.logging.Logger.Field.*;
import static fi.vm.kapa.rova.logging.Logger.Level.ERROR;

import java.util.Iterator;
import fi.vm.kapa.rova.logging.Logger;
import org.springframework.stereotype.Component;

import javax.jws.WebService;
import javax.servlet.http.HttpServletRequest;
import javax.xml.ws.Holder;

import org.springframework.beans.factory.annotation.Autowired;

import fi.vm.kapa.xml.rova.api.authorization.DecisionReasonType;
import fi.vm.kapa.xml.rova.api.authorization.ObjectFactory;
import fi.vm.kapa.xml.rova.api.authorization.Request;
import fi.vm.kapa.xml.rova.api.authorization.RovaAuthorizationPortType;
import fi.vm.kapa.xml.rova.api.authorization.RovaAuthorizationResponse;

@WebService(endpointInterface = "fi.vm.kapa.xml.rova.api.authorization.RovaAuthorizationPortType")
@Component("rovaAuthorizationService")
public class AuthorizationApi extends AbstractSoapService implements RovaAuthorizationPortType {
    private static final Logger LOG = Logger.getLogger(AuthorizationApi.class);
    
    @Autowired
    private HttpServletRequest httpReq;

    private ObjectFactory factory = new ObjectFactory();

    @Override
    public void rovaAuthorizationService(Holder<Request> request, Holder<RovaAuthorizationResponse> response) {
        // this info is needed for creating a new requestId for logging at the beginning of request chain
        LOG.info("rovaAuthorizationService called");

        long startTime = System.currentTimeMillis();

        dataProvider.handleAuthorization(request.value.getDelegateIdentifier(),
                request.value.getPrincipalIdentifier(), getService(),
                getEndUserId(), getRequestId(), response);

        logAuthorizationRequest(request, response, startTime, System.currentTimeMillis());
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
            Holder<RovaAuthorizationResponse> response, long startTime, long endTime) {

        Logger.LogMap logMap = LOG.infoMap();
        
        logMap.add(END_USER, getEndUserId());
        logMap.add(SERVICE_ID, getService());
        logMap.add(SERVICE_REQUEST_IDENTIFIER, getRequestId());
        logMap.add(DURATION, Long.toString(endTime - startTime));
        
        if (response.value != null) {
            logMap.add(AUTHORIZATION_RESULT, response.value.getAuthorization().toString());
            
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
            logMap.add(AUTHORIZATION_RESULT, "no_valid_response");
            logMap.level(ERROR);
        }

        logMap.log();
    }
}
