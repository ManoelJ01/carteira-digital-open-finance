package com.manoel.carteiradigital.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Tratamento global de exceções no padrão RFC 7807 (Problem Details for HTTP APIs).
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String TYPE_BASE = "https://carteira-digital.dev/erros/";

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ProblemDetail> handleNaoEncontrado(RecursoNaoEncontradoException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "recurso-nao-encontrado", "Recurso não encontrado", ex.getMessage(), request);
    }

    @ExceptionHandler(RegraDeNegocioException.class)
    public ResponseEntity<ProblemDetail> handleRegraNegocio(RegraDeNegocioException ex, HttpServletRequest request) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, "regra-de-negocio", "Violação de regra de negócio", ex.getMessage(), request);
    }

    @ExceptionHandler(PluggyIntegracaoException.class)
    public ResponseEntity<ProblemDetail> handlePluggy(PluggyIntegracaoException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_GATEWAY, "integracao-pluggy", "Falha na integração com a Pluggy", ex.getMessage(), request);
    }

    @ExceptionHandler(CredenciaisInvalidasException.class)
    public ResponseEntity<ProblemDetail> handleCredenciais(CredenciaisInvalidasException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "credenciais-invalidas", "Credenciais inválidas", ex.getMessage(), request);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ProblemDetail> handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "credenciais-invalidas", "Credenciais inválidas", "E-mail ou senha inválidos", request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ProblemDetail> handleAuth(AuthenticationException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "nao-autenticado", "Não autenticado", ex.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGeneric(Exception ex, HttpServletRequest request) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "erro-interno", "Erro interno do servidor",
                "Ocorreu um erro inesperado. Tente novamente mais tarde.", request);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                    HttpHeaders headers,
                                                                    HttpStatusCode status,
                                                                    WebRequest request) {
        Map<String, String> erros = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            erros.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Erro de validação nos campos enviados");
        problemDetail.setTitle("Dados inválidos");
        problemDetail.setType(URI.create(TYPE_BASE + "dados-invalidos"));
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("erros", erros);

        return ResponseEntity.badRequest().body(problemDetail);
    }

    private ResponseEntity<ProblemDetail> build(HttpStatus status, String typeSlug, String title, String detail, HttpServletRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setTitle(title);
        problemDetail.setType(URI.create(TYPE_BASE + typeSlug));
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        problemDetail.setProperty("timestamp", Instant.now());
        return ResponseEntity.status(status).body(problemDetail);
    }
}
