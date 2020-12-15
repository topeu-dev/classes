package com.perspective.smev3_integration.dispatcher.service.response_polling;

import com.perspective.smev3_integration.dispatcher.configuration.properties.document.DocumentProperty;
import com.perspective.smev3_integration.dispatcher.configuration.properties.document.DocumentsProperties;
import com.perspective.smev3_integration.dispatcher.messaging.pojo.DispatcherResponse;
import com.perspective.smev3_integration.dispatcher.service.ack.AckService;
import com.perspective.smev3_integration.dispatcher.service.dispatcher.ResponseDispatcher;
import com.perspective.smev3_integration.dispatcher.service.response_polling.strategy.GetResponseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class ResponsePollingService {

    private final GetResponseService getResponseService;
    private final ResponseDispatcher responseDispatcher;
    private final AckService ackService;
    private final DispatcherResponseMaker dispatcherResponseMaker;

    private final DocumentsProperties documentsProperties;

    @PostConstruct
    public void init() {
        documentsProperties.getMapper()
                .values()
                .stream()
                .filter(DocumentProperty::getEnabled)
                .forEach((documentProperty) -> {
                    ScheduledExecutorService executor = Executors.newScheduledThreadPool(documentProperty.getPollingThreadCount());
                    for (int i = 0; i < documentProperty.getPollingThreadCount(); i++) {
                        executor.scheduleAtFixedRate(
                                () -> poll(documentProperty),
                                documentProperty.getPollingInitialDelay(),
                                documentProperty.getPollingDelay(),
                                TimeUnit.MILLISECONDS);
                    }
                });
    }

    private void poll(DocumentProperty property) {
        try {
            getResponseService.getResponse(property)
                    .ifPresent(
                            wrappedSmevResponse -> {
                                DispatcherResponse dispatcherResponse = dispatcherResponseMaker.makeFrom(wrappedSmevResponse);
                                responseDispatcher.dispatch(dispatcherResponse);
                                ackService.ack(wrappedSmevResponse.getResponseMessageId());
                            }
                    );
        } catch (Exception e) {
            log.debug("Pooling operation failed!", e);
        }
    }
}
