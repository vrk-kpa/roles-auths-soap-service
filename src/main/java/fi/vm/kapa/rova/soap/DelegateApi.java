package fi.vm.kapa.rova.soap;


import java.util.Iterator;

import javax.jws.WebService;
import javax.xml.ws.Holder;

import org.springframework.stereotype.Component;

import fi.vm.kapa.rova.logging.Logger;
import fi.vm.kapa.xml.rova.api.delegate.DecisionReasonType;
import fi.vm.kapa.xml.rova.api.delegate.ObjectFactory;
import fi.vm.kapa.xml.rova.api.delegate.PrincipalType;
import fi.vm.kapa.xml.rova.api.delegate.Request;
import fi.vm.kapa.xml.rova.api.delegate.Response;
import fi.vm.kapa.xml.rova.api.delegate.RovaDelegatePortType;

@WebService(endpointInterface = "fi.vm.kapa.xml.rova.api.delegate.RovaDelegatePortType")
@Component("rovaDelegateService")
public class DelegateApi extends AbstractSoapService implements RovaDelegatePortType {
	Logger LOG = Logger.getLogger(DelegateApi.class, Logger.SOAP_SERVICE);

	ObjectFactory factory = new ObjectFactory();

	@Override
	public void rovaDelegateService(Holder<Request> request, Holder<Response> response) {
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
		
		sb.append(" service=");
		sb.append(getService());

		sb.append(" requestId=");
		sb.append(getRequestId());

		sb.append(" delegate=");
		if (request.value != null && request.value.getDelegateIdentifier() != null) {
			int endIndex = request.value.getDelegateIdentifier().length();
			endIndex = endIndex > 6 ? 6 : endIndex;
			sb.append(request.value.getDelegateIdentifier().substring(0, endIndex));
		} else {
			sb.append("no_valid_delegate_identifier");
		}

		if (response.value != null) {
			sb.append(" auth=");
			sb.append(response.value.getAuthorization());
			
			sb.append(" principals=");
			if (response.value.getPrincipalList() != null && response.value.getPrincipalList().getPrincipal() != null) {
				sb.append("[");
				for (Iterator<PrincipalType> iter = response.value.getPrincipalList().getPrincipal().iterator(); iter.hasNext(); ) {
					PrincipalType principal = iter.next();
					if (principal.getIdentifier() != null) {
						sb.append(principal.getIdentifier().substring(0, 6));
					} else {
						sb.append("no_valid_principal_identifier");
					}
					if (iter.hasNext()) {
						sb.append(",");
					}
				}
				sb.append("]");
			}
			
			sb.append(" reasons=[");
			if (response.value.getReason() != null) {
				for(Iterator<DecisionReasonType> iter = response.value.getReason().iterator(); iter.hasNext(); ) {
					DecisionReasonType drt = iter.next();
					sb.append(drt.getValue());
					if (iter.hasNext()) {
						sb.append(",");
					}
				}
			}
			sb.append("] ");
			
		} else {
			sb.append(" no_valid_response ");
		}

		sb.append(endTime - startTime);
		sb.append(" ms");
		
		LOG.info(sb.toString());
	}

}
