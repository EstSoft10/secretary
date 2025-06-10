package est.secretary.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String provider;       // ex) google, kakao, naver
    private String providerId;     // 소셜 서비스 내 사용자 고유 ID
    private String name;
    private String email;
    private String profileImage;
}