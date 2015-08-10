package fi.vm.kapa.rova.soap;

import java.util.Iterator;

import javax.jws.WebService;
import javax.xml.ws.Holder;

import org.springframework.stereotype.Component;

import fi.vm.kapa.rova.logging.Logger;
import fi.vm.kapa.xml.rova.api.authorization.DecisionReasonType;
import fi.vm.kapa.xml.rova.api.authorization.ObjectFactory;
import fi.vm.kapa.xml.rova.api.authorization.Request;
import fi.vm.kapa.xml.rova.api.authorization.RovaAuthorizationPortType;
import fi.vm.kapa.xml.rova.api.authorization.RovaAuthorizationResponse;

@WebService(endpointInterface = "fi.vm.kapa.xml.rova.api.authorization.RovaAuthorizationPortType")
@Component("rovaAuthorizationService")
public class AuthorizationApi extends AbstractSoapService implements
        RovaAuthorizationPortType {

    Logger LOG = Logger.getLogger(AuthorizationApi.class, Logger.SOAP_SERVICE);

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
        StringBuilder sb = new StringBuilder();

        sb.append("endUserId=");
        String endUserId = getEndUserId();
        if (endUserId.length() == 11) {
            String birthDayPart = endUserId.substring(0, 6);
            if (birthDayPart.matches("^\\d+$")) {
                endUserId = birthDayPart;
            }
        }
        sb.append(endUserId);

        sb.append(",service=");
        sb.append(getService());

        sb.append(",requestId=");
        sb.append(getRequestId());

        if (response.value != null) {
            sb.append(",auth=");
            sb.append(response.value.getAuthorization());

            sb.append(",reasons=[");
            if (response.value.getReason() != null) {
                for (Iterator<DecisionReasonType> iter = response.value.getReason().iterator(); iter.hasNext();) {
                    DecisionReasonType drt = iter.next();
                    sb.append(drt.getValue());
                    if (iter.hasNext()) {
                        sb.append(",");
                    }
                }
            }
            sb.append("]");

        } else {
            sb.append(",no_valid_response,");
        }

        sb.append(",duration=");
        sb.append(endTime - startTime);

        LOG.info(sb.toString());
    }
}
