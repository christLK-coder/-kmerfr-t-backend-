package cm.kmerfret.backend.dto.response;

import cm.kmerfret.backend.model.Review;
import java.time.OffsetDateTime;
import java.util.UUID;

public class ReviewResponse {
    private UUID id;
    private UUID missionId;
    private UUID reviewerId;
    private String reviewerName;
    private UUID reviewedId;
    private String reviewedName;
    private Short rating;
    private String comment;
    private OffsetDateTime createdAt;

    public static ReviewResponse from(Review r) {
        ReviewResponse res = new ReviewResponse();
        res.id          = r.getId();
        res.missionId   = r.getMission().getId();
        res.reviewerId  = r.getReviewer().getId();
        res.reviewerName = r.getReviewer().getFullName();
        res.reviewedId  = r.getReviewed().getId();
        res.reviewedName = r.getReviewed().getFullName();
        res.rating      = r.getRating();
        res.comment     = r.getComment();
        res.createdAt   = r.getCreatedAt();
        return res;
    }

    public UUID getId() { return id; }
    public UUID getMissionId() { return missionId; }
    public UUID getReviewerId() { return reviewerId; }
    public String getReviewerName() { return reviewerName; }
    public UUID getReviewedId() { return reviewedId; }
    public String getReviewedName() { return reviewedName; }
    public Short getRating() { return rating; }
    public String getComment() { return comment; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
