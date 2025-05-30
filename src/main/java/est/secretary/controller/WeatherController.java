package est.secretary.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
public class WeatherController {

	@Value("${weather.api.key}")
	private String apiKey;

	private final RestTemplate restTemplate = new RestTemplate();

	@GetMapping("/weather")
	@ResponseBody
	public Map<String, Object> getWeather(@RequestParam double lat, @RequestParam double lon) {
		String url = String.format(
			"https://api.openweathermap.org/data/2.5/weather?lat=%f&lon=%f&appid=%s&units=metric&lang=kr",
			lat, lon, apiKey);
		ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
		Map body = response.getBody();
		String locationUrl = String.format(
			"https://nominatim.openstreetmap.org/reverse?format=json&lat=%f&lon=%f&accept-language=ko", lat, lon);
		Map locationData = restTemplate.getForObject(locationUrl, Map.class);
		Map address = (Map)locationData.get("address");

		Map<String, Object> result = new HashMap<>();
		Map weather = ((List<Map>)body.get("weather")).get(0);
		Map main = (Map)body.get("main");

		String city = (String)address.get("city");
		String borough = (String)address.get("borough");
		String suburb = (String)address.get("suburb");

		result.put("description", weather.get("description"));
		result.put("icon", weather.get("icon"));
		result.put("temp", main.get("temp"));
		result.put("location", String.join(" ", city, borough, suburb));

		return result;
	}

}
