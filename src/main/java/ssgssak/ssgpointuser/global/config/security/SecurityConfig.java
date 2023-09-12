package ssgssak.ssgpointuser.global.config.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import ssgssak.ssgpointuser.global.config.jwt.JwtAuthenticationFilter;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final AuthenticationProvider authenticationProvider;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ExceptionHandlerFilter exceptionHandlerFilter;

    /**
     * Security Config
     *   기존의 WebSecurityConfigurerAdapter를 extends받는 방식에서
     *   SecurityFilterChain을 Bean등록해서 사용하는 방식으로 바뀜
     */
    //todo : Redis로 jwt 관리하기
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Restful API를 사용하므로, csrf는 사용할 필요가 없다
                .csrf(csrf -> csrf.disable())
                // cors는 사용할것이므로 CorsConfig만 @Configuration 등록하고 따로 설정해주지는 않음
                // 토큰 방식을 사용하므로, 서버에서 session을 관리하지 않음. 따라서 STATELESS로 설정
                .sessionManagement(sessionManagement -> sessionManagement
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 인증 절차에 대한 설정을 시작 : 필터를 적용시키지 않을 url과, 적용시킬 url을 구분
                .authorizeHttpRequests(authorizeHttpRequest -> authorizeHttpRequest
                        .requestMatchers(org.springframework.web.cors.CorsUtils::isPreFlightRequest)
                        .permitAll()
                        // RESTful 하게 구분되는 경우 -> HttpMethod 까지 적어줘야한다
                        .requestMatchers(HttpMethod.GET, "/api/v1/franchise").permitAll() // 모든 가맹점 정보 조회
                        // url이 특정지어지는 경우 -> HttpMethod를 적어줄 필요가 없다
                        .requestMatchers(
                                "/api/v1/store/map",
                                "/api/v1/store/region",
                                "/api/v1/pointcard/offline/temp",   // 임시 카드 발급
                                "/api/v1/auth/login-id",            // 로그인 아이디 찾기
                                "/api/v1/auth/sign-up",             // 회원가입
                                "/api/v1/auth/log-in",              // 로그인
                                "/swagger-ui/**",                   // 스웨거
                                "/swagger-resources/**",            // 스웨거
                                "/api-docs/**")                     // 스웨거
                        .permitAll() // 위의 url은 모두 filter를 거치지 않음
                        .anyRequest().authenticated()) // 위의 url을 제외한 모든 url은 필터를 거쳐야함
                // 폼 로그인 사용 안함
                .formLogin(formLogin -> formLogin.disable())
                // authenticationProvider 설정 : 입력된 정보와 db의 정보를 비교하여, 인증에 성공하면 Authentication 객체를 생성하여 리턴해줌
                .authenticationProvider(authenticationProvider)
                // 토큰 유효성을 검사하고 토큰을 생성하는 필터를 추가. 필터 추가 순서를 꼭 지켜야한다.
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                // filter단에서 발생하는 에러를 처리할 ExceptionHandlerFilter를 OAuth2필터 앞에 추가한다
                .addFilterBefore(exceptionHandlerFilter, OAuth2AuthorizationRequestRedirectFilter.class);
        return http.build();
    }
}
