package domaindrivers.smartschedule.allocation;

import domaindrivers.smartschedule.allocation.capabilityscheduling.AllocatableCapabilityId;
import domaindrivers.smartschedule.allocation.capabilityscheduling.CapabilityFinder;
import domaindrivers.smartschedule.availability.AvailabilityFacade;
import domaindrivers.smartschedule.availability.Owner;
import domaindrivers.smartschedule.shared.capability.Capability;
import domaindrivers.smartschedule.shared.timeslot.TimeSlot;
import jakarta.transaction.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;


public class AllocationFacade {

    private final ProjectAllocationsRepository projectAllocationsRepository;
    private final AvailabilityFacade availabilityFacade;
    private final CapabilityFinder capabilityFinder;
    private final Clock clock;

    public AllocationFacade(ProjectAllocationsRepository projectAllocationsRepository, AvailabilityFacade availabilityFacade, CapabilityFinder capabilityFinder, Clock clock) {
        this.projectAllocationsRepository = projectAllocationsRepository;
        this.availabilityFacade = availabilityFacade;
        this.capabilityFinder = capabilityFinder;
        this.clock = clock;
    }

    @Transactional
    public ProjectAllocationsId createAllocation(TimeSlot timeSlot, Demands scheduledDemands) {
        ProjectAllocationsId projectId = ProjectAllocationsId.newOne();
        ProjectAllocations projectAllocations = new ProjectAllocations(projectId, Allocations.none(), scheduledDemands, timeSlot);
        projectAllocationsRepository.save(projectAllocations);
        return projectId;
    }

    public ProjectsAllocationsSummary findAllProjectsAllocations(Set<ProjectAllocationsId> projectIds) {
        return ProjectsAllocationsSummary.of(projectAllocationsRepository.findAllById(projectIds));
    }

    public ProjectsAllocationsSummary findAllProjectsAllocations() {
        return ProjectsAllocationsSummary.of(projectAllocationsRepository.findAll());
    }

    @Transactional
    public Optional<UUID> allocateToProject(ProjectAllocationsId projectId, AllocatableCapabilityId allocatableCapabilityId, Capability capability, TimeSlot timeSlot) {
        //yes, one transaction crossing 2 modules.
        if (!capabilityFinder.isPresent(allocatableCapabilityId)) {
            return Optional.empty();
        }
        if (!availabilityFacade.block(allocatableCapabilityId.toAvailabilityResourceId(), timeSlot, Owner.of(projectId.id()))) {
            return Optional.empty();
        }
        ProjectAllocations allocations = projectAllocationsRepository.findById(projectId).orElseThrow();
        Optional<CapabilitiesAllocated> event = allocations.allocate(allocatableCapabilityId, capability, timeSlot, Instant.now(clock));
        projectAllocationsRepository.save(allocations);
        return event.map(CapabilitiesAllocated::allocatedCapabilityId);
    }

    @Transactional
    public boolean releaseFromProject(ProjectAllocationsId projectId, AllocatableCapabilityId allocatableCapabilityId, TimeSlot timeSlot) {
        //can release not scheduled capability - at least for now. Hence no check to capabilityFinder
        availabilityFacade.release(allocatableCapabilityId.toAvailabilityResourceId(), timeSlot, Owner.of(projectId.id()));
        ProjectAllocations allocations = projectAllocationsRepository.findById(projectId).orElseThrow();
        Optional<CapabilityReleased> event = allocations.release(allocatableCapabilityId, timeSlot, Instant.now(clock));
        projectAllocationsRepository.save(allocations);
        return event.isPresent();
    }

    @Transactional
    public void editProjectDates(ProjectAllocationsId projectId, TimeSlot fromTo) {
        ProjectAllocations projectAllocations = projectAllocationsRepository.findById(projectId).orElseThrow();
        projectAllocations.defineSlot(fromTo, clock.instant());
    }

    @Transactional
    public void scheduleProjectAllocationDemands(ProjectAllocationsId projectId, Demands demands) {
        ProjectAllocations projectAllocations =
                projectAllocationsRepository.findById(projectId)
                        .orElseGet(() -> ProjectAllocations.empty(projectId));
        projectAllocations.addDemands(demands, Instant.now(clock));
        projectAllocationsRepository.save(projectAllocations);
    }
}

