package fi.vm.kapa.rova.soap.providers;

import fi.vm.kapa.xml.rova.api.AuthorizationType;
import fi.vm.kapa.xml.rova.api.Principal;

public interface DataProvider {

	AuthorizationType getAuthorizationTypeResponse(String delegateId, String principalId, String industry, String service, String issue, String endUserId);
	
	Principal getPrincipalResponse(String personId, String industry, String service, String issue, String endUserId);
}
