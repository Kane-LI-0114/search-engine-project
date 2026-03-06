package search.web;

import javax.servlet.*;
import java.io.IOException;

/**
 * Servlet filter to ensure consistent UTF-8 character encoding
 * across all requests and responses.
 */
public class CharacterEncodingFilter implements Filter {

    private String encoding = "UTF-8";

    @Override
    public void init(FilterConfig config) throws ServletException {
        String configEncoding = config.getInitParameter("encoding");
        if (configEncoding != null) {
            this.encoding = configEncoding;
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        request.setCharacterEncoding(encoding);
        response.setCharacterEncoding(encoding);
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // No resources to release
    }
}
