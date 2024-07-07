package domaindrivers.smartschedule.availability;

import domaindrivers.smartschedule.allocation.ResourceId;
import domaindrivers.smartschedule.shared.timeslot.TimeSlot;
import jakarta.persistence.*;

@Entity(name = "resource_availability")
public class ResourceAvailability {
    @EmbeddedId
    private ResourceAvailabilityId id;

    @Embedded
    private ResourceId resourceId;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "from", column = @Column(name = "from_date")),
            @AttributeOverride(name = "to", column = @Column(name = "to_date"))
    })
    private TimeSlot timeSlot;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "owner", column = @Column(name = "owner_id"))
    })
    private Owner owner;

    @Enumerated(EnumType.STRING)
    private ResourceStatus resourceStatus;

    public ResourceAvailability() {}

    public ResourceAvailability(ResourceId resourceId, TimeSlot timeSlot) {
        this.id = ResourceAvailabilityId.newOne();
        this.resourceId = resourceId;
        this.timeSlot = timeSlot;
        this.owner = null;
        this.resourceStatus = ResourceStatus.AVAILABLE;
    }

    public boolean block(Owner requester) {
        if (this.resourceStatus != ResourceStatus.AVAILABLE) {
            return false;
        }
        this.owner = requester;
        this.resourceStatus = ResourceStatus.BLOCKED;
        return true;
    }

    public boolean disable(Owner requester) {
        this.owner = requester;
        this.resourceStatus = ResourceStatus.DISABLED;
        return true;
    }

    public boolean release(Owner requester) {
        if (!this.owner.equals(requester) || this.resourceStatus == ResourceStatus.DISABLED) {
            return false;
        }
        this.owner = null;
        this.resourceStatus = ResourceStatus.AVAILABLE;
        return true;
    }

    public TimeSlot timeSlot() {
        return this.timeSlot;
    }
}
