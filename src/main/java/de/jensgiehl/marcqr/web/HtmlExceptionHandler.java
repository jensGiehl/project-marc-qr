package de.jensgiehl.marcqr.web;

import de.jensgiehl.marcqr.code.CodeValidationException;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice(basePackages = "de.jensgiehl.marcqr.code")
public class HtmlExceptionHandler {

    @ExceptionHandler(CodeValidationException.class)
    public String handleCodeValidation(CodeValidationException exception,
                                       HttpServletResponse response,
                                       Model model) {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        model.addAttribute("statusCode", 400);
        model.addAttribute("message", exception.getMessage());
        return "error/error";
    }
}
