package com.gyunpang.gateway.utils;

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

	public AuthDto.SignInRes generateToken(String username) {
		String accessToken = createToken(username,
			Date.from(
				LocalDateTime.now().plusMinutes(30).toInstant(ZoneOffset.ofHours(9))
			));
		String refreshToken = createToken(username,
			Date.from(
				LocalDateTime.now().plusDays(10)
					.toInstant(ZoneOffset.ofHours(9))
			));
		return AuthDto.SignInRes.builder()
			.accessToken(accessToken)
			.refreshToken(refreshToken)
			.build();
	}

	private String createToken(String subject, Date expireDate) {
		return Jwts.builder()
			.subject(subject)
			.issuedAt(Date.from(LocalDateTime.now().toInstant(ZoneOffset.ofHours(9))))
			.expiration(expireDate)
			.signWith(key)
			.compact();
	}

	private Jws<Claims> getClaims(String token) {
		return Jwts.parser().verifyWith((SecretKey)key).build().parseSignedClaims(token);
	}

	public AuthDto.SignInRes validateToken(String token) {
		String failReason = "";
		try {
			Jws<Claims> claims = getClaims(token);
			String subject = claims.getPayload().getSubject();
			return generateToken(subject);
		} catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
			failReason = "잘못된 JWT 서명입니다.";
		} catch (ExpiredJwtException e) {
			failReason = "만료된 JWT 토큰입니다.";
		} catch (UnsupportedJwtException e) {
			failReason = "지원되지 않는 JWT 토큰입니다.";
		} catch (IllegalArgumentException e) {
			failReason = "JWT 토큰이 잘못되었습니다.";
		}
		return AuthDto.SignInRes.builder().failReason(failReason).build();
	}
}
