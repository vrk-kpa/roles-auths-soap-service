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

package fi.vm.kapa.rova.config;

import fi.vm.kapa.rova.logging.Logger;
import org.hibernate.service.spi.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

@Component
public class WsdlFilter implements Filter {

    private static Logger log = Logger.getLogger(WsdlFilter.class);

    public static final String PARAM_REQUEST_PATH = "wsdl_filter_request_path";
    public static final String PARAM_RESOURCE_PATH = "wsdl_filter_resource_path";
    public static final String PARAM_REQUEST_FILES = "wsdl_filter_request_files";

    private Map<String, String> mappings = new HashMap<>();

    @Autowired
    private Environment env;

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {

        log.debug("Filtering content in WSDL filter.");
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        String path = request.getRequestURI();
        String target = mappings.get(path);

        if (target != null) {
            log.debug("Mapping found: " + request + " -> "+ target + ".");
            writeResponse(target, servletResponse);
        } else {
            log.debug("Calling next filter in chain.");
            filterChain.doFilter(servletRequest, servletResponse);
        }
    }

    private void writeResponse(String resource, ServletResponse servletResponse) throws IOException {
        log.info("Loading resource: " + resource + ".");
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        InputStream is = cl.getResourceAsStream(resource);
        if (is == null) {
            throw new ServiceException("WSDL resource not found: " + resource + ".");
        }
        servletResponse.setContentType("text/xml");
        OutputStream os = servletResponse.getOutputStream();
        byte [] buffer = new byte[1024];
        int cnt = is.read(buffer);
        while (cnt > 0) {
            os.write(buffer, 0, cnt);
            cnt = is.read(buffer);
        }
        os.close();
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

        log.info("Initializing WSDL filter.");
        String requestPath = env.getRequiredProperty(PARAM_REQUEST_PATH);
        String resourcePath = env.getRequiredProperty(PARAM_RESOURCE_PATH);
        String requestFiles = env.getRequiredProperty(PARAM_REQUEST_FILES);
        
        if (requestFiles != null && !"".equals(requestFiles.trim())) {
            for (String file : requestFiles.split("\\s*,\\s*")) {
                mappings.put(requestPath + "/" + file, resourcePath + "/" + file);
            }
        }

        for (String key : mappings.keySet()) {
            log.info("Static file mapping : " + key + " -> " + mappings.get(key));
        }
    }

    @Override
    public void destroy() {
        // NOP
    }
}

