package in.org.projecteka.hiu.consent;

import in.org.projecteka.hiu.Caller;
import in.org.projecteka.hiu.consent.model.ConsentCreationResponse;
import in.org.projecteka.hiu.consent.model.ConsentNotificationRequest;
import in.org.projecteka.hiu.consent.model.ConsentRequestData;
import in.org.projecteka.hiu.consent.model.ConsentRequestInitResponse;
import in.org.projecteka.hiu.consent.model.ConsentRequestRepresentation;
import in.org.projecteka.hiu.consent.model.ConsentFetchResponse;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@AllArgsConstructor
public class ConsentController {
    private final ConsentService consentService;

    @PostMapping("/consent-requests")
    public Mono<ConsentCreationResponse> createConsentRequest(@RequestBody ConsentRequestData consentRequestData) {
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> (Caller) securityContext.getAuthentication().getPrincipal())
                .map(Caller::getUsername)
                .flatMap(requesterId -> consentService.create(requesterId, consentRequestData));
    }

    @PostMapping("/consent/notification")
    public Mono<Void> consentNotification(@RequestBody ConsentNotificationRequest consentNotificationRequest) {
        return consentService.handleNotification(consentNotificationRequest);
    }

    @GetMapping("/consent-requests")
    public Flux<ConsentRequestRepresentation> consentsFor() {
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> (Caller) securityContext.getAuthentication().getPrincipal())
                .map(Caller::getUsername)
                .flatMapMany(username -> consentService.requestsFrom(username));
    }

    @PostMapping("/v1/hiu/consent-requests")
    public Mono<ResponseEntity> postConsentRequest(@RequestBody ConsentRequestData consentRequestData) {
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> (Caller) securityContext.getAuthentication().getPrincipal())
                .map(Caller::getUsername)
                .flatMap(requesterId -> consentService.createRequest(requesterId, consentRequestData))
                .thenReturn(new ResponseEntity<>(HttpStatus.ACCEPTED));
    }

    @PostMapping("/v1/consent-requests/on-init")
    public Mono<ResponseEntity> onInitConsentRequest(@RequestBody ConsentRequestInitResponse consentRequestInitResponse) {
        return consentService.updatePostedRequest(consentRequestInitResponse)
                .thenReturn(new ResponseEntity<>(HttpStatus.ACCEPTED));
    }

    @GetMapping("/v1/hiu/consent-requests")
    public Flux<ConsentRequestRepresentation> consentRequests() {
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> (Caller) securityContext.getAuthentication().getPrincipal())
                .map(Caller::getUsername)
                .flatMapMany(username -> consentService.requestsOf(username));
    }

    @PostMapping("/v1/consent/on-fetch")
    public Mono<ResponseEntity> onFetch(@RequestBody ConsentFetchResponse consentFetchResponse) {
        return consentService.saveFetchedResponse(consentFetchResponse)
                .thenReturn(new ResponseEntity<>(HttpStatus.ACCEPTED));
    }
}
