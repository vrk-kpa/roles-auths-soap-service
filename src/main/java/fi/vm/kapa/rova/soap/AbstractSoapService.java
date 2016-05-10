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

    protected String getClientHeaderValue(QName header, String concatStr) {
        String result = null;
        HeaderList hl = (HeaderList) context.getMessageContext().get(
                JAXWSProperties.INBOUND_HEADER_LIST_PROPERTY);
        Header h = hl.get(header, true);
        try {
            JAXBContext jc = JAXBContext.newInstance(ClientHeader.class);
            Unmarshaller unmarshaller = jc.createUnmarshaller();
            JAXBElement<ClientHeader> jb = unmarshaller.unmarshal(h.readHeader(), ClientHeader.class);
            ClientHeader ch = jb.getValue();
            result = ch.getServiceName(concatStr);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

}
