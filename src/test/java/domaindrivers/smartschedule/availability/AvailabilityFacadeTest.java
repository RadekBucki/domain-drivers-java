package domaindrivers.smartschedule.availability;

import domaindrivers.smartschedule.TestDbConfiguration;
import domaindrivers.smartschedule.allocation.ResourceId;
import domaindrivers.smartschedule.shared.timeslot.TimeSlot;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;

import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = {TestDbConfiguration.class})
@Sql(scripts = "classpath:schema-availability.sql")
class AvailabilityFacadeTest {

    @Autowired
    AvailabilityFacade availabilityFacade;

    @Autowired
    ResourceAvailabilityRepository resourceAvailabilityRepository;

    @Test
    void canCreateAvailabilitySlots() {
        //given
        ResourceId resourceId = ResourceId.newOne();
        TimeSlot oneDay = TimeSlot.createDailyTimeSlotAtUTC(2021, 1, 1);

        //when
        availabilityFacade.createResourceSlots(resourceId, oneDay);

        //then
        assertEquals(
                oneDay.blockedBlocksCount(),
                resourceAvailabilityRepository
                        .findAllByResourceIdAndTimeSlotAndResourceStatus(resourceId, oneDay, ResourceStatus.AVAILABLE)
                        .size()
        );
    }

    @Test
    void canBlockAvailabilities() {
        //given
        ResourceId resourceId = ResourceId.newOne();
        TimeSlot oneDay = TimeSlot.createDailyTimeSlotAtUTC(2021, 1, 1);
        Owner owner = Owner.newOne();
        availabilityFacade.createResourceSlots(resourceId, oneDay);

        //when
        boolean result = availabilityFacade.block(resourceId, oneDay, owner);

        //then
        assertTrue(result);
        assertEquals(
                oneDay.blockedBlocksCount(),
                resourceAvailabilityRepository
                        .findAllByResourceIdAndTimeSlotAndResourceStatus(resourceId, oneDay, ResourceStatus.BLOCKED)
                        .size()
        );
    }

    @Test
    void canDisableAvailabilities() {
        //given
        ResourceId resourceId = ResourceId.newOne();
        TimeSlot oneDay = TimeSlot.createDailyTimeSlotAtUTC(2021, 1, 1);
        Owner owner = Owner.newOne();
        availabilityFacade.createResourceSlots(resourceId, oneDay);

        //when
        boolean result = availabilityFacade.disable(resourceId, oneDay, owner);

        //then
        assertTrue(result);
        assertEquals(
                oneDay.blockedBlocksCount(),
                resourceAvailabilityRepository
                        .findAllByResourceIdAndTimeSlotAndResourceStatus(resourceId, oneDay, ResourceStatus.DISABLED)
                        .size()
        );
    }

    @Test
    void cantBlockEvenWhenJustSmallSegmentOfRequestedSlotIsBlocked() {
        //given
        ResourceId resourceId = ResourceId.newOne();
        TimeSlot oneDay = TimeSlot.createDailyTimeSlotAtUTC(2021, 1, 1);
        Owner owner = Owner.newOne();
        availabilityFacade.createResourceSlots(resourceId, oneDay);
        //and
        availabilityFacade.block(resourceId, oneDay, owner);
        TimeSlot fifteenMinutes = new TimeSlot(oneDay.from(), oneDay.from().plus(15, ChronoUnit.MINUTES));

        //when
        boolean result = availabilityFacade.block(resourceId, fifteenMinutes, Owner.newOne());

        //then
        assertFalse(result);
        assertEquals(
                oneDay.blockedBlocksCount(),
                resourceAvailabilityRepository
                        .findAllByResourceIdAndTimeSlotAndResourceStatus(resourceId, oneDay, ResourceStatus.BLOCKED)
                        .size()
        );
    }


    @Test
    void canReleaseAvailability() {
        //given
        ResourceId resourceId = ResourceId.newOne();
        TimeSlot oneDay = TimeSlot.createDailyTimeSlotAtUTC(2021, 1, 1);
        TimeSlot fifteenMinutes = new TimeSlot(oneDay.from(), oneDay.from().plus(15, ChronoUnit.MINUTES));
        Owner owner = Owner.newOne();
        availabilityFacade.createResourceSlots(resourceId, fifteenMinutes);
        //and
        availabilityFacade.block(resourceId, fifteenMinutes, owner);

        //when
        boolean result = availabilityFacade.release(resourceId, oneDay, owner);

        //then
        assertTrue(result);
        assertEquals(
                fifteenMinutes.blockedBlocksCount(),
                resourceAvailabilityRepository
                        .findAllByResourceIdResourceStatus(resourceId, ResourceStatus.AVAILABLE)
                        .stream()
                        .filter(resourceAvailability -> resourceAvailability.timeSlot().within(fifteenMinutes))
                        .count()
        );
    }

    @Test
    void cantReleaseEvenWhenJustPartOfSlotIsOwnedByTheRequester() {
        //given
        ResourceId resourceId = ResourceId.newOne();
        TimeSlot jan_1 = TimeSlot.createDailyTimeSlotAtUTC(2021, 1, 1);
        TimeSlot jan_2 = TimeSlot.createDailyTimeSlotAtUTC(2021, 1, 2);
        TimeSlot jan_1_2 = new TimeSlot(jan_1.from(), jan_2.to());
        Owner jan1owner = Owner.newOne();
        availabilityFacade.createResourceSlots(resourceId, jan_1_2);
        //and
        availabilityFacade.block(resourceId, jan_1, jan1owner);
        //and
        Owner jan2owner = Owner.newOne();
        availabilityFacade.block(resourceId, jan_2, jan2owner);

        //when
        boolean result = availabilityFacade.release(resourceId, jan_1_2, jan1owner);

        //then
        assertFalse(result);
        assertEquals(
                jan_1_2.blockedBlocksCount(),
                resourceAvailabilityRepository
                        .findAllByResourceIdAndTimeSlotAndResourceStatus(resourceId, jan_1_2, ResourceStatus.BLOCKED)
                        .size()
        );
    }


}