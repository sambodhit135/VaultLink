package com.vaultlink.vaultlink;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.vaultlink")
@EnableScheduling
public class VaultlinkApplication {

	public static void main(String[] args) {
		SpringApplication.run(VaultlinkApplication.class, args);
	}

}
