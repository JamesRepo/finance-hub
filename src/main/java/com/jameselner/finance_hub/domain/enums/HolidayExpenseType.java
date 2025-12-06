package com.jameselner.finance_hub.domain.enums;

import lombok.Getter;

@Getter
public enum HolidayExpenseType {
    FLIGHT("Flight"),
    ACCOMMODATION("Accommodation"),
    TRAIN("Train"),
    BUS("Bus/Coach"),
    CAR_RENTAL("Car Rental"),
    TAXI("Taxi/Uber"),
    BREAKFAST("Breakfast"),
    LUNCH("Lunch"),
    DINNER("Dinner"),
    SNACKS("Snacks/Drinks"),
    LUGGAGE("Luggage"),
    ACTIVITIES("Activities/Tours"),
    SHOPPING("Shopping"),
    INSURANCE("Travel Insurance"),
    VISA("Visa/Documents"),
    PARKING("Parking"),
    FUEL("Fuel"),
    OTHER("Other");

    private final String displayName;

    HolidayExpenseType(final String displayName) {
        this.displayName = displayName;
    }

    /**
     * Check if this expense type is transportation-related
     * @return true if this is a transportation expense
     */
    public boolean isTransportation() {
        return this == FLIGHT || this == TRAIN || this == BUS ||
               this == CAR_RENTAL || this == TAXI || this == FUEL;
    }

    /**
     * Check if this expense type is food-related
     * @return true if this is a food/dining expense
     */
    public boolean isFood() {
        return this == BREAKFAST || this == LUNCH || this == DINNER || this == SNACKS;
    }
}