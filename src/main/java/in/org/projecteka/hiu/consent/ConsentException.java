package in.org.projecteka.hiu.consent;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ConsentException extends Throwable {
    private ErrorCode code;
    private String message;

    public static ConsentException creationFailed() {
        final String failedToCreateRequest = "Failed to create consent request";
        return new ConsentException(ErrorCode.CREATION_FAILED, failedToCreateRequest);
    }

    public static ConsentException fetchConsentArtefactFailed() {
        final String failedToCreateRequest = "Failed to fetch consent artefact";
        return new ConsentException(ErrorCode.FETCH_CONSENT_ARTEFACT_FAILED, failedToCreateRequest);
    }

    public static ConsentException failedToInitiateDataFlowRequest() {
        final String failedToInitiateDataFlowRequest = "Failed to initiate data flow request";
        return new ConsentException(ErrorCode.FAILED_TO_INITIATE_DATAFLOW_REQUEST, failedToInitiateDataFlowRequest);
    }

    private enum ErrorCode {
        CREATION_FAILED,
        FETCH_CONSENT_ARTEFACT_FAILED,
        FAILED_TO_INITIATE_DATAFLOW_REQUEST
    }
}