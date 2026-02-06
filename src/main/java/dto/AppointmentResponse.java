package dto;

public class AppointmentResponse {

    private Long appointmentId;
    private String status;

    public AppointmentResponse(Long appointmentId, String status) {
        this.appointmentId = appointmentId;
        this.status = status;
    }

    public Long getAppointmentId() {
        return appointmentId;
    }

    public String getStatus() {
        return status;
    }
}
