package est.secretary.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Schedule {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long scheduleId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private Member member;

	@Column(nullable = false)
	private String title;

	private String content;

	@Column(nullable = false)
	private LocalDateTime start;

	private LocalDateTime end;

	private String location;

	@Builder
	public Schedule(Member member, String title, String content, LocalDateTime start, LocalDateTime end,
		String location) {
		this.member = member;
		this.title = title;
		this.content = content;
		this.start = start;
		this.end = end;
		this.location = location;
	}

	public void update(String title, String content,
		LocalDateTime start, LocalDateTime end, String location) {
		this.title = title;
		this.content = content;
		this.start = start;
		this.end = end;
		this.location = location;
	}

}
