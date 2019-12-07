package com.inclination.http.rest;

import com.alibaba.fastjson.JSON;
import com.inclination.http.exception.RestClientException;
import lombok.extern.slf4j.Slf4j;
import com.inclination.http.config.RestTemplateConfig;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 *
 * @author: Apple
 * @date: 2019/2/27
 * @time: 19:37
 * To change this templates use File | Settings | File Templates.
 * @description:
 */
@Slf4j
@Component
@ConditionalOnBean(RestTemplateConfig.class)
public class RestApiUtils {

    /**
     * 发送http请求
     */
    @Autowired
    private RestTemplate restTemplate;

    /**
     * 获取一个application/x-www-form-urlencoded头
     */
    public HttpHeaders buildBasicFormHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        return headers;
    }
    /**
     * 获取一个包含cookie的头
     * @param cookieList
     */
    public HttpHeaders buildBasicCookieHeaders(List<String> cookieList) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.put(HttpHeaders.COOKIE, cookieList);
        httpHeaders.set("ignore-identity","true");
        return httpHeaders;
    }

    /**
     * 获取一个application/json头
     */
    private HttpHeaders buildBasicJsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        MimeType mimeType = MimeTypeUtils.parseMimeType(MediaType.APPLICATION_JSON_VALUE);
        MediaType mediaType = new MediaType(mimeType.getType(), mimeType.getSubtype(), Charset.forName("UTF-8"));
        headers.setContentType(mediaType);
        return headers;
    }

    /**
     * 获取一个text/html头
     */
    private HttpHeaders buildBasicHtmlHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_HTML);
        return headers;
    }
    /**
     * 执行请求
     *
     * @param url 请求地址
     * @param method 请求方式
     * @param httpHeaders 请求头
     * @param body 请求数据
     * @param responseType 返回数据类型,返回bean时指定Class
     * @throws RestClientException RestClient异常，包含状态码和非200的返回内容
     */
    public <T> T exchange(String url, HttpMethod method, HttpHeaders httpHeaders, Object body, Class<T> responseType) {
        try {
            HttpEntity<?> requestEntity = new HttpEntity(body, httpHeaders);
            requestEntity = convert(requestEntity);
            String responseBody=restTemplate.exchange(url, method, requestEntity, String.class).getBody();
            log.warn(responseBody.toString());
            return JSON.parseObject(responseBody, responseType);
        } catch (Exception e) {
            throw new RestClientException(e);
        }
    }

    /**
     * 对bean对象转表单模型做处理
     */
    private HttpEntity<?> convert(HttpEntity<?> requestEntity) {
        Object body = requestEntity.getBody();
        HttpHeaders headers = requestEntity.getHeaders();
        if (body == null) {
            return requestEntity;
        }
        if (headers == null || !MediaType.APPLICATION_FORM_URLENCODED.equals(headers.getContentType())) {
            return requestEntity;
        }
        if (body instanceof String) {
            return requestEntity;
        }
        if (body instanceof Collection) {
            return requestEntity;
        }
        if (body instanceof Map) {
            MultiValueMap<String, String> multiValueMap = new LinkedMultiValueMap<>();
            Map<String, ?> bodyTemp = (Map<String, ?>) body;
            for (String key : bodyTemp.keySet()) {
                multiValueMap.add(key, MapUtils.getString(bodyTemp, key));
            }
            requestEntity = new HttpEntity<>(multiValueMap, headers);
            return requestEntity;
        }
        MultiValueMap<String, Object> formEntity = new LinkedMultiValueMap<>();
        Field[] fields = body.getClass().getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
            String name = fields[i].getName();
            String value = null;
            try {
                value = BeanUtils.getProperty(body, name);
            } catch (Exception e) {
                e.printStackTrace();
            }
            formEntity.add(name, value);
        }
        return new HttpEntity<>(formEntity, headers);
    }

    /**
     * 将object[]转换为http上送的格式
     * @param objs 上送的object[]
     * @return string
     */
    private String convert(Object[] objs) {
        boolean flag = false;
        StringBuilder sb = new StringBuilder();
        for (Object obj : objs) {
            if (null != obj) {
                if (flag) {
                    sb.append(",");
                }
                sb.append(obj);
                flag = true;
            }
        }
        return sb.toString();
    }

    public String doParseUrl(Map body){
        StringBuffer sb=new StringBuffer("?");
        boolean flag = false;
        for (Iterator itr = ((Map) body).keySet().iterator(); itr.hasNext(); ) {
            String key = (String) itr.next();
            Map<?, ?> bodyTemp = (Map) body;
            Object value = bodyTemp.get(key);
            String valueTemp = null;
            if (null != value) {
                if (value instanceof String) {
                    valueTemp = String.valueOf(value);
                }else if(value instanceof Integer){
                    valueTemp = String.valueOf(value);
                } else if (value.getClass().isArray()) {
                    valueTemp = convert((Object[]) value);
                } else if (value instanceof Collection) {
                    valueTemp = convert(((Collection) value).toArray());
                }
            } else {
                continue;
            }
            if (flag) {
                sb.append("&");
            }
            sb.append(key);
            sb.append("=");
            sb.append(valueTemp);
            flag = true;
        }
        return sb.toString();
    }




    public <T> T doGetObjectByAccessToken(String url,HttpEntity<?> httpEntity, Class<T> responseType){
        try {
            ResponseEntity<String> responseEntity = restTemplate.exchange(url, HttpMethod.GET, httpEntity, String.class);
            return JSON.parseObject(responseEntity.getBody(), responseType);
        }catch (Exception e){
            throw new RestClientException(e);
        }
    }

    /**
     * get请求，通过url和参数，得到返回对象
     * @param url 请求的路径
     * @param responseType 返回值的类型
     * @param <T> 接受返回值的类型
     * @return T
     */
    public <T> T getForObject(String url, Class<T> responseType) {
        try {
            String responseBody = restTemplate.getForObject(url, String.class);
            return JSON.parseObject(responseBody, responseType);
        }catch(Exception e){
            throw new RestClientException(e);
        }
    }

    /**
     * 通过传入object或者map来自动调用外围接口，如果是object则发送json
     * @param url 接口地址
     * @param body 上送的报文
     * @param responseType 返回值类型
     * @param <T> 返回值
     * @return 返回
     */
    public <T> T getForOject(String url,Object body,Class<T> responseType){
        // 请求参数是Map时转为在srvApi中拼接处理.
        StringBuilder sb = new StringBuilder(url);
        if (body instanceof Map) {
            sb.append(doParseUrl((Map) body));
            return exchange(sb.toString(), HttpMethod.GET, buildBasicJsonHeaders(),null, responseType);
        }
        return exchange(sb.toString(), HttpMethod.GET,buildBasicJsonHeaders(), JSON.toJSON(body), responseType);
    }



    /**
     * 通过往http中添加accesstoken的方式来获取对象
     * @param url 请求的路径
     * @param token token值
     * @param responseType 返回值的类型
     * @param <T> T
     * @return T
     */
    public <T> T getForObjectByAccessToken(String url, Object body,String token, Class<T> responseType) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("accesstoken", token);
        HttpEntity<String> httpEntity = new HttpEntity<>(null, httpHeaders);
        HttpEntity<?> requestEntity = new HttpEntity(body);
        requestEntity = convert(requestEntity);
        return doGetObjectByAccessToken(url,requestEntity,responseType);
    }

    /**
     * 通过再http头中添加cookie来获取相关的对象
     * @param url 访问的url
     * @param cookieList cookie列表
     * @param responseType 返回值的类型
     * @param <T> 接受值的类型
     * @return t
     */
    public <T> T getForObjectByCookie(String url, List<String> cookieList, Class<T> responseType) {
        HttpHeaders httpHeaders=buildBasicCookieHeaders(cookieList);
        HttpEntity<String> httpEntity = new HttpEntity<>(null,httpHeaders);
       return doGetObjectByAccessToken(url,httpEntity,responseType);
    }

    /**
     * 通过传入map<Sring,Object>的方式获取调用外围接口
     * @param url 路径
     * @param paramMap 参数列表
     * @param responseType 返回值的类型
     * @param <T> 返回值类型
     * @return 返回值
     */
    public <T> T postForObject(String url, Map<String, Object> paramMap, Class<T> responseType) {
        try {
            String responseBody = restTemplate.postForObject(url, paramMap, String.class);
            return JSON.parseObject(responseBody, responseType);
        }catch (Exception e){
            throw new RestClientException(e);
        }
    }

    /**
     * 通过传入Object的格式调用外围接口
     * @param url 请求路径
     * @param param 对象参数
     * @param responseType 返回类型
     * @param <T> T接受
     * @return 返回值
     */
    public <T> T postForObject(String url, Object param, Class<T> responseType) {
        try {
            String responseBody = restTemplate.postForObject(url, param, String.class);
            log.warn(responseBody);
            return JSON.parseObject(responseBody, responseType);
        }catch (Exception e){
            throw new RestClientException(e);
        }
    }

    /**
     * 通过传入对象，并发送json的形式调用外围接口
     * @param url 请求路径
     * @param param 传入的对象
     * @param responseType 返回的类型
     * @param <T> 返回类型
     * @return 返回
     */
    public <T> T postForEntity(String url, Object param, Class<T> responseType) {
        HttpHeaders httpHeaders = buildBasicJsonHeaders();
        HttpEntity<String> httpEntity = new HttpEntity<String>(JSON.toJSONString(param),httpHeaders);
        try {
            ResponseEntity<String> responseBody = restTemplate.postForEntity(url, httpEntity, String.class);
            log.warn(responseBody.getBody());
            return JSON.parseObject(responseBody.getBody(), responseType);
        }catch (Exception e){
            throw new RestClientException(e);
        }
    }
}
