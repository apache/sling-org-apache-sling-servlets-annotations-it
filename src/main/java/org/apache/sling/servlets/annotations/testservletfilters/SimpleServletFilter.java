package org.apache.sling.servlets.annotations.testservletfilters;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.servlets.annotations.SlingServletFilter;
import org.apache.sling.servlets.annotations.SlingServletFilterScope;
import org.osgi.service.component.annotations.Component;

@Component
@SlingServletFilter(scope=SlingServletFilterScope.REQUEST)
public class SimpleServletFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
       chain.doFilter(request, response);
       if ((request instanceof SlingHttpServletRequest) && (response instanceof SlingHttpServletResponse)) {
            afterDoFilter((SlingHttpServletRequest) request, (SlingHttpServletResponse) response, chain);
       } else {
           throw new ServletException("Not a Sling HTTP request/response");
       }
    }

    private void afterDoFilter(SlingHttpServletRequest request, SlingHttpServletResponse response, FilterChain chain) {
        if (request.getRequestURI().endsWith("simplefilter")) {
            // increase status by 1
            response.setStatus(response.getStatus()+1);
        }
    }
    
    @Override
    public void destroy() {
    }

}
