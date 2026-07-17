package com.nova.finance.goal;

import com.nova.finance.goal.web.dto.GoalContributionResponse;
import com.nova.finance.goal.web.dto.GoalProgress;
import com.nova.finance.goal.web.dto.GoalResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps the {@link Goal} entity (and its contribution rows) to API responses. The
 * derived {@link GoalProgress} is passed in alongside the entity so the response is
 * fully formed in one call; audit timestamps are carried through verbatim. Mirrors the
 * budget mapper's project-the-entity style.
 */
@Mapper(componentModel = "spring")
public interface GoalMapper {

    @Mapping(target = "type", source = "goal.type")
    @Mapping(target = "targetAmount", source = "goal.targetAmount")
    @Mapping(target = "currentAmount", source = "goal.currentAmount")
    @Mapping(target = "active", source = "goal.active")
    @Mapping(target = "paused", source = "goal.paused")
    @Mapping(target = "progress", source = "progress")
    GoalResponse toResponse(Goal goal, GoalProgress progress);

    @Mapping(target = "contributedAt", source = "contribution.contributedAt")
    GoalContributionResponse toContributionResponse(GoalContribution contribution);
}
