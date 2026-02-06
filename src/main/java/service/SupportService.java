package service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import Repository.SupportTicketRepository;
import entity.SupportTicket;
import entity.SupportTicket.SupportCategory;
import entity.roles;
import exception.badRequestException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SupportService {
	private final SupportTicketRepository repository;
	private final EmailService emailService;

	public void createTicket(Long userId, String subject, String message, roles userRole, SupportCategory category) {
		SupportTicket ticket = new SupportTicket();

		ticket.setUserId(userId);
		ticket.setRole(userRole);
		ticket.setSubject(subject);
		ticket.setMessage(message);
		ticket.setCategory(category);
		ticket.setStatus(SupportTicket.Status.OPEN);
		ticket.setCreatedAt(LocalDateTime.now());

		repository.save(ticket);

		// Email notification to support team
		emailService.sendSimpleMessage("support@experthub.com", "New Support Ticket: " + subject,
				"User ID: " + userId + " (" + userRole.getName() + ") has submitted a new ticket.\n\nMessage: "
						+ message);
	}

	public List<SupportTicket> getUserTickets(Long userId) {
		return repository.findByUserId(userId);
	}

	public List<SupportTicket> getAllTickets() {
		return repository.findAll();
	}

	public void closeTicket(Long ticketId) {
		SupportTicket ticket = repository.findById(ticketId)
				.orElseThrow(() -> new badRequestException("Ticket not found"));
		ticket.setStatus(SupportTicket.Status.CLOSED);
		repository.save(ticket);
	}
}
