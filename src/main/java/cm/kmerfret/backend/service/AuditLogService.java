package cm.kmerfret.backend.service;

import cm.kmerfret.backend.model.AuditLog;
import cm.kmerfret.backend.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository repo;

    @Async
    public void log(String actorEmail, String action, String entityType,
                    String entityId, String details, String ip,
                    String method, String path, int status) {
        AuditLog log = AuditLog.builder()
                .actorEmail(actorEmail)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .details(details)
                .ipAddress(ip)
                .httpMethod(method)
                .requestPath(path)
                .responseStatus(status)
                .build();
        repo.save(log);
    }

    @Async
    public void logAction(String actorEmail, String action) {
        log(actorEmail, action, null, null, null, null, null, null, 200);
    }
}
