package com.inclination.properties;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/***
 * http配置类
 */
@Configuration
@ConfigurationProperties(prefix = "rest.http")
public class RestTemplateProperties {

    /**
     * 连接超时时间，毫秒
     */
    @Value("${rest.http.timeout:15000}")
    public String httpConnectTimeout;

    /**
     * 读写超时时间，毫秒, 数据读取超时时间，即SocketTimeout
     */
    @Value("${rest.http.read.timeout:60000}")
    public String readTimeout;
    /**
     * 连接不够用的等待时间，不宜过长，必须设置，比如连接不够用时，时间过长将是灾难性的
     */
    @Value("${rest.http.connnection.request.timeout:200}")
    public String connectionRequestTimeout;

    public String getHttpConnectTimeout() {
        return httpConnectTimeout;
    }

    public String getReadTimeout() {
        return readTimeout;
    }

    public String getConnectionRequestTimeout() {
        return connectionRequestTimeout;
    }

    public void setHttpConnectTimeout(String httpConnectTimeout) {
        this.httpConnectTimeout = httpConnectTimeout;
    }

    public void setReadTimeout(String readTimeout) {
        this.readTimeout = readTimeout;
    }

    public void setConnectionRequestTimeout(String connectionRequestTimeout) {
        this.connectionRequestTimeout = connectionRequestTimeout;
    }
}
