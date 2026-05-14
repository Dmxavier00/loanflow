package com.api.loanflow;

import com.api.loanflow.compartilhado.apresentacao.FrontendBuildBootstrap;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LoanflowApplication {

	public static void main(String[] args) {
		FrontendBuildBootstrap.prepare();
		SpringApplication.run(LoanflowApplication.class, args);
	}

}
