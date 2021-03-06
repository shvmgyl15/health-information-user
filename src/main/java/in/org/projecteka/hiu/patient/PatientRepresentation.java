package in.org.projecteka.hiu.patient;

import in.org.projecteka.hiu.clients.Patient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class PatientRepresentation {
    private String id;
    private String name;

    public static PatientRepresentation from(Patient patient) {
        return new PatientRepresentation(patient.getIdentifier(), patient.getFirstName());
    }

    public Patient toPatient() {
        return new Patient(this.getId(), this.getName(), "");
    }
}
