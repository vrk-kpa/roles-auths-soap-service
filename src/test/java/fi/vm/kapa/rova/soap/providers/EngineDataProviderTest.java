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
package fi.vm.kapa.rova.soap.providers;

import fi.vm.kapa.rova.engine.HpaClient;
import fi.vm.kapa.rova.engine.model.hpa.AuthorizationInternal;
import fi.vm.kapa.rova.engine.model.hpa.DecisionReason;
import fi.vm.kapa.rova.engine.model.hpa.HpaDelegate;
import fi.vm.kapa.rova.engine.model.hpa.Principal;
import fi.vm.kapa.rova.external.model.AuthorizationType;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;

import javax.xml.ws.Holder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;

public class EngineDataProviderTest {

    private static final String REQUEST_ID = "test-request-id-5-1-2016";
    private static final String DELEGATE_ID = "150245-951K";
    private static final String PRINCIPAL_ID = "010144-002V";
    private static final String END_USER = "test-end-user";
    private static final String SERVICE_IDENTIFIER = "FI-DEV_COM_1516651-3_kaparova";

    @Test
    public void testHandleDelegateRestRequest() {

        EngineDataProvider provider = new EngineDataProvider();
        HpaClient hpaClient = EasyMock.createMock(HpaClient.class);
        EasyMock.expect(hpaClient.getDelegate(anyString(), anyString(), anyString()))
                .andReturn(getHpaDelegate()).once();
        EasyMock.replay(hpaClient);
        provider.setHpaClient(hpaClient);

        Holder<fi.vm.kapa.xml.rova.api.delegate.Response> responseHolder = new Holder<>(null);

        provider.handleDelegate(DELEGATE_ID, SERVICE_IDENTIFIER, END_USER, REQUEST_ID, responseHolder);
        EasyMock.verify(hpaClient);
        Assert.assertNotNull(responseHolder.value);
    }

    @Test
    public void testHandleAuthorizationRequest() {
        EngineDataProvider provider = new EngineDataProvider();
        HpaClient hpaClient = EasyMock.createMock(HpaClient.class);
        EasyMock.expect(hpaClient.getAuthorization(anyString(), anyString(), anyString(), anyString(), anyObject()))
                .andReturn(getAuthorization()).once();
        EasyMock.replay(hpaClient);
        provider.setHpaClient(hpaClient);

        Holder<fi.vm.kapa.xml.rova.api.authorization.RovaAuthorizationResponse> responseHolder =
                new Holder<fi.vm.kapa.xml.rova.api.authorization.RovaAuthorizationResponse>(null);

        provider.handleAuthorization(DELEGATE_ID, PRINCIPAL_ID, Collections.emptyList(),
                SERVICE_IDENTIFIER, END_USER, REQUEST_ID, responseHolder);
        EasyMock.verify(hpaClient);
        Assert.assertNotNull(responseHolder.value);
    }

    private HpaDelegate getHpaDelegate() {
        return new HpaDelegate() {
            @Override
            public String getDelegateId() {
                return DELEGATE_ID;
            }

            @Override
            public AuthorizationType getAuthorizationType() {
                return AuthorizationType.ALLOWED;
            }

            @Override
            public List<Principal> getPrincipal() {
                return new ArrayList<>();
            }

            @Override
            public List<DecisionReason> getReasons() {
                return new ArrayList<>();
            }

            @Override
            public String getServiceUuid() {
                return "TEST-SERVICE-UUID";
            }
        };
    }

    private AuthorizationInternal getAuthorization() {
        AuthorizationInternal auth = new AuthorizationInternal();
        auth.setResult(AuthorizationType.ALLOWED);
        return auth;
    }
}
