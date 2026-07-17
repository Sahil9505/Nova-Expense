package com.nova.finance.budget;

import com.nova.finance.budget.web.dto.BudgetResponse;
import com.nova.finance.budget.web.dto.CategoryRef;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps the {@link Budget} entity to its API response. The {@code period} enum is
 * projected by name; an owned category is reduced to its {@link CategoryRef}; audit
 * timestamps are carried through verbatim.
 */
@Mapper(componentModel = "spring")
public interface BudgetMapper {

    @Mapping(target = "period", source = "period")
    @Mapping(target = "category", expression = "java(mapCategory(budget))")
    BudgetResponse toResponse(Budget budget);

    default CategoryRef mapCategory(Budget budget) {
        return budget.getCategory() != null ? CategoryRef.from(budget.getCategory()) : null;
    }
}
