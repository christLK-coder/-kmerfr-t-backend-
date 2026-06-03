package cm.kmerfret.backend.dto.request;

import jakarta.validation.constraints.*;

public class CreateReviewRequest {

    @NotNull
    private java.util.UUID missionId;

    @NotNull @Min(1) @Max(5)
    private Short rating;

    @Size(max = 500)
    private String comment;

    public java.util.UUID getMissionId() { return missionId; }
    public void setMissionId(java.util.UUID missionId) { this.missionId = missionId; }

    public Short getRating() { return rating; }
    public void setRating(Short rating) { this.rating = rating; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
