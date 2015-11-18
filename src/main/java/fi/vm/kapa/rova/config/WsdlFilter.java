
package fi.vm.kapa.rova.config;

import fi.vm.kapa.rova.logging.Logger;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import org.hibernate.service.spi.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

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

