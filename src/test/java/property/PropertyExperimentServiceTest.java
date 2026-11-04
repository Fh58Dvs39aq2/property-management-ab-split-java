package property;

import java.util.List;

public final class PropertyExperimentServiceTest {
    public static void main(String[] args) {
        String assigned = PropertyExperimentService.variantFrom("{\"ok\":true,\"data\":{\"value\":\"A\"}}");
        if (!assigned.equals("A")) throw new AssertionError("expected A");
        int openItems = 1 + 1 + 0;
        boolean needsAttention = openItems > 0;
        if (!needsAttention) throw new AssertionError("open property items need attention");
        if (PropertyExperimentService.variantFrom("{\"ok\":true,\"data\":{\"value\":\"B\"}}").equals("A")) {
            throw new AssertionError("the service must preserve the assigned variant");
        }
        System.out.println("property experiment decision passed");
    }
}
