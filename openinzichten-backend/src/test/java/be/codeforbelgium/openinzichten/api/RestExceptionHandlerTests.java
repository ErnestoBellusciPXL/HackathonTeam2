package be.codeforbelgium.openinzichten.api;

import be.codeforbelgium.openinzichten.exceptions.*;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RestExceptionHandlerTests {

    private final RestExceptionHandler handler = new RestExceptionHandler();

    @Test
    void usernameTaken_returns_conflict() {
        UsernameTakenException ex = new UsernameTakenException("already");
        ResponseEntity<Map<String, String>> resp = handler.handleUsernameTaken(ex);
        assertEquals(409, resp.getStatusCode().value());
        assertEquals("username_taken", resp.getBody().get("error"));
    }

    @Test
    void usernameTooShort_and_tooLong_return_bad_request() {
        UsernameTooShortException s = new UsernameTooShortException("too short");
        ResponseEntity<Map<String, String>> r1 = handler.handleUsernameTooShort(s);
        assertEquals(400, r1.getStatusCode().value());
        assertEquals("username_too_short", r1.getBody().get("error"));

        UsernameTooLongException l = new UsernameTooLongException("too long");
        ResponseEntity<Map<String, String>> r2 = handler.handleUsernameTooLong(l);
        assertEquals(400, r2.getStatusCode().value());
        assertEquals("username_too_long", r2.getBody().get("error"));
    }

    @Test
    void emailTaken_and_invalidEmail_accountNotFound_invalidPassword() {
        EmailTakenException e = new EmailTakenException("taken");
        ResponseEntity<Map<String, String>> re = handler.handleEmailTaken(e);
        assertEquals(409, re.getStatusCode().value());
        assertEquals("email_taken", re.getBody().get("error"));

        InvalidEmailException ie = new InvalidEmailException("bad email");
        ResponseEntity<Map<String, String>> rie = handler.handleInvalidEmail(ie);
        assertEquals(400, rie.getStatusCode().value());
        assertEquals("invalid_email", rie.getBody().get("error"));

        AccountNotFoundException an = new AccountNotFoundException("not found");
        ResponseEntity<Map<String, String>> ran = handler.handleAccountNotFound(an);
        assertEquals(404, ran.getStatusCode().value());
        assertEquals("account_not_found", ran.getBody().get("error"));

        InvalidCredentialsException ip = new InvalidCredentialsException("bad pw");
        ResponseEntity<Map<String, String>> rip = handler.handleInvalidPassword(ip);
        assertEquals(400, rip.getStatusCode().value());
        assertEquals("invalid_password", rip.getBody().get("error"));
    }

    @Test
    void handleValidationErrors_username_email_password_and_fallback() {
        // username message that contains the 'between 3 and 50' phrase
        BeanPropertyBindingResult br = new BeanPropertyBindingResult(new Object(), "obj");
        FieldError fe = new FieldError("obj", "username", "must be between 3 and 50");
        br.addError(fe);

        MethodArgumentNotValidException manv = mock(MethodArgumentNotValidException.class);
        when(manv.getBindingResult()).thenReturn(br);

        ResponseEntity<Map<String, String>> r = handler.handleValidationErrors(manv);
        assertEquals(400, r.getStatusCode().value());
        assertEquals("username_invalid", r.getBody().get("error"));

        // email field
        BeanPropertyBindingResult br2 = new BeanPropertyBindingResult(new Object(), "obj2");
        FieldError fe2 = new FieldError("obj2", "email", "not an email");
        br2.addError(fe2);
        MethodArgumentNotValidException manv2 = mock(MethodArgumentNotValidException.class);
        when(manv2.getBindingResult()).thenReturn(br2);
        ResponseEntity<Map<String, String>> r2 = handler.handleValidationErrors(manv2);
        assertEquals(400, r2.getStatusCode().value());
        assertEquals("invalid_email", r2.getBody().get("error"));

        // password field
        BeanPropertyBindingResult br3 = new BeanPropertyBindingResult(new Object(), "obj3");
        FieldError fe3 = new FieldError("obj3", "password", "bad pw");
        br3.addError(fe3);
        MethodArgumentNotValidException manv3 = mock(MethodArgumentNotValidException.class);
        when(manv3.getBindingResult()).thenReturn(br3);
        ResponseEntity<Map<String, String>> r3 = handler.handleValidationErrors(manv3);
        assertEquals(400, r3.getStatusCode().value());
        assertEquals("invalid_password", r3.getBody().get("error"));

        // fallback when messages are null
        BeanPropertyBindingResult br4 = new BeanPropertyBindingResult(new Object(), "obj4");
        FieldError fe4 = new FieldError("obj4", "other", null);
        br4.addError(fe4);
        MethodArgumentNotValidException manv4 = mock(MethodArgumentNotValidException.class);
        when(manv4.getBindingResult()).thenReturn(br4);
        ResponseEntity<Map<String, String>> r4 = handler.handleValidationErrors(manv4);
        assertEquals(400, r4.getStatusCode().value());
        assertEquals("validation_error", r4.getBody().get("error"));
        assertEquals("validation failed", r4.getBody().get("message"));
    }

    @Test
    void handleConstraintViolation_joins_messages() {
        ConstraintViolation<?> v1 = mock(ConstraintViolation.class);
        when(v1.getMessage()).thenReturn("a");
        ConstraintViolation<?> v2 = mock(ConstraintViolation.class);
        when(v2.getMessage()).thenReturn("b");

        ConstraintViolationException cve = new ConstraintViolationException(Set.of(v1, v2));
        ResponseEntity<Map<String, String>> r = handler.handleConstraintViolation(cve);
        assertEquals(400, r.getStatusCode().value());
        assertEquals("validation_error", r.getBody().get("error"));
        assertTrue(r.getBody().get("message").contains("a"));
        assertTrue(r.getBody().get("message").contains("b"));
    }

    @Test
    void handleBindException_concatenates_field_messages() {
        BindException be = new BindException(new Object(), "obj");
        be.addError(new FieldError("obj", "field1", "err1"));
        be.addError(new FieldError("obj", "field2", "err2"));

        ResponseEntity<Map<String, String>> r = handler.handleBindException(be);
        assertEquals(400, r.getStatusCode().value());
        assertEquals("validation_error", r.getBody().get("error"));
        String msg = r.getBody().get("message");
        assertTrue(msg.contains("err1"));
        assertTrue(msg.contains("err2"));
    }

    @Test
    void illegalArgument_handler_handles_null_and_message() {
        ResponseEntity<Map<String, String>> r1 = handler.handleIllegalArgument(new IllegalArgumentException((String) null));
        assertEquals(400, r1.getStatusCode().value());
        assertEquals("invalid_argument", r1.getBody().get("error"));
        assertEquals("invalid argument", r1.getBody().get("message"));

        ResponseEntity<Map<String, String>> r2 = handler.handleIllegalArgument(new IllegalArgumentException("bad arg"));
        assertEquals("bad arg", r2.getBody().get("message"));
    }

    @Test
    void username_field_with_null_default_message_returns_invalid_username_default() {
        BeanPropertyBindingResult br = new BeanPropertyBindingResult(new Object(), "obj");
        FieldError fe = new FieldError("obj", "username", null);
        br.addError(fe);
        MethodArgumentNotValidException manv = mock(MethodArgumentNotValidException.class);
        when(manv.getBindingResult()).thenReturn(br);

        ResponseEntity<Map<String, String>> r = handler.handleValidationErrors(manv);
        assertEquals(400, r.getStatusCode().value());
        assertEquals("username_invalid", r.getBody().get("error"));
        assertEquals("invalid username", r.getBody().get("message"));
    }

    @Test
    void story_and_ticket_related_handlers_return_expected_errors() {
        ResponseEntity<Map<String, String>> s = handler.handleStoryNotFound(new StoryNotFoundException("no story"));
        assertEquals(404, s.getStatusCode().value());
        assertEquals("story_not_found", s.getBody().get("error"));

        ResponseEntity<Map<String, String>> t = handler.handleTicketNotFound(new TicketNotFoundException("no ticket"));
        assertEquals(404, t.getStatusCode().value());
        assertEquals("ticket_not_found", t.getBody().get("error"));

        ResponseEntity<Map<String, String>> ta = handler.handleTicketAlreadyExists(new TicketAlreadyExistsException("exists"));
        assertEquals(409, ta.getStatusCode().value());
        assertEquals("ticket_already_exists", ta.getBody().get("error"));

        ResponseEntity<Map<String, String>> sal = handler.handleStoryAlreadyLiked(new StoryAlreadyLikedException("liked"));
        assertEquals(409, sal.getStatusCode().value());
        assertEquals("story_already_liked", sal.getBody().get("error"));

        ResponseEntity<Map<String, String>> snl = handler.handleStoryNotLiked(new StoryNotLikedException("not liked"));
        assertEquals(400, snl.getStatusCode().value());
        assertEquals("story_not_liked", snl.getBody().get("error"));
    }

    @Test
    void chat_not_found_and_access_denied() {
        ResponseEntity<Map<String, String>> r1 = handler.handleChatNotFound(new ChatNotFoundException(UUID.randomUUID()));
        ResponseEntity<Map<String, String>> r2 = handler.handleChatAccessDenied(new ChatAccessDeniedException());
        assertEquals("chat_not_found", r1.getBody().get("error"));
        assertEquals(403, r2.getStatusCode().value());
        assertEquals("chat_forbidden", r2.getBody().get("error"));
    }

}
