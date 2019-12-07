package com.inclination.http.config;

import com.inclination.http.properties.RestTemplateProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.Header;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeader;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * 支持:
 * 1. httpclient连接池，依赖httpclient 4.5.3
 * 2. ribbon负载均衡.
 * 3. 支持设置连接超时、读写超时、最大并发数、路由并发数
 * @author 123
 */
@Slf4j
@Configuration
@ConditionalOnBean(RestTemplateProperties.class)
public class RestTemplateConfig {


  private RestTemplateProperties restTemplateConfigProperties;

  public RestTemplateConfig(RestTemplateProperties restTemplateConfigProperties){
      this.restTemplateConfigProperties=restTemplateConfigProperties;
  }

  /**
   * httpClient连接池.
   */
  @Bean(name = "poolingHttpClientConnectionManager")
  public PoolingHttpClientConnectionManager poolingHttpClientConnectionManager() {

    PoolingHttpClientConnectionManager poolingHttpClientConnectionManager = new PoolingHttpClientConnectionManager();
    //并发连接数
    poolingHttpClientConnectionManager.setMaxTotal(5000);
    //同路由并发数
    poolingHttpClientConnectionManager.setDefaultMaxPerRoute(100);
    return poolingHttpClientConnectionManager;
  }

  @Bean(name = "httpClientBuilder")
  public HttpClientBuilder httpClientBuilder() {
    HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
    httpClientBuilder.setConnectionManager(poolingHttpClientConnectionManager());

    //重试, 默认次数2次,不开启
    HttpRequestRetryHandler requestRetryHandler = new DefaultHttpRequestRetryHandler(2, false);
    httpClientBuilder.setRetryHandler(requestRetryHandler);
    List<Header> headers = new ArrayList<Header>();
    headers.add(new BasicHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/31.0.1650.16 Safari/537.36"));
    headers.add(new BasicHeader("Accept-Encoding", "gzip,deflate"));
    headers.add(new BasicHeader("Accept-Language", "zh-CN"));
    headers.add(new BasicHeader("Connection", "close"));
    httpClientBuilder.setDefaultHeaders(headers);
    return httpClientBuilder;
  }

  @Bean(name = "httpClient")
  public HttpClient httpClient() {
    return httpClientBuilder().build();
  }

  @Bean(name = "clientHttpRequestFactory")
  public HttpComponentsClientHttpRequestFactory clientHttpRequestFactory() {
    HttpComponentsClientHttpRequestFactory clientHttpRequestFactory = new HttpComponentsClientHttpRequestFactory(httpClient());
    // httpClient连接配置

    int httpConnectTimeoutInt = Integer.parseInt(restTemplateConfigProperties.getHttpConnectTimeout());
    int readTimeoutInt = Integer.parseInt(restTemplateConfigProperties.getReadTimeout());
    int connectionRequestTimeoutInt = Integer.parseInt(restTemplateConfigProperties.getConnectionRequestTimeout());

    if (httpConnectTimeoutInt > 0) {
      log.info("===>>rest.connect.timeout: {}", restTemplateConfigProperties.getHttpConnectTimeout());
      clientHttpRequestFactory.setConnectTimeout(httpConnectTimeoutInt);
    }
    if (readTimeoutInt > 0) {
      log.info("===>>rest.read.timeout: {}", restTemplateConfigProperties.readTimeout);
      clientHttpRequestFactory.setReadTimeout(readTimeoutInt);
    }
    if (connectionRequestTimeoutInt > 0) {
      clientHttpRequestFactory.setConnectionRequestTimeout(connectionRequestTimeoutInt);
    }

    //缓冲请求数据，默认值是true。通过POST或者PUT大量发送数据时，建议将此属性更改为false，以免耗尽内存
    clientHttpRequestFactory.setBufferRequestBody(false);
    return clientHttpRequestFactory;
  }



  @Bean
  public RestTemplate simpleRestTemplate() {
    RestTemplate restTemplate = new RestTemplate();
    restTemplate.setRequestFactory(clientHttpRequestFactory());
    restTemplate.setErrorHandler(new DefaultResponseErrorHandler());
    return restTemplate;
  }
}
