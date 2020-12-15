package com.perspective.provider_report.commons;

import com.perspective.object_pipeline.wrapper.IPayloadWrapper;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class PayloadUtil {
    @SuppressWarnings({"unchecked"})
    public static IPayloadWrapper applyFunctionToAllStringNodes(IPayloadWrapper payloadWrapper, Function<String, String> function) {
        Map<String, Object> payloadBody = payloadWrapper.getBody();
        for (Map.Entry<String, Object> entry : payloadBody.entrySet()) {
            if (entry.getValue() instanceof String) {
                entry.setValue(function.apply((String) entry.getValue()));
            }
            if (entry.getValue() instanceof IPayloadWrapper) {
                applyFunctionToAllStringNodes((IPayloadWrapper) entry.getValue(), function);
            }
            if (entry.getValue() instanceof List) {
                for (Object item : ((List) entry.getValue())) {

                    if (item instanceof String) {
                        ((List) entry.getValue()).set(((List) entry.getValue()).indexOf(item), function.apply((String) item));
                    }
                    if (item instanceof IPayloadWrapper) {
                        applyFunctionToAllStringNodes((IPayloadWrapper) item, function);
                    }
                }
            }
        }
        return payloadWrapper;
    }
}
