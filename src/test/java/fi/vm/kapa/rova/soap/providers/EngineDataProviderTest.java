package fi.vm.kapa.rova.soap.providers;

import java.lang.annotation.Annotation;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.Link.Builder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response.Status.Family;
import javax.xml.ws.Holder;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.http.HttpStatus;

import fi.vm.kapa.rova.rest.identification.RequestIdentificationFilter;

public class EngineDataProviderTest {

    private static final String REQUEST_ID = "test-request-id-5-1-2016";
    private static final String DELEGATE_ID = "150245-951K";
    private static final String PRINCIPAL_ID = "010144-002E";
    private static final String END_USER = "test-end-user";
    private static final String SERVICE_IDENTIFIER = "FI-DEV_COM_1516651-3_kaparova";
    private static final String ENGINE_URL = "http://höpöhpö:8001/rest/";

    @Test
    public void testHandleDelegateRestRequest() {

        List<Object> mocks = getMocks(
                ENGINE_URL + "hpa/delegate/" + SERVICE_IDENTIFIER + "/" + END_USER + "/" + DELEGATE_ID);

        EngineDataProvider engineDataProvider = getEnginedataProvider((Client) mocks.get(0));
        Holder<fi.vm.kapa.xml.rova.api.delegate.Response> responseHolder = new Holder<fi.vm.kapa.xml.rova.api.delegate.Response>(
                null);
        EasyMock.replay(mocks.toArray());
        engineDataProvider.handleDelegate(DELEGATE_ID, SERVICE_IDENTIFIER, END_USER, REQUEST_ID, responseHolder);
        EasyMock.verify(mocks.toArray());
        Assert.assertNotNull(responseHolder.value);
    }

    @Test
    public void testHandleAuthorizationRequest() {
        List<Object> mocks = getMocks(ENGINE_URL + "hpa/authorization/" + SERVICE_IDENTIFIER + "/" // + END_USER + "/"
                + DELEGATE_ID + "/" + PRINCIPAL_ID);
        EngineDataProvider engineDataProvider = getEnginedataProvider((Client) mocks.get(0));
        Holder<fi.vm.kapa.xml.rova.api.authorization.RovaAuthorizationResponse> responseHolder = new Holder<fi.vm.kapa.xml.rova.api.authorization.RovaAuthorizationResponse>(
                null);
        
        EasyMock.replay(mocks.toArray());
        engineDataProvider.handleAuthorization(DELEGATE_ID, PRINCIPAL_ID, SERVICE_IDENTIFIER, END_USER, REQUEST_ID,
                responseHolder);
        EasyMock.verify(mocks.toArray());
        Assert.assertNotNull(responseHolder.value);
    }

    private EngineDataProvider getEnginedataProvider(Client clientMock) {
        EngineDataProvider engineDataProvider = new EngineDataProvider() {
            @Override
            protected Client getClient() {
                return clientMock;
            }
        };
        engineDataProvider.setEngineApiKey("abcdefghijklmn");
        engineDataProvider.setEngineUrl(ENGINE_URL);
        engineDataProvider.setRequestAliveSeconds(50);

        return engineDataProvider;
    }

    private List<Object> getMocks(String expectedUrl) {
        List<Object> mocks = new ArrayList<>();

        Client clientMock = EasyMock.createStrictMock(Client.class);

        WebTarget webTargetMock = EasyMock.createStrictMock(WebTarget.class);
        EasyMock.expect(clientMock.target(expectedUrl)).andReturn(webTargetMock).once();
        EasyMock.expect(webTargetMock.queryParam("requestId", REQUEST_ID)).andReturn(webTargetMock).once();
        EasyMock.expect(webTargetMock.register(EasyMock.anyObject(RequestIdentificationFilter.class)))
                .andReturn(webTargetMock).once();

        Invocation.Builder invocationBuilderMock = EasyMock.createStrictMock(Invocation.Builder.class);
        EasyMock.expect(webTargetMock.request(MediaType.APPLICATION_JSON)).andReturn(invocationBuilderMock).once();
        EasyMock.expect(invocationBuilderMock.get()).andReturn(getInvocationResponse()).once();

        mocks.add(clientMock);
        mocks.add(webTargetMock);
        mocks.add(invocationBuilderMock);

        return mocks;
    }

    private javax.ws.rs.core.Response getInvocationResponse() {
        javax.ws.rs.core.Response resp = new javax.ws.rs.core.Response() {

            @Override
            public int getStatus() {
                return HttpStatus.FOUND.value();
            }

            @Override
            public StatusType getStatusInfo() {
                StatusType statusType = new StatusType() {

                    @Override
                    public int getStatusCode() {
                        return getStatus();
                    }

                    @Override
                    public String getReasonPhrase() {
                        return "Reason from mocked response";
                    }

                    @Override
                    public Family getFamily() {
                        throw new UnsupportedOperationException();
                    }
                };
                return statusType;
            }

            @Override
            public Object getEntity() {
                return new Object();
            }

            @Override
            public <T> T readEntity(Class<T> entityType) {
                throw new UnsupportedOperationException();
            }

            @Override
            public <T> T readEntity(GenericType<T> entityType) {
                throw new UnsupportedOperationException();
            }

            @Override
            public <T> T readEntity(Class<T> entityType, Annotation[] annotations) {
                throw new UnsupportedOperationException();
            }

            @Override
            public <T> T readEntity(GenericType<T> entityType, Annotation[] annotations) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean hasEntity() {
                return false;
            }

            @Override
            public boolean bufferEntity() {
                throw new UnsupportedOperationException();
            }

            @Override
            public void close() {

            }

            @Override
            public MediaType getMediaType() {
                throw new UnsupportedOperationException();
            }

            @Override
            public Locale getLanguage() {
                throw new UnsupportedOperationException();
            }

            @Override
            public int getLength() {
                throw new UnsupportedOperationException();
            }

            @Override
            public Set<String> getAllowedMethods() {
                throw new UnsupportedOperationException();
            }

            @Override
            public Map<String, NewCookie> getCookies() {
                throw new UnsupportedOperationException();
            }

            @Override
            public EntityTag getEntityTag() {
                throw new UnsupportedOperationException();
            }

            @Override
            public Date getDate() {
                return new Date();
            }

            @Override
            public Date getLastModified() {
                throw new UnsupportedOperationException();
            }

            @Override
            public URI getLocation() {
                throw new UnsupportedOperationException();
            }

            @Override
            public Set<Link> getLinks() {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean hasLink(String relation) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Link getLink(String relation) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Builder getLinkBuilder(String relation) {
                throw new UnsupportedOperationException();
            }

            @Override
            public MultivaluedMap<String, Object> getMetadata() {
                throw new UnsupportedOperationException();
            }

            @Override
            public MultivaluedMap<String, String> getStringHeaders() {
                throw new UnsupportedOperationException();
            }

            @Override
            public String getHeaderString(String name) {
                throw new UnsupportedOperationException();
            }

        };
        return resp;
    }

}
