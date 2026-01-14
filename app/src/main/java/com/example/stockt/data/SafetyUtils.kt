package com.example.stockt.data

enum class SafetyStatus { SAFE, WARNING, UNKNOWN}

object SafetyUtils {

    fun checkSafety(item: Item, prefs: UserPreferences): SafetyStatus {
        val analysisList = item.analysisTags?.split(",") ?: emptyList()
        val allergenList = item.allergenTags?.split(",") ?: emptyList()

        // 1. CHECK DIET
        if (prefs.isVegetarian && !containsTag(analysisList, "en:vegetarian")) return SafetyStatus.WARNING
        if (prefs.isVegan && !containsTag(analysisList, "en:vegan")) return SafetyStatus.WARNING
        if (prefs.isPalmOilFree && containsTag(analysisList, "en:palm-oil")) return SafetyStatus.WARNING

        // 2. CHECK ALLERGENS
        if (prefs.avoidGluten && hasAllergen(allergenList, "gluten")) return SafetyStatus.WARNING
        if (prefs.avoidMilk && hasAllergen(allergenList, "milk")) return SafetyStatus.WARNING
        if (prefs.avoidEggs && hasAllergen(allergenList, "egg")) return SafetyStatus.WARNING
        if (prefs.avoidNuts && (hasAllergen(allergenList, "nut") || hasAllergen(allergenList, "hazelnut") || hasAllergen(allergenList, "cashew"))) return SafetyStatus.WARNING
        if (prefs.avoidPeanuts && hasAllergen(allergenList, "peanut")) return SafetyStatus.WARNING
        if (prefs.avoidSoy && hasAllergen(allergenList, "soy")) return SafetyStatus.WARNING
        if (prefs.avoidFish && hasAllergen(allergenList, "fish")) return SafetyStatus.WARNING

        return SafetyStatus.SAFE
    }

    private fun containsTag(list: List<String>, tag: String): Boolean {
        return list.any { it.contains(tag, ignoreCase = true) }
    }

    private fun hasAllergen(list: List<String>, keyword: String): Boolean {
        return list.any { it.contains(keyword, ignoreCase = true) }
    }
}