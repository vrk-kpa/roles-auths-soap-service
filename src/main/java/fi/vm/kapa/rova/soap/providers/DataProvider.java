package fi.vm.kapa.rova.soap.providers;

import java.util.List;

import javax.xml.ws.Holder;

import fi.vm.kapa.xml.rova.api.authorization.AuthorizationType;
import fi.vm.kapa.xml.rova.api.delegate.AuthorizationResponseType;
import fi.vm.kapa.xml.rova.api.delegate.Principal;

public interface DataProvider {

	void handleAuthorizationTypeResponse(
			String delegateId,
			String principalId,
			String service,
			String endUserId,
			String requestId,
			Holder<AuthorizationType> authorizationTypeResponse,
			Holder<List<fi.vm.kapa.xml.rova.api.authorization.DecisionReasonType>> reason);

	void handlePrincipalResponse(String personId, String service,
			String endUserId, String requestId,
			Holder<Principal> principal,
			Holder<AuthorizationResponseType> authorizationResponseType);
}
