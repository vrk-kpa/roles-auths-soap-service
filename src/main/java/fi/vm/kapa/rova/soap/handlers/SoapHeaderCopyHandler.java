package fi.vm.kapa.rova.soap.handlers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPHeaderElement;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;

import org.springframework.stereotype.Component;

@Component("xroadHeaderHandler")
public class SoapHeaderCopyHandler implements SOAPHandler<SOAPMessageContext> {
	public Set<QName> getHeaders() {
		return Collections.emptySet();
	}

	public boolean handleMessage(SOAPMessageContext messageContext) {
		Boolean outboundProperty = (Boolean) messageContext
				.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
		
		SOAPMessage soapMsg = messageContext.getMessage();
		SOAPEnvelope soapEnv;
		
		if (!outboundProperty.booleanValue()) {
			try {
				soapEnv = soapMsg.getSOAPPart().getEnvelope();
				SOAPHeader soapHeader = soapEnv.getHeader();
				List<SOAPHeaderElement> headerList = new ArrayList<SOAPHeaderElement>();
				@SuppressWarnings("rawtypes")
				Iterator childs = soapHeader.getChildElements();
				while (childs.hasNext()) {
					headerList.add((SOAPHeaderElement)childs.next());
				}
				messageContext.put("original-soap-headers", headerList);
			} catch (SOAPException e) {
				e.printStackTrace();
			}
		} else if (outboundProperty.booleanValue()) {
			try {
				soapEnv = soapMsg.getSOAPPart().getEnvelope();
				if (soapEnv.getHeader() == null) {
					soapEnv.addHeader();
				}
				SOAPHeader header = soapEnv.getHeader();
				
				@SuppressWarnings("unchecked")
				List<SOAPHeaderElement> headerList = (List<SOAPHeaderElement>)messageContext.get("original-soap-headers");
				for (SOAPHeaderElement h : headerList) {
					header.addChildElement((h));
				}
				soapMsg.saveChanges();
			} catch (SOAPException e) {
				e.printStackTrace();
			}
		}
		return true;
	}

	public boolean handleFault(SOAPMessageContext messageContext) {
		return true;
	}

	public void close(MessageContext messageContext) {
	}
}
