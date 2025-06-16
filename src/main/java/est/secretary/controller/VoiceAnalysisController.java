package est.secretary.controller;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import est.secretary.domain.CustomOAuth2User;
import est.secretary.domain.Member;
import est.secretary.domain.Schedule;
import est.secretary.dto.VoiceAnalysisResult;
import est.secretary.dto.VoiceScheduleRequest;
import est.secretary.repository.MemberRepository;
import est.secretary.service.GeminiService;
import est.secretary.service.ScheduleService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/voice")
public class VoiceAnalysisController {

	private final GeminiService geminiService;
	private final ScheduleService scheduleService;
	private final MemberRepository memberRepository;

	@PostMapping("/analyze")
	public ResponseEntity<?> analyzeVoice(
		@AuthenticationPrincipal CustomOAuth2User principal,
		@RequestBody VoiceScheduleRequest request,
		HttpSession session
	) {
		Member member = memberRepository.findByEmail(principal.getEmail())
			.orElseThrow(() -> new RuntimeException("사용자 정보를 찾을 수 없습니다."));

		String userQuery = request.getQuery().toLowerCase();

		String pendingAction = (String)session.getAttribute("pending_action");
		if (pendingAction != null) {

			if ("CONFIRM_DELETE".equals(pendingAction)) {
				boolean isYes =
					userQuery.contains("예") || userQuery.contains("네") || userQuery.contains("응") || userQuery.contains(
						"그래") || userQuery.contains("맞아");

				boolean isNo = userQuery.contains("아니요") || userQuery.contains("아니");

				if (isYes) {
					Long scheduleId = (Long)session.getAttribute("target_schedule_id");
					scheduleService.deleteScheduleById(scheduleId, member);
					clearConversationSession(session);
					return ResponseEntity.ok(Collections.singletonMap("message", "🗑️ 일정을 삭제했어요."));
				} else if (isNo) {
					clearConversationSession(session);
					return ResponseEntity.ok(Collections.singletonMap("message", "알겠습니다. 삭제를 취소했어요."));
				} else {
					return ResponseEntity.ok(Collections.singletonMap("message", "죄송합니다. 예 또는 아니요로 다시 말씀해주세요."));
				}
			} else if ("AWAITING_UPDATE_VALUE".equals(pendingAction)) {
				Long scheduleId = (Long)session.getAttribute("target_schedule_id");
				String targetField = (String)session.getAttribute("target_field");
				Schedule scheduleToUpdate = scheduleService.findScheduleById(scheduleId, member);

				VoiceAnalysisResult updates = geminiService.analyze(request.getQuery());
				boolean updated = false;

				if ("start".equals(targetField) && updates.getStart() != null) {
					scheduleToUpdate.update(null, LocalDateTime.parse(updates.getStart()), null);
					updated = true;
				} else if ("title".equals(targetField) && updates.getTitle() != null) {
					scheduleToUpdate.update(updates.getTitle(), null, null);
					updated = true;
				} else if ("location".equals(targetField) && updates.getLocation() != null) {
					scheduleToUpdate.update(null, null, updates.getLocation());
					updated = true;
				}

				if (updated) {
					scheduleService.save(scheduleToUpdate);
					clearConversationSession(session);
					return ResponseEntity.ok(Collections.singletonMap("message", "✅ 일정을 성공적으로 수정했어요."));
				} else {
					String question = (String)session.getAttribute("question");
					return ResponseEntity.ok(Collections.singletonMap("message", "값을 이해하지 못했어요. " + question));
				}
			} else if ("AWAITING_UPDATE_FIELD".equals(pendingAction)) {
				VoiceAnalysisResult updates = geminiService.analyze(request.getQuery());
				boolean hasNewValues =
					updates.getTitle() != null || updates.getStart() != null || updates.getLocation() != null;

				if (hasNewValues) {
					Long scheduleId = (Long)session.getAttribute("target_schedule_id");
					Schedule scheduleToUpdate = scheduleService.findScheduleById(scheduleId, member);
					LocalDateTime newStart =
						(updates.getStart() != null) ? LocalDateTime.parse(updates.getStart()) : null;
					scheduleToUpdate.update(updates.getTitle(), newStart, updates.getLocation());
					scheduleService.save(scheduleToUpdate);
					clearConversationSession(session);
					return ResponseEntity.ok(Collections.singletonMap("message", "✅ 일정을 성공적으로 수정했어요."));
				} else {
					String question = null;
					String targetField = null;

					if (userQuery.contains("시간") || userQuery.contains("언제")) {
						question = "언제로 변경할까요?";
						targetField = "start";
					} else if (userQuery.contains("장소") || userQuery.contains("어디")) {
						question = "어디로 변경할까요?";
						targetField = "location";
					} else if (userQuery.contains("제목") || userQuery.contains("이름")) {
						question = "무엇으로 변경할까요?";
						targetField = "title";
					}

					if (question != null) {
						session.setAttribute("pending_action", "AWAITING_UPDATE_VALUE");
						session.setAttribute("target_field", targetField);
						session.setAttribute("question", question);
						return ResponseEntity.ok(Collections.singletonMap("message", question));
					} else {
						return ResponseEntity.ok(
							Collections.singletonMap("message", "이해하지 못했어요. 제목, 시간, 장소 중 무엇을 바꿀지 말씀해주세요."));
					}
				}
			}
		}

		VoiceAnalysisResult previous = (VoiceAnalysisResult)session.getAttribute("voice-progress");
		VoiceAnalysisResult current = geminiService.analyze(request.getQuery());
		VoiceAnalysisResult merged = mergeAnalysisResults(previous, current);

		String message;
		switch (merged.getIntent()) {
			case "create" -> {
				boolean missingTitle = merged.getTitle() == null;
				boolean missingTime = isStartMissingOrIncomplete(merged.getStart());
				boolean missingLocation = merged.getLocation() == null;

				if (merged.getStart() != null && merged.getStart().matches("^\\d{4}-\\d{2}-\\d{2}$")) {
					session.setAttribute("voice-progress", merged);
					return ResponseEntity.ok(Collections.singletonMap("message", "📆 날짜는 확인했어요! 몇 시에 진행할까요?"));
				}
				if (merged.getStart() != null && merged.getStart().matches("^T?\\d{2}:\\d{2}(:\\d{2})?$")) {
					session.setAttribute("voice-progress", merged);
					return ResponseEntity.ok(Collections.singletonMap("message", "⏰ 시간은 확인했어요! 날짜도 알려주세요 🙏"));
				}
				if (missingTitle || missingTime || missingLocation) {
					StringBuilder sb = new StringBuilder("📌 일정 등록을 위해 ");
					if (missingTitle)
						sb.append("제목을, ");
					if (missingTime)
						sb.append("시간을, ");
					if (missingLocation)
						sb.append("장소를, ");
					sb.setLength(sb.length() - 2);
					sb.append(" 알려주세요 🙏");
					session.setAttribute("voice-progress", merged);
					return ResponseEntity.ok(Collections.singletonMap("message", sb.toString()));
				}

				scheduleService.saveScheduleByParsedResult(merged, member);
				clearConversationSession(session);
				message =
					"✅ 일정 등록 완료!\n" + "제목: " + merged.getTitle() + "\n" + "시간: " + merged.getStart() + "\n" + "장소: "
						+ merged.getLocation();
			}

			case "delete" -> {
				boolean isDateOnly = merged.getStart() != null && merged.getStart().matches("^\\d{4}-\\d{2}-\\d{2}$");
				if (isDateOnly) {
					List<Schedule> schedules = scheduleService.findSchedulesByDateAndOptionalTitle(merged.getStart(),
						merged.getTitle(), member);
					if (schedules.isEmpty()) {
						message = "❌ 해당 날짜에 일치하는 일정이 없어요.";
						clearConversationSession(session);
					} else if (schedules.size() == 1) {
						Schedule scheduleToDelete = schedules.get(0);
						session.setAttribute("pending_action", "CONFIRM_DELETE");
						session.setAttribute("target_schedule_id", scheduleToDelete.getScheduleId());
						message = String.format("🗓️ '%s' 일정이 있습니다. 이 일정을 삭제할까요? (예/아니요)", scheduleToDelete.getTitle());
					} else {
						StringBuilder sb = new StringBuilder("어떤 일정을 삭제할까요? 유사한 일정이 여러 개 있어요.\n");
						schedules.forEach(
							s -> sb.append(String.format("- %s (%s)\n", s.getTitle(), s.getStart().toLocalTime())));
						message = sb.toString();
						session.setAttribute("voice-progress", merged);
					}
					return ResponseEntity.ok(Collections.singletonMap("message", message));
				}

				boolean titleMissing = merged.getTitle() == null;
				boolean timeMissing = isStartMissingOrIncomplete(merged.getStart());
				if (titleMissing || timeMissing) {
					StringBuilder sb = new StringBuilder("📌 삭제할 일정을 찾기 위해 ");
					if (titleMissing)
						sb.append("제목을, ");
					if (timeMissing)
						sb.append("시간을, ");
					sb.setLength(sb.length() - 2);
					sb.append(" 알려주세요 🙏");
					session.setAttribute("voice-progress", merged);
					return ResponseEntity.ok(Collections.singletonMap("message", sb.toString()));
				}

				List<String> matches = scheduleService.findMatchingSchedules(merged, member);
				if (matches.isEmpty()) {
					message = "❌ 일치하는 일정이 없어요. 제목과 시간을 다시 확인해주세요.";
					clearConversationSession(session);
				} else if (matches.size() == 1) {
					scheduleService.deleteScheduleByParsedResult(merged, member);
					clearConversationSession(session);
					message = "🗑️ 다음 일정을 삭제했어요:\n" + matches.get(0);
				} else {
					message = "❗ 유사한 일정이 여러 개 있어요:\n";
					for (String m : matches)
						message += "- " + m + "\n";
					message += "더 구체적으로 말씀해 주세요!";
					session.setAttribute("voice-progress", merged);
				}
			}

			case "update" -> {
				List<Schedule> schedules = scheduleService.findSchedulesByDateAndOptionalTitle(merged.getStart(),
					merged.getTitle(), member);
				if (schedules.isEmpty()) {
					message = "❌ 수정할 일정을 찾지 못했어요. 날짜와 제목을 다시 확인해주세요.";
					clearConversationSession(session);
				} else if (schedules.size() > 1) {
					message = "❗ 수정할 일정이 여러 개 있어요. 더 구체적으로 말씀해 주세요.";
					session.setAttribute("voice-progress", merged);
				} else {
					Schedule scheduleToUpdate = schedules.get(0);
					session.setAttribute("pending_action", "AWAITING_UPDATE_FIELD");
					session.setAttribute("target_schedule_id", scheduleToUpdate.getScheduleId());
					message = String.format("🗓️ '%s' 일정을 찾았어요. 무엇을 변경할까요?", scheduleToUpdate.getTitle());
				}
			}

			default -> {
				message = "❓ 무슨 작업을 원하시는지 명확하지 않아요.";
				clearConversationSession(session);
			}
		}

		return ResponseEntity.ok(Collections.singletonMap("message", message));
	}

	private void clearConversationSession(HttpSession session) {
		session.removeAttribute("pending_action");
		session.removeAttribute("target_schedule_id");
		session.removeAttribute("target_field");
		session.removeAttribute("question");
		session.removeAttribute("voice-progress");
	}

	private VoiceAnalysisResult mergeAnalysisResults(VoiceAnalysisResult previous, VoiceAnalysisResult current) {
		VoiceAnalysisResult merged = new VoiceAnalysisResult();
		String finalIntent;
		if (previous != null && previous.getIntent() != null) {
			finalIntent = previous.getIntent();
		} else if (current.getIntent() != null && !current.getIntent().equals("read")) {
			finalIntent = current.getIntent();
		} else {
			finalIntent = "create";
		}
		merged.setIntent(finalIntent);
		merged.setTitle(
			current.getTitle() != null ? current.getTitle() : (previous != null ? previous.getTitle() : null));
		merged.setLocation(
			current.getLocation() != null ? current.getLocation() : (previous != null ? previous.getLocation() : null));
		String previousStart = (previous != null) ? previous.getStart() : null;
		String currentStart = current.getStart();
		String finalStart = null;
		if (currentStart != null) {
			if (currentStart.matches("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}(:\\d{2})?$")) {
				finalStart = currentStart;
			} else if (currentStart.matches("^T?\\d{2}:\\d{2}(:\\d{2})?$") && previousStart != null
				&& previousStart.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
				finalStart = previousStart + "T" + currentStart.replace("T", "");
			} else {
				finalStart = currentStart;
			}
		} else {
			finalStart = previousStart;
		}
		merged.setStart(finalStart);
		return merged;
	}

	private boolean isStartMissingOrIncomplete(String start) {
		if (start == null || start.isBlank())
			return true;
		if (start.matches("^\\d{4}-\\d{2}-\\d{2}$"))
			return true;
		if (start.matches("^T\\d{2}:\\d{2}(:\\d{2})?$"))
			return true;
		return !start.matches("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}(:\\d{2})?$");
	}
}
