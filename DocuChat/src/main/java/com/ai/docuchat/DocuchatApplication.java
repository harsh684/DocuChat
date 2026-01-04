package com.ai.docuchat;

import org.springframework.ai.model.huggingface.autoconfigure.HuggingfaceChatAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = { HuggingfaceChatAutoConfiguration.class})
public class DocuchatApplication {

	public static void main(String[] args) {
		SpringApplication.run(DocuchatApplication.class, args);
	}

}
