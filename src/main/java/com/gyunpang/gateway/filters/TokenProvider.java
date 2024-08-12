package com.gyunpang.gateway.filters;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.gyunpang.gateway.utils.CommonCode;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class TokenProvider {
	private final SecretKey key; // secret Key

	public TokenProvider(@Value("${jwt.secret}") String secretKey
	) {
		byte[] secretByteKey = Decoders.BASE64.decode(secretKey);
		this.key = Keys.hmacShaKeyFor(secretByteKey);
	}

	public String validateTokenAndGetUsername(String token) {
		Jws<Claims> claimsJws = Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
		return claimsJws.getPayload().containsKey(CommonCode.HEADER_USERNAME.getContext()) ?
			(String)claimsJws.getPayload().get(CommonCode.HEADER_USERNAME.getContext()) : "";
	}

}