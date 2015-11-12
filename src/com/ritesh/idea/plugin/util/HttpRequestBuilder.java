/*
 * Copyright 2015 Ritesh Kapoor
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ritesh.idea.plugin.util;

import com.google.common.io.CharStreams;
import com.google.gson.Gson;
import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Ritesh
 */
public class HttpRequestBuilder {
    public static final int CONNECT_TIMEOUT = 15000;

    private HttpRequestBase request;
    private URIBuilder urlBuilder = new URIBuilder();
    private String route = "";
    private List<NameValuePair> formParams = new ArrayList<>();
    private String fileParam;
    private String fileName;
    private byte[] fileBytes;
    private RequestConfig requestConfig;

    public HttpRequestBuilder route(String value) {
        route = route + "/" + value;
        return this;
    }

    public HttpRequestBuilder route(int value) {
        route = route + "/" + String.valueOf(value);
        return this;
    }

    public HttpRequestBuilder slash() {
        route = route + "/";
        return this;
    }

    public HttpRequestBuilder queryString(String param, Object value) {
        urlBuilder.addParameter(param, String.valueOf(value));
        return this;
    }

    public HttpRequestBuilder file(String param, String name, byte[] bytes) {
        fileParam = param;
        fileName = name;
        fileBytes = bytes;
        return this;
    }

    public HttpRequestBuilder field(String param, Object value) {
        formParams.add(new BasicNameValuePair(param, String.valueOf(value)));
        return this;
    }

    public HttpRequestBuilder header(String name, String value) {
        request.addHeader(name, value);
        return this;
    }

    public static HttpRequestBuilder post(String url) throws URISyntaxException {
        HttpRequestBuilder builder = new HttpRequestBuilder();
        builder.urlBuilder = new URIBuilder(url);
        builder.request = new HttpPost();
        return builder;
    }

    public static HttpRequestBuilder get(String url) throws URISyntaxException {
        HttpRequestBuilder builder = new HttpRequestBuilder();
        builder.urlBuilder = new URIBuilder(url);
        builder.request = new HttpGet();
        return builder;
    }


    public static HttpRequestBuilder put(String url) throws URISyntaxException {
        HttpRequestBuilder builder = new HttpRequestBuilder();
        builder.urlBuilder = new URIBuilder(url);
        builder.request = new HttpPut();
        return builder;
    }

    private HttpRequestBase getHttpRequest() throws URISyntaxException, UnsupportedEncodingException {
        if (!route.isEmpty()) urlBuilder.setPath(route);
        request.setURI(urlBuilder.build());
        if (request instanceof HttpPost) {
            if (fileParam != null) {
                MultipartEntityBuilder builder = MultipartEntityBuilder.create();
                for (NameValuePair formParam : formParams) {
                    builder.addTextBody(formParam.getName(), formParam.getValue());
                }
                HttpEntity entity = builder
                        .addBinaryBody(fileParam, fileBytes, ContentType.MULTIPART_FORM_DATA, fileName)
                        .build();
                ((HttpPost) request).setEntity(entity);
            } else if (!formParams.isEmpty()) {
                ((HttpPost) request).setEntity(new UrlEncodedFormEntity(formParams));
            }
        } else if (request instanceof HttpPut) {
            if (!formParams.isEmpty()) {
                ((HttpPut) request).setEntity(new UrlEncodedFormEntity(formParams));
            }
        }
        return request;

    }


    public <T> T asJson(Class<T> clazz) throws IOException, URISyntaxException {
        try (CloseableHttpClient client = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build()) {
            HttpRequestBase request = getHttpRequest();
            CloseableHttpResponse response = client.execute(request);
            Reader reader = new InputStreamReader(response.getEntity().getContent(), Consts.UTF_8);
            return new Gson().fromJson(reader, clazz);
        }
    }


    public String asString() throws IOException, URISyntaxException {
        try (CloseableHttpClient client = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build()) {
            HttpRequestBase request = getHttpRequest();
            CloseableHttpResponse response = client.execute(request);
            return CharStreams.toString(new InputStreamReader(response.getEntity().getContent()));
        }
    }

    public HttpRequestBase request() throws Exception {
        return getHttpRequest();
    }

    private HttpRequestBuilder() {
        requestConfig = RequestConfig.copy(RequestConfig.DEFAULT).setConnectTimeout(CONNECT_TIMEOUT).build();
    }

}
