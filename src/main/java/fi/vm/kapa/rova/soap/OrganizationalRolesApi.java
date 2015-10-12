package fi.vm.kapa.rova.soap;

import javax.jws.WebService;
import javax.xml.ws.Holder;

import org.springframework.stereotype.Component;

import fi.vm.kapa.rova.logging.Logger;
import fi.vm.kapa.xml.rova.api.orgroles.ObjectFactory;
import fi.vm.kapa.xml.rova.api.orgroles.Request;
import fi.vm.kapa.xml.rova.api.orgroles.Response;
import fi.vm.kapa.xml.rova.api.orgroles.RovaOrganizationalRolesPortType;

import static fi.vm.kapa.rova.logging.Logger.Field.*;

@WebService(endpointInterface = "fi.vm.kapa.xml.rova.api.orgroles.RovaOrganizationalRolesPortType")
@Component("rovaOrganizationalRolesService")
public class OrganizationalRolesApi extends AbstractSoapService implements RovaOrganizationalRolesPortType {

    Logger LOG = Logger.getLogger(OrganizationalRolesApi.class);

    ObjectFactory factory = new ObjectFactory();

    @Override
    public void rovaOrganizationalRolesService(Holder<Request> request, Holder<Response> response) {
        // this info is needed for creating a new requestId for logging at the beginning of request chain
        LOG.info("rovaOrganizationalRolesService called");

        long startTime = System.currentTimeMillis();

        dataProvider.handleOrganizationalRoles(request.value.getDelegateIdentifier(),
                request.value.getOrganizationIdentifierList(),
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
        StringBuilder sb = new StringBuilder();

        Logger.LogMap logMap = LOG.infoMap();

        logMap.add(END_USER, getEndUserId());
        logMap.add(SERVICE_ID, getService());
        logMap.add(SERVICE_REQUEST_IDENTIFIER, getRequestId());
        logMap.add(DURATION, Long.toString(endTime - startTime));

        sb.append(",service=");
        sb.append(getService());

        sb.append(",requestId=");
        sb.append(getRequestId());

        if (response.value != null) {
            sb.append(",auth=");
            sb.append(response.value.getOrganizationList());
        } else {
            sb.append(",no_valid_response,");
        }

        sb.append(",duration=");
        sb.append(endTime - startTime);

        LOG.info(sb.toString());
    }

}
