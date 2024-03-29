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

import fi.vm.kapa.xml.rova.api.authorization.RovaAuthorizationResponse;
import fi.vm.kapa.xml.rova.api.authorization.list.RovaAuthorizationListResponse;
import fi.vm.kapa.xml.rova.api.delegate.Response;

import javax.xml.ws.Holder;
import java.util.List;


public interface DataProvider {

    String handleAuthorization(
            String delegateId,
            String principalId,
            List<String> issues,
            String service,
            String endUserId,
            String requestId,
            Holder<RovaAuthorizationResponse> response);

    String handleAuthorizationList(
            String delegateId,
            String principalId,
            String service,
            String endUserId,
            String requestId,
            Holder<RovaAuthorizationListResponse> response);

    String handleDelegate(String personId, String service,
            String endUserId, String requestId,
            Holder<Response> response);
    
    String handleOrganizationalRoles(String personId, List<String> organizationIds, String service,
                                   String endUserId, String requestId,
                                   Holder<fi.vm.kapa.xml.rova.api.orgroles.Response> response);
    
}
