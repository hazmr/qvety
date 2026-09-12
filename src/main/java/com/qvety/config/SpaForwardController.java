package com.qvety.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Deep links like /clients/123 must load the Angular app, not 404.
 * Anything without a dot and not under /api or /actuator forwards to index.html.
 */
@Controller
public class SpaForwardController {

    @RequestMapping({"/{path:^(?!api|actuator|v3|swagger-ui)[^\\.]*}", "/{path:^(?!api|actuator|v3|swagger-ui)[^\\.]*}/**"})
    public String forward() {
        return "forward:/index.html";
    }
}
