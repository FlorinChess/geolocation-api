package api.geolocation.exceptions;

import org.json.simple.parser.ParseException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@ControllerAdvice
public class MapControllerAdvisor extends ResponseEntityExceptionHandler {
    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(value = HttpStatus.NOT_FOUND)
    public String notFoundException(NotFoundException exception) {
        return URLEncoder.encode(exception.getMessage(), StandardCharsets.UTF_8);
    }

    @ExceptionHandler(InternalIssuesException.class)
    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    public String internalIssuesException(InternalIssuesException exception) {
        return URLEncoder.encode(exception.getMessage(), StandardCharsets.UTF_8);
    }

    @ExceptionHandler(InvalidRequestException.class)
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    public String invalidRequestException(InvalidRequestException exception) {
        return URLEncoder.encode(exception.getMessage(), StandardCharsets.UTF_8);
    }

    @ExceptionHandler(ParseException.class)
    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    public String parseException(ParseException exception) {
        return URLEncoder.encode(exception.getMessage(), StandardCharsets.UTF_8);
    }
}
