package de.jensgiehl.marcqr.web;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class AppErrorController implements ErrorController {

    @RequestMapping("/error")
    public String error(HttpServletRequest request, Model model) {
        int statusCode = resolveStatus(request);
        model.addAttribute("statusCode", statusCode);
        if (statusCode == HttpStatus.NOT_FOUND.value()) {
            return "error/404";
        }
        if (statusCode >= 500) {
            return "error/500";
        }
        model.addAttribute("message", resolveMessage(request));
        return "error/error";
    }

    private int resolveStatus(HttpServletRequest request) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        if (status instanceof Integer statusCode) {
            return statusCode;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR.value();
    }

    private String resolveMessage(HttpServletRequest request) {
        Object message = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        return message == null || message.toString().isBlank()
                ? "Die Anfrage konnte leider nicht verarbeitet werden."
                : message.toString();
    }
}
