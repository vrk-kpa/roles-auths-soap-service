package fi.vm.kapa.rova.soap;

import static fi.vm.kapa.rova.logging.Logger.Field.*;
import static fi.vm.kapa.rova.logging.Logger.Level.ERROR;

import java.util.Iterator;

import javax.jws.WebService;
import javax.xml.ws.Holder;

import org.springframework.stereotype.Component;

import fi.vm.kapa.rova.logging.Logger;
import fi.vm.kapa.xml.rova.api.delegate.DecisionReasonType;
import fi.vm.kapa.xml.rova.api.delegate.ObjectFactory;
import fi.vm.kapa.xml.rova.api.delegate.Request;
import fi.vm.kapa.xml.rova.api.delegate.Response;
import fi.vm.kapa.xml.rova.api.delegate.RovaDelegatePortType;

@WebService(endpointInterface = "fi.vm.kapa.xml.rova.api.delegate.RovaDelegatePortType")
@Component("rovaDelegateService")
public class DelegateApi extends AbstractSoapService implements RovaDelegatePortType {

    Logger LOG = Logger.getLogger(DelegateApi.class);

    ObjectFactory factory = new ObjectFactory();

    @Override
    public void rovaDelegateService(Holder<Request> request, Holder<Response> response) {
        // this info is needed for creating a new requestId for logging at the beginning of request chain
        LOG.info("rovaDelegateService called");

        long startTime = System.currentTimeMillis();

        dataProvider.handleDelegate(request.value.getDelegateIdentifier(),
                getService(), getEndUserId(), getRequestId(), response);

        logDelegateRequest(request, response, startTime, System.currentTimeMillis());
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
            Holder<Response> response, long startTime, long endTime) {

        Logger.LogMap logMap = LOG.infoMap();
        
        logMap.add(END_USER, getEndUserId());
        logMap.add(SERVICE_ID, getService());
        logMap.add(SERVICE_REQUEST_IDENTIFIER, getRequestId());
        logMap.add(DURATION, Long.toString(endTime - startTime));
        
        if (response.value != null) {
            logMap.add(AUTHORIZATION_RESULT, response.value.getAuthorization() != null ? response.value.getAuthorization().toString() : "null");
            
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
            logMap.add(AUTHORIZATION_RESULT, "no_valid_response");
            logMap.level(ERROR);
        }

        logMap.log();
    }
}
