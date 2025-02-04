package com.gyunpang.gateway.utils;

import java.nio.file.AccessDeniedException;
import java.security.Key;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.gyunpang.gateway.dto.AuthDto;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class JwtUtil {
	private final Key key;

	public JwtUtil(@Value("${jwt.secret}") String JWT_SECRET) {
		byte[] decode = Decoders.BASE64.decode(JWT_SECRET);
		this.key = Keys.hmacShaKeyFor(decode);
	}

	public AuthDto.SignInRes generateToken(String username, Integer authority) {
		String accessToken = createToken(username, authority,
			Date.from(
				LocalDateTime.now().plusMinutes(30).toInstant(ZoneOffset.ofHours(9))
			));
		String refreshToken = createToken(username, authority,
			Date.from(
				LocalDateTime.now().plusDays(10)
					.toInstant(ZoneOffset.ofHours(9))
			));
		return AuthDto.SignInRes.builder()
			.accessToken(accessToken)
			.refreshToken(refreshToken)
			.build();
	}

	private String createToken(String subject, Integer authority, Date expireDate) {
		return Jwts.builder()
			.subject(subject)
			.issuedAt(Date.from(LocalDateTime.now().toInstant(ZoneOffset.ofHours(9))))
			.expiration(expireDate)
			.signWith(key)
			.claim(GatewayConstant.HEADER_USERNAME, subject)
			.claim(GatewayConstant.HEADER_AUTHORITY, String.valueOf(authority))
			.compact();
	}

	public Jws<Claims> getClaims(String token) {
		return Jwts.parser().verifyWith((SecretKey)key).build().parseSignedClaims(token);
	}

	public boolean validateToken(String token) {
		try {
			getClaims(token);
			return true;
		} catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
			log.warn("잘못된 JWT 서명입니다.");
		} catch (ExpiredJwtException e) {
			log.warn("만료된 JWT 토큰입니다.");
		} catch (UnsupportedJwtException e) {
			log.warn("지원되지 않는 JWT 토큰입니다.");
		} catch (IllegalArgumentException e) {
			log.warn("JWT 토큰이 잘못되었습니다.");
		}
		return false;
	}

	public AuthDto.SignInRes tryRefreshToken(String token) {
		String failReason = "";
		try {
			Jws<Claims> claims = getClaims(token);
			String subject = claims.getPayload().getSubject();
			if (!claims.getPayload().containsKey(GatewayConstant.HEADER_USERNAME)) {
				throw new AccessDeniedException("Fail to check");
			}
			return generateToken(subject,
				Integer.parseInt((String)claims.getPayload().get(GatewayConstant.HEADER_USERNAME)));
		} catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
			failReason = "잘못된 JWT 서명입니다.";
		} catch (ExpiredJwtException e) {
			failReason = "만료된 JWT 토큰입니다.";
		} catch (UnsupportedJwtException e) {
			failReason = "지원되지 않는 JWT 토큰입니다.";
		} catch (IllegalArgumentException e) {
			failReason = "JWT 토큰이 잘못되었습니다.";
		} catch (AccessDeniedException e) {
			failReason = "유저 권한이 확인되지 않습니다.";
		}
		return AuthDto.SignInRes.builder().failReason(failReason).build();
	}
}
