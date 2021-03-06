package in.org.projecteka.hiu.user.model;

import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class Patient {
    String id;
    String name;
    Gender gender;
    Integer yearOfBirth;
    Address address;
}