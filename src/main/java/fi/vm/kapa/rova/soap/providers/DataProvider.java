package fi.vm.kapa.rova.soap.providers;

import javax.xml.ws.Holder;

import fi.vm.kapa.xml.rova.api.authorization.RovaAuthorizationResponse;
import fi.vm.kapa.xml.rova.api.delegate.Response;

public interface DataProvider {

    void handleAuthorization(
            String delegateId,
            String principalId,
            String service,
            String endUserId,
            String requestId,
            Holder<RovaAuthorizationResponse> response);

    void handleDelegate(String personId, String service,
            String endUserId, String requestId,
            Holder<Response> response);
    
    void handleOrganizationalRoles(String personId, String service,
            String endUserId, String requestId,
            Holder<fi.vm.kapa.xml.rova.api.orgroles.Response> response);
}
