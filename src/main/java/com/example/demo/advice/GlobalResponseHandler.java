package com.example.demo.advice;

import com.example.demo.dto.HotelDto;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalResponseHandler implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType, Class<? extends HttpMessageConverter<?>> selectedConverterType, ServerHttpRequest request, ServerHttpResponse response) {
        List<String> allowedRoutes = List.of("/v3/api-docs", "/actuator"); // We do not change the bodies of the path with swagger or actuator.

        boolean isAllowed = allowedRoutes
                .stream()
                .anyMatch(route -> request.getURI().getPath().contains(route));

        if(body instanceof ApiResponse<?> || isAllowed) {
            return body; // The body could already be wrapped in ApiResponse. In that case we do not change anything.
        }

        return new ApiResponse<>(body);
    }
}
