package controller;

import java.util.List;


import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import entity.SupportTicket.SupportCategory;


import dto.SupportRequestDto;
import dto.apiResponse;
import entity.SupportTicket;
import entity.user;
import org.springframework.web.bind.annotation.RequestBody;
import service.SupportService;

@RestController
@RequestMapping("/api/support")
public class SupportController {
	private final SupportService supportService;

    public SupportController(SupportService supportService) {
        this.supportService = supportService;
    }

    @PostMapping("/ticket")
    public apiResponse<String> createTicket(@RequestBody SupportRequestDto request) {
        user currentUser = (user) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        supportService.createTicket(
                currentUser.getId(),
                request.getSubject(),
                request.getMessage(),
                currentUser.getRole(),
                request.getCategory()
        );

        return new apiResponse<>("Support ticket created", true, null);
    }

    @GetMapping("/my-tickets")
    public apiResponse<List<SupportTicket>> myTickets() {
        user currentUser = (user) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        return new apiResponse<>(
                "My support tickets",
                true,
                supportService.getUserTickets(currentUser.getId())
        );
    }
}
