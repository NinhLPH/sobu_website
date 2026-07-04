package com.vn.sodu.utilites;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "encryption")
public class EncryptionProperties {

    private String secretKey;
}
