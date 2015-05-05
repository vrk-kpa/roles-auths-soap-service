package fi.vm.kapa.rova.soap;

import java.util.logging.Logger;

import javax.jws.WebService;
import javax.xml.ws.Holder;

import org.springframework.stereotype.Component;

import fi.vm.kapa.xml.rova.api.delegate.ObjectFactory;
import fi.vm.kapa.xml.rova.api.delegate.Request;
import fi.vm.kapa.xml.rova.api.delegate.Response;
import fi.vm.kapa.xml.rova.api.delegate.RovaDelegatePortType;

@WebService(endpointInterface = "fi.vm.kapa.xml.rova.api.delegate.RovaDelegatePortType")
@Component("rovaDelegateService")
public class DelegateApi extends AbstractSoapService implements
		RovaDelegatePortType {
	Logger LOG = Logger.getLogger(DelegateApi.class.toString());

	ObjectFactory factory = new ObjectFactory();

	@Override
	public void rovaDelegateService(Holder<Request> request,
			Holder<Response> response) {
		dataProvider.handleDelegate(request.value.getDelegateIdentifier(),
				getService(), getEndUserId(), getRequestId(), response);
		System.out.println(request.value.getDelegateIdentifier());

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
