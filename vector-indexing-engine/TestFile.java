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
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

public final class PemLoader {

    private PemLoader() {}

    public static LoadedPem load(Path pemPath) throws Exception {

        PrivateKey privateKey = null;
        X509Certificate certificate = null;

        try (PEMParser parser = new PEMParser(new FileReader(pemPath.toFile()))) {
            Object obj;
            while ((obj = parser.readObject()) != null) {

                if (obj instanceof PrivateKeyInfo pk) {
                    privateKey = new JcaPEMKeyConverter().getPrivateKey(pk);
                }

                if (obj instanceof X509CertificateHolder cert) {
                    certificate = new JcaX509CertificateConverter()
                            .getCertificate(cert);
                }
            }
        }

        if (privateKey == null || certificate == null) {
            throw new IllegalStateException(
                    "PEM must contain both CERTIFICATE and PRIVATE KEY"
            );
        }

        return new LoadedPem(privateKey, certificate);
    }

    public record LoadedPem(
            PrivateKey privateKey,
            X509Certificate certificate
    ) {}
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

import com.azure.core.credential.TokenCredential;
import org.apache.hc.client5.http.classic.HttpClient;
import org.springframework.ai.azure.openai.AzureOpenAiApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AzureOpenAiConfig {

    @Bean
    public AzureOpenAiApi azureOpenAiApi(
            TokenCredential credential,
            HttpClient proxyHttpClient
    ) {
        return new AzureOpenAiApi(
                AzureOpenAiApi.builder()
                        .credential(credential)
                        .httpClient(proxyHttpClient)
                        .build()
        );
    }
}


package com.example.config;

import org.springframework.ai.azure.openai.AzureOpenAiEmbeddingModel;
import org.springframework.ai.azure.openai.AzureOpenAiEmbeddingOptions;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmbeddingConfig {

    @Bean
    public EmbeddingModel embeddingModel(AzureOpenAiApi api) {
        return new AzureOpenAiEmbeddingModel(
                api,
                AzureOpenAiEmbeddingOptions.builder()
                        .withDeploymentName("text-embedding-3-large")
                        .build()
        );
    }
}


package com.example.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.FileVectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;

@Configuration
public class VectorStoreConfig {

    @Bean
    public FileVectorStore vectorStore(EmbeddingModel embeddingModel) {
        return new FileVectorStore(
                embeddingModel,
                Path.of("./vector-store")
        );
    }
}
