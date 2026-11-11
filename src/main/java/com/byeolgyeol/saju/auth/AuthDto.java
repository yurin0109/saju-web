package com.byeolgyeol.saju.auth;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AuthDto {

    private AuthDto() {}

    public record SignupRequest(
            @NotBlank(message = "아이디를 입력해주세요.")
            @Pattern(regexp = "[a-z0-9_]{4,20}", message = "아이디는 영어 소문자 · 숫자 · _ 로 4~20자예요.")
            String username,

            @NotBlank(message = "비밀번호를 입력해주세요.")
            @Size(min = 8, max = 64, message = "비밀번호는 8~64자로 정해주세요.")
            @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "비밀번호에 영문과 숫자를 모두 넣어주세요.")
            String password,

            boolean agree
    ) {
        @AssertTrue(message = "이용약관과 개인정보 처리방침에 동의해주세요.")
        public boolean isAgreed() { return agree; }
    }

    public record LoginRequest(
            @NotBlank(message = "아이디를 입력해주세요.") @Size(max = 20, message = "아이디 또는 비밀번호가 맞지 않아요.") String username,
            @NotBlank(message = "비밀번호를 입력해주세요.") @Size(max = 64, message = "아이디 또는 비밀번호가 맞지 않아요.") String password
    ) {}

    /**
     * loggedIn=false 면 username 은 null. moved = 로그인하면서 계정으로 옮겨온 사주 수.
     * recoveryCode = 가입 직후 한 번만 내려주는 복구 코드 (그 외에는 null)
     */
    public record MeResponse(boolean loggedIn, String username, int moved, String recoveryCode) {
        public MeResponse(boolean loggedIn, String username, int moved) { this(loggedIn, username, moved, null); }
    }

    /** 아이디 찾기 */
    public record FindIdRequest(
            @NotBlank(message = "복구 코드를 입력해주세요.") @Size(max = 40, message = "복구 코드가 맞지 않아요.") String recoveryCode
    ) {}

    public record FindIdResponse(String username) {}

    /** 비밀번호 재설정 (아이디 + 복구 코드) */
    public record ResetPasswordRequest(
            @NotBlank(message = "아이디를 입력해주세요.") @Size(max = 20, message = "아이디 또는 복구 코드가 맞지 않아요.") String username,
            @NotBlank(message = "복구 코드를 입력해주세요.") @Size(max = 40, message = "아이디 또는 복구 코드가 맞지 않아요.") String recoveryCode,
            @NotBlank(message = "새 비밀번호를 입력해주세요.")
            @Size(min = 8, max = 64, message = "비밀번호는 8~64자로 정해주세요.")
            @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "비밀번호에 영문과 숫자를 모두 넣어주세요.")
            String newPassword
    ) {}

    /** 비밀번호 변경 (로그인한 상태) */
    public record ChangePasswordRequest(
            @NotBlank(message = "지금 비밀번호를 입력해주세요.") @Size(max = 64) String currentPassword,
            @NotBlank(message = "새 비밀번호를 입력해주세요.")
            @Size(min = 8, max = 64, message = "비밀번호는 8~64자로 정해주세요.")
            @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "비밀번호에 영문과 숫자를 모두 넣어주세요.")
            String newPassword
    ) {}

    /** 복구 코드 다시 받기 (로그인 + 비밀번호 확인) */
    public record PasswordCheckRequest(@NotBlank(message = "비밀번호를 입력해주세요.") @Size(max = 64) String password) {}

    public record RecoveryResponse(String recoveryCode) {}
}
