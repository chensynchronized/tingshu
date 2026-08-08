package com.atguigu.tingshu.common.feign;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class FeignInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate requestTemplate) {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes != null){
            ServletRequestAttributes sra = (ServletRequestAttributes) requestAttributes;
            String token = sra.getRequest().getHeader("token");
            requestTemplate.header("token", token);
        }
    }
}
