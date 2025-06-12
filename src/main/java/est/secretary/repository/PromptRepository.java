package est.secretary.repository;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import est.secretary.dto.PromptItem;

@Repository
public class PromptRepository {

	public List<PromptItem> findAllPrompts() {
		try {
			ObjectMapper mapper = new ObjectMapper();
			InputStream inputStream = new ClassPathResource("static/json/prompts.json").getInputStream();
			return mapper.readValue(inputStream, new TypeReference<>() {
			});
		} catch (IOException e) {
			throw new RuntimeException("프롬프트 로딩 실패", e);
		}
	}
}
