package in.org.projecteka.hiu.dataflow;

import in.org.projecteka.hiu.ClientError;
import in.org.projecteka.hiu.DestinationsConfig;
import in.org.projecteka.hiu.common.RabbitQueueNames;
import in.org.projecteka.hiu.common.TraceableMessage;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.core.AmqpTemplate;
import reactor.core.publisher.Mono;

import java.util.Map;

import static in.org.projecteka.hiu.common.Constants.CORRELATION_ID;

@AllArgsConstructor
public class DataAvailabilityPublisher {

    private static final Logger logger = LoggerFactory.getLogger(DataAvailabilityPublisher.class);
    private final AmqpTemplate amqpTemplate;
    private final DestinationsConfig destinationsConfig;
    private final RabbitQueueNames queueNames;

    @SneakyThrows
    public Mono<Void> broadcastDataAvailability(Map<String, String> contentRef) {
        DestinationsConfig.DestinationInfo destinationInfo =
                destinationsConfig.getQueues().get(queueNames.getDataFlowProcessQueue());
        TraceableMessage traceableMessage = TraceableMessage.builder()
                .correlationId(MDC.get(CORRELATION_ID))
                .message(contentRef)
                .build();

        if (destinationInfo == null) {
            logger.info(String.format("Queue %s not found",queueNames.getDataFlowProcessQueue()));
            throw ClientError.queueNotFound();
        }

        return Mono.create(monoSink -> {
            amqpTemplate.convertAndSend(
                    destinationInfo.getExchange(),
                    destinationInfo.getRoutingKey(),
                    traceableMessage);
            logger.info("Broadcasting data availability for transaction id : " + contentRef.toString());
            monoSink.success();
        });

    }
}
