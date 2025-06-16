package est.secretary.dto;

import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
public class AiActionRequest {
	private String actionQuery;
	private List<Map<String, String>> history;
}
