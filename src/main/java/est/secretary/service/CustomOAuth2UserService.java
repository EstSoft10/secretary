package est.secretary.service;

import est.secretary.domain.Member;
import est.secretary.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final MemberRepository memberRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = new DefaultOAuth2UserService().loadUser(request);
        String provider = request.getClientRegistration().getRegistrationId();
        Map<String, Object> attributes = oauth2User.getAttributes();

        String providerId = null;
        String name = null;
        String email = null;
        String profileImage = null;

        switch (provider) {
            case "google" -> {
                providerId = attributes.get("sub").toString();
                name = attributes.get("name").toString();
                email = attributes.get("email").toString();
                profileImage = attributes.get("picture").toString();
            }
            case "kakao" -> {
                Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
                Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
                providerId = attributes.get("id").toString();
                name = profile.get("nickname").toString();
                email = kakaoAccount.get("email").toString();
                profileImage = profile.get("profile_image_url").toString();
            }
            case "naver" -> {
                Map<String, Object> response = (Map<String, Object>) attributes.get("response");
                providerId = response.get("id").toString();
                name = response.get("name").toString();
                email = response.get("email").toString();
                profileImage = response.get("profile_image").toString();
            }
            default -> throw new OAuth2AuthenticationException("지원하지 않는 provider: " + provider);
        }


        final String finalProvider = provider;
        final String finalProviderId = providerId;
        final String finalName = name;
        final String finalEmail = email;
        final String finalProfileImage = profileImage;

        Member member = memberRepository.findByProviderAndProviderId(finalProvider, finalProviderId)
                .orElseGet(() -> memberRepository.save(Member.builder()
                        .provider(finalProvider)
                        .providerId(finalProviderId)
                        .name(finalName)
                        .email(finalEmail)
                        .profileImage(finalProfileImage)
                        .build()));

        return new CustomOAuth2User(member, attributes);
    }
}