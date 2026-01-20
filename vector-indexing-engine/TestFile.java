<dependencies>

    <!-- Spring Boot -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter</artifactId>
    </dependency>

    <!-- ===================== -->
    <!-- Spring AI -->
    <!-- ===================== -->

    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-core</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-azure-openai</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-vector-store-file</artifactId>
    </dependency>

    <!-- ===================== -->
    <!-- Azure AD (Certificate Auth) -->
    <!-- ===================== -->

    <dependency>
        <groupId>com.azure</groupId>
        <artifactId>azure-identity</artifactId>
        <version>1.12.0</version>
    </dependency>

    <!-- ===================== -->
    <!-- HTTP Client (Proxy + TLS) -->
    <!-- ===================== -->

    <dependency>
        <groupId>org.apache.httpcomponents.client5</groupId>
        <artifactId>httpclient5</artifactId>
        <version>5.3</version>
    </dependency>

    <!-- ===================== -->
    <!-- PEM Parsing -->
    <!-- ===================== -->

    <dependency>
        <groupId>org.bouncycastle</groupId>
        <artifactId>bcprov-jdk18on</artifactId>
        <version>1.78</version>
    </dependency>

    <dependency>
        <groupId>org.bouncycastle</groupId>
        <artifactId>bcpkix-jdk18on</artifactId>
        <version>1.78</version>
    </dependency>


spring:
  ai:
    azure:
      openai:
        endpoint: https://<your-resource>.openai.azure.com/
        embedding:
          deployment-name: text-embedding-3-large
          model: text-embedding-3-large

azure:
  tenant-id: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  client-id: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  cert-pem: /etc/certs/azure-client.pem

proxy:
  host: proxy.corp.internal
  port: 8080


 package com.example.security;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;

import java.io.FileReader;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

public final class PemLoader {

    private PemLoader() {}

    /**
     * Load certificate + private key from a single PEM file
     */
    public static LoadedPem load(Path pemPath) throws Exception {

        PrivateKey privateKey = null;
        X509Certificate certificate = null;

        try (PEMParser parser = new PEMParser(new FileReader(pemPath.toFile()))) {
            Object obj;
            while ((obj = parser.readObject()) != null) {

                if (obj instanceof PrivateKeyInfo pkInfo) {
                    privateKey = new JcaPEMKeyConverter()
                            .getPrivateKey(pkInfo);
                }

                if (obj instanceof X509CertificateHolder certHolder) {
                    certificate = new JcaX509CertificateConverter()
                            .getCertificate(certHolder);
                }
            }
        }

        if (privateKey == null || certificate == null) {
            throw new IllegalStateException(
                    "PEM file must contain BOTH a private key and a certificate"
            );
        }

        return new LoadedPem(privateKey, certificate);
    }

    /**
     * Load PEM into a KeyStore (for proxy mTLS)
     */
    public static KeyStore loadKeyStore(Path pemPath) throws Exception {

        LoadedPem pem = load(pemPath);

        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null);

        keyStore.setKeyEntry(
                "client",
                pem.privateKey(),
                null,
                new java.security.cert.Certificate[]{pem.certificate()}
        );

        return keyStore;
    }

    /**
     * Holder for cert + key
     */
    public record LoadedPem(
            PrivateKey privateKey,
            X509Certificate certificate
    ) {}
}


package com.example.config;

import com.example.security.PemLoader;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.impl.routing.DefaultProxyRoutePlanner;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.ssl.SSLContexts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.SSLContext;
import java.nio.file.Path;

@Configuration
public class ProxyHttpClientConfig {

    @Value("${proxy.host}")
    private String proxyHost;

    @Value("${proxy.port}")
    private int proxyPort;

    @Value("${azure.cert-pem}")
    private String pemPath;

    @Bean
    public HttpClient httpClient() throws Exception {

        // Load client cert + key for proxy mTLS
        var keyStore = PemLoader.loadKeyStore(Path.of(pemPath));

        SSLContext sslContext = SSLContexts.custom()
                .loadKeyMaterial(keyStore, null)
                .build();

        SSLConnectionSocketFactory sslSocketFactory =
                SSLConnectionSocketFactoryBuilder.create()
                        .setSslContext(sslContext)
                        .build();

        PoolingHttpClientConnectionManager connectionManager =
                PoolingHttpClientConnectionManagerBuilder.create()
                        .setSSLSocketFactory(sslSocketFactory) // ✅ THIS IS THE KEY LINE
                        .build();

        return HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setRoutePlanner(
                        new DefaultProxyRoutePlanner(
                                new HttpHost(proxyHost, proxyPort)
                        )
                )
                .build();
    }
}


package com.example.config;

import com.azure.identity.ClientCertificateCredential;
import com.azure.identity.ClientCertificateCredentialBuilder;
import com.example.security.PemLoader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;

@Configuration
public class AzureAdAuthConfig {

    @Value("${azure.tenant-id}")
    private String tenantId;

    @Value("${azure.client-id}")
    private String clientId;

    @Value("${azure.cert-pem}")
    private String pemPath;

    @Bean
    public ClientCertificateCredential clientCertificateCredential() throws Exception {

        var pem = PemLoader.load(Path.of(pemPath));

        return new ClientCertificateCredentialBuilder()
                .tenantId(tenantId)
                .clientId(clientId)
                .pemCertificate(
                        pem.certificate(),
                        pem.privateKey()
                )
                .build();
    }
}


package com.example.config;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.routing.DefaultProxyRoutePlanner;
import org.apache.hc.client5.http.ssl.ClientTlsStrategyBuilder;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.ssl.SSLContexts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.SSLContext;
import java.nio.file.Path;

@Configuration
public class ProxyHttpClientConfig {

    @Value("${proxy.host}")
    private String proxyHost;

    @Value("${proxy.port}")
    private int proxyPort;

    @Value("${azure.cert-pem}")
    private String pemPath;

    @Bean
    public HttpClient httpClient() throws Exception {

        var keyStore = com.example.security.PemLoader
                .loadKeyStore(Path.of(pemPath));

        SSLContext sslContext = SSLContexts.custom()
                .loadKeyMaterial(keyStore, null)
                .build();

        return HttpClients.custom()
                .setRoutePlanner(
                        new DefaultProxyRoutePlanner(
                                new HttpHost(proxyHost, proxyPort)
                        )
                )
                .setTlsStrategy(
                        ClientTlsStrategyBuilder.create()
                                .setSslContext(sslContext)
                                .build()
                )
                .build();
    }
}

@Configuration
public class EmbeddingConfig {

    @Bean
    public EmbeddingModel embeddingModel(AzureOpenAiEmbeddingModel model) {
        return model;
    }
}




package com.example.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;

import jakarta.annotation.PreDestroy;
import java.io.File;

@Configuration
public class VectorStoreConfig {

    @Value("${vectorstore.file}")
    private String vectorStoreFile;

    private SimpleVectorStore store;

    @Bean
    public SimpleVectorStore vectorStore(EmbeddingModel embeddingModel) {

        store = new SimpleVectorStore(embeddingModel);

        File file = new File(vectorStoreFile);
        if (file.exists()) {
            store.load(file);
        }

        return store;
    }

    @PreDestroy
    public void persist() {
        if (store != null) {
            store.save(new File(vectorStoreFile));
        }
    }
}


