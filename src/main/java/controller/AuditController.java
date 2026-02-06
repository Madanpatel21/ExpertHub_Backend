//package controller;
//
//import java.util.List;
//
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PathVariable;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//import Repository.auditLogRepository;
//import dto.apiResponse;
//import entity.auditLog;
//
//@RestController
//@RequestMapping("/internal/admin/audit")
//public class AuditController {
//	private final auditLogRepository AuditRepo;
//	
//	public AuditController(auditLogRepository AuditRepo) {
//		this.AuditRepo=AuditRepo;
//	}
//	@GetMapping("/all")
//	public apiResponse<List<auditLog>> getAllLogs(){
//		return new apiResponse<>(
//				"Audit Log Fetched",
//				true,
//				AuditRepo.findAll()
//				
//				);
//	}
//	
//	@GetMapping("/actor/{email}")
//	public apiResponse<List<auditLog>> getByActor(@PathVariable String email){
//		return new apiResponse<>(
//				"Audit logs for admin",
//				true,
//				AuditRepo.findByActor(email)
//				);
//	}
//	
//	
//}
