package fi.vm.kapa.rova.soap.providers;

import fi.vm.kapa.xml.rova.api.authorization.RovaAuthorizationResponse;
import fi.vm.kapa.xml.rova.api.delegate.Response;

import javax.xml.ws.Holder;
import java.math.BigInteger;
import java.util.List;


public interface DataProvider {

    void handleAuthorization(
            String delegateId,
            String principalId,
            List<String> issues,
            String service,
            String endUserId,
            String requestId,
            Holder<RovaAuthorizationResponse> response);

    void handleDelegate(String personId, String service,
            String endUserId, String requestId,
            Holder<Response> response);
    
    void handleOrganizationalRoles(String personId, List<String> organizationIds, String service,
                                   String endUserId, BigInteger offset, BigInteger limit, String requestId,
                                   Holder<fi.vm.kapa.xml.rova.api.orgroles.Response> response);
    
}
