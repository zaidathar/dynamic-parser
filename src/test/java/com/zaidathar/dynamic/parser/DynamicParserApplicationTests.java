package com.zaidathar.dynamic.parser;

import com.zaidathar.dynamic.parser.config.MappingConfigLoader;
import com.zaidathar.dynamic.parser.config.MappingRule;
import com.zaidathar.dynamic.parser.controller.WebhookController;
import com.zaidathar.dynamic.parser.service.GitHubConfigFetcher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {MappingConfigLoader.class, GitHubConfigFetcher.class, WebhookController.class}, properties = {"logging.level.com.example.servicea=DEBUG"})
class DynamicParserApplicationTests {

	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	private GitHubConfigFetcher gitHubConfigFetcher;

	@Autowired
	private MappingConfigLoader mappingConfigLoader;


	@Autowired
	private WebhookController webhookController;

	@Test
	void contextLoads() {
		assertThat(applicationContext).isNotNull();
	}

	@Test
	void mappingConfigLoaderBeanLoads() {
		assertThat(mappingConfigLoader).isNotNull();
	}

	@Test
	void gitHubConfigFetcherBeanLoads() {
		assertThat(gitHubConfigFetcher).isNotNull();
	}

	@Test
	void webhookControllerBeanLoads() {
		assertThat(webhookController).isNotNull();
	}


	@Test
	@DisplayName("Should load default config via @PostConstruct")
	void testPostConstructLoading() {
		MappingRule rule = mappingConfigLoader.getRule("UserData->UserProfile");
		org.junit.jupiter.api.Assertions.assertNotNull(rule, "@PostConstruct should have loaded rules from mappings.yaml");
	}
}
