package com.miapay.miapaylite.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.miapay.miapaylite.auth.EmailAlreadyUsedException;

// Extending ResponseEntityExceptionHandler turns validation errors (400) into RFC 9457 ProblemDetail bodies.
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

	@ExceptionHandler(EmailAlreadyUsedException.class)
	ProblemDetail handleEmailAlreadyUsed(EmailAlreadyUsedException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Email already used");
	}

}
