package ssgssak.ssgpointuser.domain.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthGetLoginIdRequestDto {
    private String userName;
    private String phoneNumber;
}
