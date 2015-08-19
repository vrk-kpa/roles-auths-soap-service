package fi.vm.kapa.rova.soap.providers;

import java.util.List;

import javax.xml.ws.Holder;

import fi.vm.kapa.xml.rova.api.orgroles.OrganizationType;

public interface OrganizationDataProvider {
    
    void handleOrganizationalRoles(String personId, List<String> organizationIds, String service,
            String endUserId, String requestId,
            Holder<fi.vm.kapa.xml.rova.api.orgroles.Response> response);
}
