package fi.vm.kapa.rova.soap;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.ws.WebServiceContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;

import com.sun.xml.ws.api.message.Header;
import com.sun.xml.ws.api.message.HeaderList;
import com.sun.xml.ws.developer.JAXWSProperties;

import fi.vm.kapa.rova.soap.model.ClientHeader;
import fi.vm.kapa.rova.soap.providers.DataProvider;

abstract class AbstractSoapService extends SpringBeanAutowiringSupport {

	@Autowired
	DataProvider dataProvider;

	@Resource
	WebServiceContext context;

	@PostConstruct
	public void init() {
		SpringBeanAutowiringSupport.processInjectionBasedOnCurrentContext(this);
	}

	protected String getHeaderValue(QName header) {
		HeaderList hl = (HeaderList) context.getMessageContext().get(
				JAXWSProperties.INBOUND_HEADER_LIST_PROPERTY);
		Header h = hl.get(header, true);
		return h.getStringContent();
	}

	protected String getClientHeaderValue(QName header) {
		String result = null;
		HeaderList hl = (HeaderList) context.getMessageContext().get(
				JAXWSProperties.INBOUND_HEADER_LIST_PROPERTY);
		Header h = hl.get(header, true);
		try {
			JAXBContext jc = JAXBContext.newInstance(ClientHeader.class);
			Unmarshaller unmarshaller = jc.createUnmarshaller();
			JAXBElement<ClientHeader> jb = unmarshaller.unmarshal(h.readHeader(), ClientHeader.class);
			ClientHeader ch = jb.getValue();
			result = ch.getServiceName();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}
}
