package com.mileowl.tracker.data.model

enum class TripPurpose(val label: String, val iconName: String) {
    BUSINESS("Business", "business_center"),
    BETWEEN_OFFICES("Between Offices", "domain"),
    CUSTOMER_VISIT("Customer Visit", "location_on"),
    MEETING("Meeting", "groups"),
    ERRAND_SUPPLIES("Errand/Supplies", "shopping_cart"),
    MEAL_ENTERTAIN("Meal/Entertain", "restaurant"),
    TEMPORARY_SITE("Temporary Site", "construction"),
    AIRPORT_TRAVEL("Airport/Travel", "flight"),
    DELIVERY("Delivery", "local_shipping"),
    PERSONAL("Personal", "home"),
    COMMUTE("Commute", "directions_car"),
    MEDICAL("Medical", "local_hospital"),
    CHARITY("Charity", "volunteer_activism"),
    MOVING("Moving", "local_shipping"),
    OTHER("Other", "more_horiz");

    /** Returns the appropriate TripClassification for this purpose */
    fun toClassification(): TripClassification = when (this) {
        PERSONAL, COMMUTE -> TripClassification.PERSONAL
        else -> TripClassification.BUSINESS
    }
}
