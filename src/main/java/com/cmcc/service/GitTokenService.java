package com.cmcc.service;

import com.alibaba.fastjson.JSONObject;
import com.cmcc.domain.GitToken;
import com.cmcc.domain.RedisKey;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.client.urlconnection.HTTPSProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.*;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

/**
 * Created by zmcc on 17/3/8.
 */
@Service
public class GitTokenService {

    @Value("${git.token.clientid}")
    private String clientId;

    @Value("${git.token.clientsecret}")
    private String clientSec;

    @Value("${git.token.returnurl}")
    private String returnUrl;

    @Value("${git.token.url}")
    private String tokenUrl;

    @Value("${git.api.projects}")
    private String projectsUrl;

    @Value("${git.api.branches}")
    private String branchUrl;

    @Value("${git.api.tree}")
    private String treeUrl;

    @Value("${docker.images}")
    private String imageUrl;

    @Value("${k8s.service}")
    private String k8sServiceUrl;

    @Value("${k8s.deployment}")
    private String k8sDeploymentUrl;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private final RestTemplate restTemplate;

    public GitTokenService(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.build();
    }

    /**
     * 根据code获取token
     *
     * @param code
     * @return
     */
    public GitToken getToken(String code) {
        String token = redisTemplate.opsForValue().get(RedisKey.CODE_TO_TOKEN + code);
        if (token != null) {
            GitToken t = JSONObject.parseObject(token, GitToken.class);
            return t;
        }

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("client_id", clientId);
        map.add("client_secret", clientSec);
        map.add("code", code);
        map.add("grant_type", "authorization_code");
        map.add("redirect_uri", returnUrl);

        GitToken t = restTemplate.postForObject(tokenUrl, map, GitToken.class);
        redisTemplate.opsForValue().set(RedisKey.CODE_TO_TOKEN + code, JSONObject.toJSONString(t));
        if (t.getExpires_in() != 0) {
            redisTemplate.expire(RedisKey.CODE_TO_TOKEN + code, t.getExpires_in(), TimeUnit.SECONDS);
        }
        return t;
    }

    /**
     * 获取用户的项目列表
     *
     * @param code
     * @return
     */
    public Object getProjects(String code) {
        GitToken token = getToken(code);
        return restTemplate.getForObject(projectsUrl + "?access_token=" + token.getAccess_token(), Object.class);
    }


    /**
     * 获取项目分支
     *
     * @param pid
     * @param code
     * @return
     */
    public Object getBranches(String pid, String code) {
        GitToken token = getToken(code);
        return restTemplate.getForObject(String.format(branchUrl, pid) + "?access_token=" + token.getAccess_token(), Object.class);
    }

    public Object getTree(String pid, String branch, String path, String code) {
        GitToken token = getToken(code);
        String url = String.format(treeUrl, pid) + "?access_token=" + token.getAccess_token();
        if (branch != null) {
            url += "&ref_name=" + branch;
        }
        if (path != null) {
            url += "&path=" + path;
        }
        return restTemplate.getForObject(url, Object.class);
    }

    /**
     * 获取镜像列表
     *
     * @return
     */
    public Object getImages() {
        return restTemplate.getForObject(imageUrl, Object.class);
    }


    /**
     * 获取服务列表
     */
    public Object getServices(String namespace) {
        Client client = getClient();
        String url = String.format(k8sServiceUrl, namespace);
        System.out.println("请求服务列表地址:" + url);

        ClientResponse rep = client.resource(url)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .header("Authorization", "Bearer e5a2415ceb7386aa")
                .get(ClientResponse.class);
        String entity = rep.getEntity(String.class);
        System.out.println("获取services返回:" + entity);
        return entity;
    }


    /**
     * 调用k8s部署镜像
     *
     * @param appname
     * @param name
     * @param podnum
     * @param containerPort
     * @param nodePort
     */
    public String deploy(String appname, String name,
                         String podnum, String containerPort,
                         String nodePort, String namespace) {
        InputStream is = null;
        Scanner sc = null;
        try {
            // 创建service
            is = GitTokenService.class.getClassLoader().getResourceAsStream("service.json");
            sc = new Scanner(is).useDelimiter("\\A");
            String serviceTemplate = sc.hasNext() ? sc.next() : "";

            serviceTemplate = serviceTemplate.replaceAll("<0>", appname);
            serviceTemplate = serviceTemplate.replaceAll("<1>", namespace);
            serviceTemplate = serviceTemplate.replaceAll("<2>", containerPort);
            if (nodePort != null && !nodePort.isEmpty()) {
                serviceTemplate = serviceTemplate.replaceAll("<3>", "type: NodePort,");
                serviceTemplate = serviceTemplate.replaceAll("<4>", "nodePort: " + nodePort + ",");
            } else {
                serviceTemplate = serviceTemplate.replaceAll("<3>", "");
                serviceTemplate = serviceTemplate.replaceAll("<4>", "");
            }

            System.out.println("创建service请求参数:" + serviceTemplate);

            Client client = getClient();

            ClientResponse rep = client.resource(String.format(k8sServiceUrl, namespace))
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .header("Authorization", "Bearer e5a2415ceb7386aa")
                    .entity(serviceTemplate).post(ClientResponse.class);
            String entity = rep.getEntity(String.class);
            System.out.println("创建service返回:" + entity);


            StringBuilder sb = new StringBuilder();
            sb.append(entity);
            sb.append("\n");

            // 创建deployment
            is = GitTokenService.class.getClassLoader().getResourceAsStream("deployment.json");
            sc = new Scanner(is).useDelimiter("\\A");
            String deployTemplate = sc.hasNext() ? sc.next() : "";
            deployTemplate = deployTemplate.replaceAll("<0>", appname);
            deployTemplate = deployTemplate.replaceAll("<1>", namespace);
            deployTemplate = deployTemplate.replaceAll("<2>", podnum);
            deployTemplate = deployTemplate.replaceAll("<3>", name);
            deployTemplate = deployTemplate.replaceAll("<4>", containerPort);

            System.out.println("创建deployment请求参数:" + deployTemplate);

            rep = client.resource(String.format(k8sDeploymentUrl, namespace))
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .header("Authorization", "Bearer e5a2415ceb7386aa")
                    .entity(deployTemplate).post(ClientResponse.class);
            entity = rep.getEntity(String.class);
            System.out.println("创建deployment返回:" + entity);
            sb.append(entity);

            rep.close();
            return sb.toString();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (sc != null) {
                sc.close();
            }
        }
        return null;
    }


    /**
     * 获取https配置的client
     *
     * @return
     */

    public static Client getClient() {
        try {
            System.setProperty("https.protocols", "TLSv1.2");
            TrustManager[] tms = new TrustManager[]{new InsecureTrustManager()};
            HostnameVerifier hostnameVerifier = HttpsURLConnection.getDefaultHostnameVerifier();
            ClientConfig config = new DefaultClientConfig();
            SSLContext ctx = SSLContext.getInstance("SSL");
            ctx.init(null, tms, null);
            config.getProperties().put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES, new HTTPSProperties(hostnameVerifier, ctx));
            Client client = Client.create(config);
            return client;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}

class InsecureTrustManager implements X509TrustManager {
    /**
     * {@inheritDoc}
     */
    @Override
    public void checkClientTrusted(final X509Certificate[] chain, final String authType) throws CertificateException {
        // Everyone is trusted!
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void checkServerTrusted(final X509Certificate[] chain, final String authType) throws CertificateException {
        // Everyone is trusted!
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
    }
}
