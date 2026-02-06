package dto;

import entity.SupportTicket.SupportCategory;
import lombok.Data;

@Data
public class SupportRequestDto {

    private Long userId;
    private String subject;
    private String message;
    private String role;
    private SupportCategory category;
}
