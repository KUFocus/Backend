package org.focus.logmeet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class LogmeetApplication {

	public static void main(String[] args) {
		SpringApplication.run(LogmeetApplication.class, args);
	}

}
