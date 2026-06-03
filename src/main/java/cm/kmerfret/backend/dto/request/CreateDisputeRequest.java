package cm.kmerfret.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public class CreateDisputeRequest {

    @NotNull
    private UUID missionId;

    @NotBlank
    @Size(max = 50)
    private String disputeType; // DAMAGED_GOODS, PAYMENT_ISSUE, DELAY, DRIVER_BEHAVIOR, OTHER

    @NotBlank
    @Size(max = 1000)
    private String description;

    public UUID getMissionId() { return missionId; }
    public void setMissionId(UUID missionId) { this.missionId = missionId; }
    public String getDisputeType() { return disputeType; }
    public void setDisputeType(String disputeType) { this.disputeType = disputeType; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
