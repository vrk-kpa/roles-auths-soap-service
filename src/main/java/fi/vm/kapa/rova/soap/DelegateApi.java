package fi.vm.kapa.rova.soap;


import javax.jws.WebService;
import javax.xml.ws.Holder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import fi.vm.kapa.xml.rova.api.delegate.ObjectFactory;
import fi.vm.kapa.xml.rova.api.delegate.Request;
import fi.vm.kapa.xml.rova.api.delegate.Response;
import fi.vm.kapa.xml.rova.api.delegate.RovaDelegatePortType;

@WebService(endpointInterface = "fi.vm.kapa.xml.rova.api.delegate.RovaDelegatePortType")
@Component("rovaDelegateService")
public class DelegateApi extends AbstractSoapService implements
		RovaDelegatePortType {
	Logger LOG = LoggerFactory.getLogger(DelegateApi.class);

	ObjectFactory factory = new ObjectFactory();

	@Override
	public void rovaDelegateService(Holder<Request> request,
			Holder<Response> response) {
		dataProvider.handleDelegate(request.value.getDelegateIdentifier(),
				getService(), getEndUserId(), getRequestId(), response);
		LOG.debug("delegateIdentifier from request: "+ request.value.getDelegateIdentifier());
	}

	private String getEndUserId() {
		return getHeaderValue(factory.createUserId("").getName());
	}

	private String getRequestId() {
		return getHeaderValue(factory.createId("").getName());
	}

	private String getService() {
		return getClientHeaderValue(factory.createClient(
				factory.createSdsbClientIdentifierType()).getName());
	}
}
