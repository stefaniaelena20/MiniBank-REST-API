package ro.axonsoft.eval.minibank.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ErrorResponse {
    private String status;
    private String message;

    public static ErrorResponse rejected(String message) {
        return new ErrorResponse("REJECTED", message);
    }
}
