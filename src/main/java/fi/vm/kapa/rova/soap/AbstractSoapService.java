package fi.vm.kapa.rova.soap;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.xml.namespace.QName;
import javax.xml.ws.WebServiceContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;

import com.sun.xml.ws.api.message.Header;
import com.sun.xml.ws.api.message.HeaderList;
import com.sun.xml.ws.developer.JAXWSProperties;

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
		HeaderList hl = (HeaderList)context.getMessageContext().get(JAXWSProperties.INBOUND_HEADER_LIST_PROPERTY);
		Header h = hl.get(header, true);
		return h.getStringContent();
	}

	protected String getClientHeaderValue(QName header) {
		StringBuilder result = new StringBuilder();
		HeaderList hl = (HeaderList)context.getMessageContext().get(JAXWSProperties.INBOUND_HEADER_LIST_PROPERTY);
		Header h = hl.get(header, true);
		String content = h.getStringContent();
		for (String s : content.split("\n")) {
			s = s.trim();
			if (s.length() > 0) {
				result.append(s);
				result.append("_");
			}
		}
		// trim the last separator and return
		return result.substring(0, result.length()-1);
	}

}
