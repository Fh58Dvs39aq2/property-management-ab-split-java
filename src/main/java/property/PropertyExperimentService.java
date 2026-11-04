package property;

import java.io.IOException;
import java.util.List;

public final class PropertyExperimentService {
    record MaintenanceRequest(String id, boolean open) {}
    record TenantDocument(String id, boolean unread) {}
    record InspectionReminder(String id, boolean due) {}
    record ExperimentDecision(String userId, String variant, int openItems, boolean needsAttention) {}

    private final InfraiClient infrai;

    public PropertyExperimentService(InfraiClient infrai) {
        this.infrai = infrai;
    }

    ExperimentDecision decide(String userId, String flagKey,
                              List<MaintenanceRequest> maintenance,
                              List<TenantDocument> documents,
                              List<InspectionReminder> inspections)
            throws IOException, InterruptedException {
        String userEnvelope = infrai.user(userId);
        String flagEnvelope = infrai.value(flagKey, userId);
        if (!userEnvelope.contains("\"ok\":true") || !flagEnvelope.contains("\"ok\":true")) {
            throw new IllegalStateException("The Infrai envelope did not confirm the request");
        }
        String variant = variantFrom(flagEnvelope);
        int openItems = (int) maintenance.stream().filter(MaintenanceRequest::open).count()
                + (int) documents.stream().filter(TenantDocument::unread).count()
                + (int) inspections.stream().filter(InspectionReminder::due).count();
        return new ExperimentDecision(userId, variant, openItems, openItems > 0);
    }

    static String variantFrom(String envelope) {
        int marker = envelope.indexOf("\"data\"");
        int value = envelope.indexOf("\"value\"", marker);
        if (value >= 0) {
            int colon = envelope.indexOf(':', value);
            int quote = envelope.indexOf('"', colon);
            int end = envelope.indexOf('"', quote + 1);
            if (quote >= 0 && end > quote) return envelope.substring(quote + 1, end);
        }
        throw new IllegalStateException("The flag response did not contain a value");
    }

    public static void main(String[] args) throws Exception {
        String key = required("INFRAI_API_KEY");
        String flagKey = required("INFRAI_FLAG_KEY");
        String userId = required("INFRAI_USER_ID");
        PropertyExperimentService service = new PropertyExperimentService(new InfraiClient(key));
        ExperimentDecision decision = service.decide(userId, flagKey,
                List.of(new MaintenanceRequest("repair-18", true)),
                List.of(new TenantDocument("lease-18", true)),
                List.of(new InspectionReminder("inspection-18", false)));
        System.out.println("user=" + decision.userId() + " variant=" + decision.variant()
                + " openItems=" + decision.openItems());
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " is required");
        return value;
    }
}
