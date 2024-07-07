package domaindrivers.smartschedule.availability;

import domaindrivers.smartschedule.allocation.ResourceId;
import domaindrivers.smartschedule.shared.timeslot.TimeSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Set;
import java.util.stream.Collectors;

@Repository
public interface ResourceAvailabilityRepository extends JpaRepository<ResourceAvailability, ResourceAvailabilityId> {

    @Query("SELECT ra FROM resource_availability ra WHERE ra.resourceId = :resourceId")
    Set<ResourceAvailability> findAllByResourceId(ResourceId resourceId);

    @Query(
            "SELECT ra FROM resource_availability ra " +
            "WHERE ra.resourceId = :resourceId AND ra.resourceStatus = :resourceStatus"
    )
    Set<ResourceAvailability> findAllByResourceIdResourceStatus(ResourceId resourceId, ResourceStatus resourceStatus);

    default Set<ResourceAvailability> findAllByResourceIdAndTimeSlot(ResourceId resourceId, TimeSlot timeSlot) {
        return findAllByResourceId(resourceId)
                .stream()
                .filter(resourceAvailability -> resourceAvailability.timeSlot().within(timeSlot))
                .collect(Collectors.toSet());
    }

    default Set<ResourceAvailability> findAllByResourceIdAndTimeSlotAndResourceStatus(
            ResourceId resourceId,
            TimeSlot timeSlot,
            ResourceStatus resourceStatus
    ) {
        return findAllByResourceIdResourceStatus(resourceId, resourceStatus)
                .stream()
                .filter(resourceAvailability -> resourceAvailability.timeSlot().within(timeSlot))
                .collect(Collectors.toSet());
    }
}
