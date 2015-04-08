package fi.vm.kapa.rova.soap.providers;

import java.util.List;

import javax.xml.ws.Holder;

import fi.vm.kapa.xml.rova.api.AuthorizationType;
import fi.vm.kapa.xml.rova.api.DecisionReasonType;
import fi.vm.kapa.xml.rova.api.Principal;

public interface DataProvider {

	AuthorizationType getAuthorizationTypeResponse(String delegateId, String principalId, String industry, String service, String issue, String endUserId, Holder<List<DecisionReasonType>> reason);
	
	Principal getPrincipalResponse(String personId, String industry, String service, String issue, String endUserId, Holder<List<DecisionReasonType>> reason);
}
