package az.cyberlaw.config;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class AiConfig {

	/** Short-term conversation memory (kept in RAM, cleared on restart). */
	@Bean
	ChatMemory chatMemory(CyberLawProperties props) {
		return MessageWindowChatMemory.builder()
			.chatMemoryRepository(new InMemoryChatMemoryRepository())
			.maxMessages(props.rag().historyMessages())
			.build();
	}

	/** Documents are indexed one at a time to stay inside the free Gemini rate limits. */
	@Bean
	TaskExecutor ingestionExecutor() {
		return executor("ingest-", 1);
	}

	@Bean
	TaskExecutor complianceExecutor() {
		return executor("compliance-", 2);
	}

	private static ThreadPoolTaskExecutor executor(String prefix, int threads) {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setThreadNamePrefix(prefix);
		executor.setCorePoolSize(threads);
		executor.setMaxPoolSize(threads);
		executor.setQueueCapacity(500);
		executor.initialize();
		return executor;
	}

}
