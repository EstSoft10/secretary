package est.secretary.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class HolidayService {

	@Value("${holiday.api.key}")
	private String holidayApiKey;

	public List<Map<String, String>> getHolidayEvents(int year, int month) throws IOException {
		StringBuilder urlBuilder = new StringBuilder(
			"http://apis.data.go.kr/B090041/openapi/service/SpcdeInfoService/getRestDeInfo");
		urlBuilder.append("?serviceKey=" + holidayApiKey);
		urlBuilder.append("&solYear=" + year);
		urlBuilder.append("&solMonth=" + String.format("%02d", month));

		URL url = new URL(urlBuilder.toString());
		HttpURLConnection conn = (HttpURLConnection)url.openConnection();
		conn.setRequestMethod("GET");
		conn.setRequestProperty("Content-type", "application/json");

		BufferedReader rd;
		if (conn.getResponseCode() >= 200 && conn.getResponseCode() <= 300) {
			rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		} else {
			rd = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
		}

		StringBuilder sb = new StringBuilder();
		String line;
		while ((line = rd.readLine()) != null) {
			sb.append(line);
		}
		rd.close();
		conn.disconnect();

		List<Map<String, String>> result = new ArrayList<>();
		Pattern datePattern = Pattern.compile("<locdate>(\\d{8})</locdate>");
		Pattern namePattern = Pattern.compile("<dateName>(.*?)</dateName>");

		Matcher dateMatcher = datePattern.matcher(sb.toString());
		Matcher nameMatcher = namePattern.matcher(sb.toString());

		while (dateMatcher.find() && nameMatcher.find()) {
			String date = dateMatcher.group(1);
			String title = nameMatcher.group(1);

			Map<String, String> event = new HashMap<>();
			event.put("title", title);
			event.put("start", LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyyMMdd")).toString());
			event.put("color", "#ffccb6");
			event.put("textColor", "red");
			result.add(event);
		}

		return result;
	}
}
