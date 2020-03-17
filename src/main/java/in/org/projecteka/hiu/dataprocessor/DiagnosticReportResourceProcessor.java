package in.org.projecteka.hiu.dataprocessor;

import in.org.projecteka.hiu.dataprocessor.model.DataContext;
import in.org.projecteka.hiu.dicomweb.DicomStudy;
import in.org.projecteka.hiu.dicomweb.OrthancDicomWebServer;
import lombok.SneakyThrows;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.Media;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DiagnosticReportResourceProcessor implements HITypeResourceProcessor {

    public static final String VS_SYSTEM_DIAGNOSTIC_SERVICE_SECTIONS = "http://hl7.org/fhir/ValueSet/diagnostic" +
            "-service-sections";
    public static final String RADILOGY_CATEGORY_CODE = "RAD";
    private final String DEFAULT_FILE_EXTN = ".txt";
    private Map<String, String> mediaTypeToFileExtnMap = new HashMap<>() {{
        put("APPLICATION/PDF", ".pdf");
        put("APPLICATION/DICOM", ".dcm");
        put("APPLICATION/MSWORD", ".doc");
        put("TEXT/RTF", ".rtf");
    }};
    private OrthancDicomWebServer localDicomWebServer;

    public DiagnosticReportResourceProcessor(OrthancDicomWebServer localDicomWebServer) {
        this.localDicomWebServer = localDicomWebServer;
    }

    @Override
    public boolean supports(ResourceType type) {
        return type.equals(ResourceType.DiagnosticReport);
    }

    @Override
    public void process(Resource resource, DataContext context) {
        DiagnosticReport diagnosticReport = (DiagnosticReport) resource;
        processPresentedForm(diagnosticReport, context.getLocalStoragePath());
        processMedia(diagnosticReport, context.getLocalStoragePath());
    }

    private void processMedia(DiagnosticReport diagnosticReport, Path localStoragePath) {
        List<DiagnosticReport.DiagnosticReportMediaComponent> mediaList = diagnosticReport.getMedia();
        if (mediaList.isEmpty()) {
            return;
        }

        boolean radiologyCategory = isRadiologyCategory(diagnosticReport);

        for (DiagnosticReport.DiagnosticReportMediaComponent media : mediaList) {
            if (media.hasLink()) {
                Media linkTarget = (Media) media.getLink().getResource();
                Path savedAttachmentPath = saveAttachmentAsFile(linkTarget.getContent(), localStoragePath);
                if (radiologyCategory && isRadiologyFile(linkTarget.getContent())) {
                    uploadToLocalDicomServer(linkTarget.getContent(), savedAttachmentPath);
                }
            }
        }
    }

    private void processPresentedForm(DiagnosticReport diagnosticReport, Path localStorePath) {
        if (diagnosticReport.hasPresentedForm()) {
            List<Attachment> presentedForm = diagnosticReport.getPresentedForm();
            for (Attachment attachment : presentedForm) {
                if (hasLink(attachment)) {
                    downloadAndSaveFile(attachment, localStorePath);
                } else {
                    saveAttachmentAsFile(attachment, localStorePath);
                }
            }
        }
    }

    private Path saveAttachmentAsFile(Attachment attachment, Path localStorePath) throws RuntimeException {
        if (attachment.getData() != null) {
            byte[] data = Base64.getDecoder().decode(attachment.getDataElement().getValueAsString());
            Path attachmentFilePath = getFileAttachmentPath(attachment, localStorePath);
            try (FileChannel channel = (FileChannel) Files.newByteChannel(attachmentFilePath,
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
                ByteBuffer buffer = ByteBuffer.allocate(data.length);
                buffer.put(data);
                buffer.flip();
                channel.write(buffer);
            } catch (IOException ex) {
                ex.printStackTrace();
                throw new RuntimeException(ex);
            }
            attachment.setData(null);
            attachment.setUrl(referenceWebUrl(attachmentFilePath));
            return attachmentFilePath;
        } else {
            return downloadAndSaveFile(attachment, localStorePath);
        }
    }

    @SneakyThrows
    private Path downloadAndSaveFile(Attachment attachment, Path localStorePath) {
        Path attachmentFilePath = getFileAttachmentPath(attachment, localStorePath);
        HttpGet request = new HttpGet(URI.create(attachment.getUrl()));
        try (CloseableHttpClient client = HttpClientBuilder.create().build();
             CloseableHttpResponse response = client.execute(request)) {
            HttpEntity entity = response.getEntity();
            InputStream inputStream = entity.getContent();
            Files.copy(inputStream, attachmentFilePath);
        }
        attachment.setUrl(referenceWebUrl(attachmentFilePath));
        return attachmentFilePath;
    }

    private Path getFileAttachmentPath(Attachment attachment, Path localStorePath) {
        String randomFileName = UUID.randomUUID().toString() + getFileExtension(attachment);
        return Paths.get(localStorePath.toString(), randomFileName);
    }

    private String referenceWebUrl(Path attachmentFilePath) {
        //TODO create a referenceable path so that, UI can use that. maybe startwith /
        return String.format("/attachments/%s", attachmentFilePath.getFileName().toString());
    }

    private String getFileExtension(Attachment attachment) {
        String extension = mediaTypeToFileExtnMap.get(attachment.getContentType().toUpperCase());
        return (extension != null) ? extension : DEFAULT_FILE_EXTN;
    }

    private boolean hasLink(Attachment attachment) {
        return (attachment.getUrl() != null) && !attachment.getUrl().isBlank();
    }

    private boolean isRadiologyFile(Attachment attachment) {
        String extension = mediaTypeToFileExtnMap.get(attachment.getContentType().toUpperCase());
        return (extension != null) && extension.equals(".dcm");
    }

    private boolean isRadiologyCategory(DiagnosticReport diagnosticReport) {
        return diagnosticReport.getCategoryFirstRep().getCoding().stream()
                .filter(c -> c.getCode().equalsIgnoreCase(RADILOGY_CATEGORY_CODE)).count() > 0;
    }

    private void uploadToLocalDicomServer(Attachment content, Path savedFilePath) {
        if (localDicomWebServer.exists()) {
            DicomStudy dicomStudy = localDicomWebServer.upload(savedFilePath);
            content.setUrl(referenceLocalDicomServerUrl(dicomStudy.getStudyInstanceUid()));
        }
    }

    private String referenceLocalDicomServerUrl(String studyInstanceUid) {
        return String.format("/dicom-server/studies/%s", studyInstanceUid);
    }
}