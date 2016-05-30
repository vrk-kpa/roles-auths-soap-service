/**
 * The MIT License
 * Copyright (c) 2016 Population Register Centre
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package fi.vm.kapa.rova.soap.handlers;

import java.util.Optional;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFault;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;
import javax.xml.ws.soap.SOAPFaultException;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sun.xml.ws.api.message.Header;
import com.sun.xml.ws.api.message.HeaderList;
import com.sun.xml.ws.developer.JAXWSProperties;

/**
 * Raises a SOAP fault if the xro:userId header element doesn't exist or is empty.
 */
@Component("verifyXRoadUserIdSoapHandler")
public class VerifyXRoadUserIdSoapHandler implements SOAPHandler<SOAPMessageContext> {
    
    private static final QName XROAD_USERID_HEADER = new QName("http://x-road.eu/xsd/xroad.xsd", "userId");
    
    @Override
    public boolean handleMessage(SOAPMessageContext messageContext) {
        SOAPMessage message = messageContext.getMessage();
        HeaderList hl = (HeaderList) messageContext.get(
                JAXWSProperties.INBOUND_HEADER_LIST_PROPERTY);
        Header header = Optional.ofNullable(hl.get(XROAD_USERID_HEADER, true))
                .orElseThrow(() -> buildSOAPFaultException(message, "X-Road userId header is not present"));

        if (StringUtils.isEmpty(header.getStringContent()))
            throw buildSOAPFaultException(message, "X-Road userId header is empty");
        
        return true;
    }
    
    private SOAPFaultException buildSOAPFaultException(SOAPMessage message, String reason) {
        try {
            SOAPBody soapBody = message.getSOAPPart().getEnvelope().getBody();
            SOAPFault soapFault = soapBody.addFault();
            soapFault.setFaultString(reason);
            throw new SOAPFaultException(soapFault); 
        } catch (SOAPException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean handleFault(SOAPMessageContext context) {
        return true;
    }

    @Override
    public void close(MessageContext context) {
    }

    @Override
    public Set<QName> getHeaders() {
        return null;
    }
}
