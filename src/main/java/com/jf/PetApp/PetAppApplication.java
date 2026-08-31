package com.jf.PetApp;

import com.jf.PetApp.infrastructure.config.DotenvLoader;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PetAppApplication {

	public static void main(String[] args) {
		DotenvLoader.loadIntoSystemProperties();
		SpringApplication.run(PetAppApplication.class, args);
	}

}
