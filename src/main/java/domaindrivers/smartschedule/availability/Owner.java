package domaindrivers.smartschedule.availability;

import jakarta.persistence.Embeddable;

import java.util.UUID;

@Embeddable
public record Owner(UUID owner) {

    static Owner none() {
        return new Owner(null);
    }

    static Owner newOne() {
        return new Owner(UUID.randomUUID());
    }

}
