package domaindrivers.smartschedule.availability;


import domaindrivers.smartschedule.allocation.ResourceId;
import domaindrivers.smartschedule.shared.timeslot.TimeSlot;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class AvailabilityFacade {
    private final ResourceAvailabilityRepository resourceAvailabilityRepository;

    public AvailabilityFacade(ResourceAvailabilityRepository resourceAvailabilityRepository) {
        this.resourceAvailabilityRepository = resourceAvailabilityRepository;
    }

    public void createResourceSlots(ResourceId resourceId, TimeSlot timeslot) {
        timeslot.splitToBlocks()
                .forEach(block -> {
                    ResourceAvailability resourceAvailability = new ResourceAvailability(resourceId, block);
                    resourceAvailabilityRepository.save(resourceAvailability);
                });
    }

    public boolean block(ResourceId resourceId, TimeSlot timeSlot, Owner requester) {
        Set<ResourceAvailability> resourceAvailabilities = resourceAvailabilityRepository
                .findAllByResourceIdAndTimeSlot(resourceId, timeSlot);
        if (resourceAvailabilities.size() != timeSlot.blockedBlocksCount()) {
            return false;
        }
        for (ResourceAvailability resourceAvailability : resourceAvailabilities) {
            if (!resourceAvailability.block(requester)) {
                return false;
            }
        }
        resourceAvailabilityRepository.saveAll(resourceAvailabilities);
        return true;
    }

    public boolean release(ResourceId resourceId, TimeSlot timeSlot, Owner requester) {
        Set<ResourceAvailability> resourceAvailabilities = resourceAvailabilityRepository
                .findAllByResourceIdAndTimeSlot(resourceId, timeSlot);
        for (ResourceAvailability resourceAvailability : resourceAvailabilities) {
            if (!resourceAvailability.release(requester)) {
                return false;
            }
        }
        resourceAvailabilityRepository.saveAll(resourceAvailabilities);
        return true;
    }

    public boolean disable(ResourceId resourceId, TimeSlot timeSlot, Owner requester) {
        Set<ResourceAvailability> resourceAvailabilities = resourceAvailabilityRepository
                .findAllByResourceIdAndTimeSlot(resourceId, timeSlot);
        if (resourceAvailabilities.size() != timeSlot.blockedBlocksCount()) {
            return false;
        }
        resourceAvailabilities.forEach(resourceAvailability -> resourceAvailability.disable(requester));
        resourceAvailabilityRepository.saveAll(resourceAvailabilities);
        return true;
    }
}


