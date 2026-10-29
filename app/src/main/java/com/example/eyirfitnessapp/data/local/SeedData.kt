package com.example.eyirfitnessapp.data.local

/** Original seed data for offline food search.
 *  Per-item foods use servingSize = 1 in their own unit (1 medium, 1 bar, 1 slice…);
 *  weighable foods use servingSize = 100 with unit g.
 *  Egg carries USDA size options (Small 44 g … X-Large 63 g) with the base
 *  row documented as large (56 g = 72 kcal); its gram weight feeds the size chips. */
object SeedData {

    val foods = listOf(
        FoodEntity(0, "Banana", null, null, 1.0, "medium", 105.0, 1.3, 27.0, 0.4, 3.1, 14.0, 1.0, true, "seed"),
        FoodEntity(0, "Apple", null, null, 1.0, "medium", 95.0, 0.5, 25.0, 0.3, 4.4, 19.0, 2.0, true, "seed"),
        FoodEntity(0, "Orange", null, null, 1.0, "medium", 62.0, 1.2, 15.4, 0.2, 3.1, 12.0, 0.0, true, "seed"),
        FoodEntity(
            0, "Egg", null, null, 1.0, "large", 72.0, 6.3, 0.4, 4.8, 0.0, 0.2, 62.0, true, "seed",
            gramsPerServing = 56.0,
            sizes = "Small=44,Medium=50,Large=56,X-Large=63"
        ),
        FoodEntity(0, "Chicken Breast", null, null, 100.0, "g", 165.0, 31.0, 0.0, 3.6, 0.0, 0.0, 74.0, true, "seed"),
        FoodEntity(0, "Salmon Fillet", null, null, 100.0, "g", 208.0, 20.0, 0.0, 13.0, 0.0, 0.0, 59.0, true, "seed"),
        FoodEntity(0, "White Rice", null, null, 100.0, "g", 130.0, 2.7, 28.0, 0.3, 0.4, 0.1, 1.0, true, "seed"),
        FoodEntity(0, "Brown Rice", null, null, 100.0, "g", 123.0, 2.7, 26.0, 1.0, 1.6, 0.4, 4.0, true, "seed"),
        FoodEntity(0, "Whole Wheat Bread", null, null, 1.0, "slice", 82.0, 4.0, 14.0, 1.1, 2.2, 1.4, 134.0, true, "seed"),
        FoodEntity(0, "White Bread", null, null, 1.0, "slice", 79.0, 2.7, 15.0, 1.0, 0.8, 1.6, 140.0, true, "seed"),
        FoodEntity(0, "Rolled Oats", null, null, 1.0, "serving", 152.0, 5.3, 27.0, 2.6, 4.0, 0.5, 2.0, true, "seed"),
        FoodEntity(0, "Greek Yogurt", null, null, 1.0, "container", 100.0, 17.3, 6.0, 0.7, 0.0, 6.0, 65.0, true, "seed"),
        FoodEntity(0, "Whole Milk", null, null, 1.0, "cup", 149.0, 7.7, 11.7, 8.0, 0.0, 12.3, 105.0, true, "seed"),
        FoodEntity(0, "Cheddar Cheese", null, null, 1.0, "serving", 115.0, 7.0, 0.4, 9.4, 0.0, 0.2, 174.0, true, "seed"),
        FoodEntity(0, "Pasta", null, null, 100.0, "g", 158.0, 5.8, 31.0, 0.9, 1.6, 0.6, 3.0, true, "seed"),
        FoodEntity(0, "Ground Beef 90%", null, null, 100.0, "g", 176.0, 20.0, 0.0, 10.0, 0.0, 0.0, 66.0, true, "seed"),
        FoodEntity(0, "Peanut Butter", null, null, 1.0, "tbsp", 188.0, 8.0, 6.4, 16.0, 1.8, 3.0, 140.0, true, "seed"),
        FoodEntity(0, "Almonds", null, null, 1.0, "serving", 164.0, 6.0, 6.1, 14.2, 3.8, 1.2, 0.0, true, "seed"),
        FoodEntity(0, "Avocado", null, null, 100.0, "g", 160.0, 2.0, 8.5, 14.7, 6.7, 0.7, 7.0, true, "seed"),
        FoodEntity(0, "Broccoli", null, null, 100.0, "g", 34.0, 2.8, 6.6, 0.4, 2.6, 1.7, 33.0, true, "seed"),
        FoodEntity(0, "Sweet Potato", null, null, 1.0, "medium", 112.0, 2.0, 26.0, 0.1, 3.9, 5.4, 66.0, true, "seed"),
        FoodEntity(0, "Potato", null, null, 1.0, "medium", 161.0, 4.3, 37.0, 0.2, 3.8, 2.2, 17.0, true, "seed"),
        FoodEntity(0, "Lentils", null, null, 100.0, "g", 116.0, 9.4, 20.0, 0.4, 7.9, 1.8, 2.0, true, "seed"),
        FoodEntity(0, "Black Beans", null, null, 100.0, "g", 132.0, 8.9, 24.0, 0.5, 8.7, 0.3, 1.0, true, "seed"),
        FoodEntity(0, "Tofu", null, null, 100.0, "g", 76.0, 8.0, 1.9, 4.8, 0.3, 0.6, 7.0, true, "seed"),
        FoodEntity(0, "Olive Oil", null, null, 1.0, "tbsp", 119.0, 0.0, 0.0, 13.5, 0.0, 0.0, 0.0, true, "seed"),
        FoodEntity(0, "Butter", null, null, 1.0, "tbsp", 102.0, 0.1, 0.0, 11.5, 0.0, 0.1, 81.0, true, "seed"),
        FoodEntity(0, "Cheddar Crackers", "Snack Co", "1000000000015", 1.0, "serving", 142.0, 3.0, 18.0, 7.0, 1.0, 2.0, 210.0, false, "seed"),
        FoodEntity(0, "Protein Bar", "Snack Co", "1000000000022", 1.0, "bar", 210.0, 20.0, 22.0, 7.0, 5.0, 5.0, 160.0, false, "seed"),
        FoodEntity(0, "Granola Bar", "Snack Co", "1000000000039", 1.0, "bar", 160.0, 3.0, 26.0, 6.0, 2.0, 8.0, 105.0, false, "seed"),
        FoodEntity(0, "Corn Flakes Cereal", "Morning Co", "1000000000046", 1.0, "serving", 110.0, 2.0, 25.0, 0.2, 0.6, 2.4, 190.0, false, "seed"),
        FoodEntity(0, "Sparkling Water", "Fresh Drinks", "1000000000053", 1.0, "can", 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, true, "seed"),
        FoodEntity(0, "Cola", "Fresh Drinks", "1000000000060", 1.0, "can", 139.0, 0.0, 35.0, 0.0, 0.0, 35.0, 10.0, false, "seed"),
        FoodEntity(0, "Black Coffee", null, null, 1.0, "cup", 2.0, 0.3, 0.0, 0.0, 0.0, 0.0, 2.0, true, "seed"),
        FoodEntity(0, "Banana Smoothie", "Smoothie Bar", "1000000000077", 1.0, "serving", 210.0, 5.0, 42.0, 2.0, 4.0, 28.0, 45.0, false, "seed")
    )

    // MET values: compendium of physical activities (Ainsworth et al.)
    private val coreExercises = listOf(
        ExerciseEntity(0, "Walking (moderate)", "Walking", 3.5),
        ExerciseEntity(0, "Walking (brisk)", "Walking", 5.0),
        ExerciseEntity(0, "Running (8 km/h)", "Running", 8.3),
        ExerciseEntity(0, "Running (10 km/h)", "Running", 9.8),
        ExerciseEntity(0, "Running (12 km/h)", "Running", 11.8),
        ExerciseEntity(0, "Cycling (light)", "Cycling", 6.8),
        ExerciseEntity(0, "Cycling (moderate)", "Cycling", 8.0),
        ExerciseEntity(0, "Swimming (freestyle)", "Swimming", 8.3),
        ExerciseEntity(0, "HIIT", "HIIT", 9.0),
        ExerciseEntity(0, "Yoga", "Yoga", 3.0),
        ExerciseEntity(0, "Weight Training", "Strength", 5.0),
        ExerciseEntity(0, "Bench Press", "Strength", 5.0),
        ExerciseEntity(0, "Squats", "Strength", 5.5),
        ExerciseEntity(0, "Deadlift", "Strength", 6.0),
        ExerciseEntity(0, "Football", "Sports", 7.0),
        ExerciseEntity(0, "Basketball", "Sports", 6.5),
        ExerciseEntity(0, "Tennis", "Sports", 7.3)
    )

    /**
     * Full exercise list seeded into the app: the original core entries plus
     * the expanded [ExerciseCatalog] (home, gym, machines, cardio…). The
     * repository merges these into the existing table by name, so upgraded
     * installs gain the catalog without duplicating anything already there.
     */
    val exercises: List<ExerciseEntity> =
        (coreExercises + ExerciseCatalog.exercises)
            .distinctBy { it.name.trim().lowercase() }
}

