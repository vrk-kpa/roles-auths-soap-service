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

import fi.vm.kapa.rova.logging.Logger;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;
import javax.xml.soap.*;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;
import java.util.*;

@Component("xroadHeaderHandler")
public class SoapHeaderCopyHandler implements SOAPHandler<SOAPMessageContext> {

    private static Logger LOG = Logger.getLogger(SoapHeaderCopyHandler.class);

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
                    headerList.add((SOAPHeaderElement) childs.next());
                }
                messageContext.put("original-soap-headers", headerList);
            } catch (SOAPException e) {
                LOG.error(e.getMessage(), e);
            }
        } else if (outboundProperty.booleanValue()) {
            try {
                soapEnv = soapMsg.getSOAPPart().getEnvelope();
                if (soapEnv.getHeader() == null) {
                    soapEnv.addHeader();
                }
                SOAPHeader header = soapEnv.getHeader();

                @SuppressWarnings("unchecked")
                List<SOAPHeaderElement> headerList = (List<SOAPHeaderElement>) messageContext.get("original-soap-headers");
                for (SOAPHeaderElement h : headerList) {
                    header.addChildElement((h));
                }
                soapMsg.saveChanges();
            } catch (SOAPException e) {
                LOG.error(e.getMessage(), e);
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
