package domaindrivers.smartschedule.risk;

import domaindrivers.smartschedule.allocation.*;
import domaindrivers.smartschedule.allocation.cashflow.Earnings;
import domaindrivers.smartschedule.allocation.cashflow.EarningsRecalculated;
import domaindrivers.smartschedule.availability.ResourceTakenOver;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import org.hibernate.annotations.Type;

import java.time.Duration;
import java.time.Instant;


@Entity(name = "project_risk_sagas")
class RiskPeriodicCheckSaga {

    static final Earnings RISK_THRESHOLD_VALUE = Earnings.of(1000);
    static final int UPCOMING_DEADLINE_AVAILABILITY_SEARCH = 30;
    static final int UPCOMING_DEADLINE_REPLACEMENT_SUGGESTION = 15;

    @EmbeddedId
    private RiskPeriodicCheckSagaId riskSagaId;

    @Embedded
    private ProjectAllocationsId projectId;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private Demands missingDemands;

    @Embedded
    private Earnings earnings;

    @Version
    private int version;

    private Instant deadline;

    RiskPeriodicCheckSaga(ProjectAllocationsId projectId, Demands missingDemands) {
        this.riskSagaId = RiskPeriodicCheckSagaId.newOne();
        this.projectId = projectId;
        this.missingDemands = missingDemands;
    }

    RiskPeriodicCheckSaga(ProjectAllocationsId projectId, Earnings earnings) {
        this.riskSagaId = RiskPeriodicCheckSagaId.newOne();
        this.projectId = projectId;
        this.missingDemands = Demands.none();
        this.earnings = earnings;
    }

    public RiskPeriodicCheckSaga() {
    }

    boolean areDemandsSatisfied() {
        return this.missingDemands.equals(Demands.none());
    }

    Demands missingDemands() {
        return missingDemands;
    }

    RiskPeriodicCheckSagaStep handle(EarningsRecalculated event) {
        this.earnings = event.earnings();
        return RiskPeriodicCheckSagaStep.DO_NOTHING;
    }

    RiskPeriodicCheckSagaStep handle(ProjectAllocationsDemandsScheduled event) {
        this.missingDemands = event.missingDemands();

        if (areDemandsSatisfied()) {
            return RiskPeriodicCheckSagaStep.NOTIFY_ABOUT_DEMANDS_SATISFIED;
        }
        return RiskPeriodicCheckSagaStep.DO_NOTHING;
    }

    RiskPeriodicCheckSagaStep handle(ProjectAllocationScheduled event) {
        this.deadline = event.fromTo().to();
        return null;
    }

    RiskPeriodicCheckSagaStep handle(ResourceTakenOver event) {
        if (event.occurredAt().isAfter(deadline)) {
            return RiskPeriodicCheckSagaStep.DO_NOTHING;
        }
        return RiskPeriodicCheckSagaStep.NOTIFY_ABOUT_POSSIBLE_RISK;
    }

    RiskPeriodicCheckSagaStep handle(CapabilityReleased event) {
        return RiskPeriodicCheckSagaStep.DO_NOTHING;
    }

    RiskPeriodicCheckSagaStep handle(CapabilitiesAllocated event) {
        this.missingDemands = event.missingDemands();

        if (this.missingDemands.equals(Demands.none())) {
            return RiskPeriodicCheckSagaStep.NOTIFY_ABOUT_DEMANDS_SATISFIED;
        }
        return RiskPeriodicCheckSagaStep.DO_NOTHING;
    }

    RiskPeriodicCheckSagaStep handleWeeklyCheck(Instant when) {
        if (deadline == null || when.isAfter(deadline) || areDemandsSatisfied()) {
            return RiskPeriodicCheckSagaStep.DO_NOTHING;
        }

        if (isDeadlineClose(when) && earnings.greaterThan(RISK_THRESHOLD_VALUE)) {
            return RiskPeriodicCheckSagaStep.SUGGEST_REPLACEMENT;
        }

        if (isDeadlineUpcoming(when)) {
            return RiskPeriodicCheckSagaStep.FIND_AVAILABLE;
        }

        return RiskPeriodicCheckSagaStep.DO_NOTHING;
    }

    private boolean isDeadlineUpcoming(Instant when) {
        return deadline != null && when.isAfter(deadline.minus(Duration.ofDays(UPCOMING_DEADLINE_AVAILABILITY_SEARCH)));
    }

    private boolean isDeadlineClose(Instant when) {
        return deadline != null
                && when.isAfter(deadline.minus(Duration.ofDays(UPCOMING_DEADLINE_REPLACEMENT_SUGGESTION)));
    }

    ProjectAllocationsId projectId() {
        return projectId;
    }

    Earnings earnings() {
        return earnings;
    }

    Instant deadline() {
        return deadline;
    }

}
