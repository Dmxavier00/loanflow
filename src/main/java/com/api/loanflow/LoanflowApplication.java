package com.api.loanflow;

import com.api.loanflow.shared.frontend.FrontendBuildBootstrap;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class LoanflowApplication {

	public static void main(String[] args) {
		FrontendBuildBootstrap.prepare();
		SpringApplication.run(LoanflowApplication.class, args);
	}

}
