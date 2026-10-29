# Fitness Tracker App — MyFitnessPal-Inspired Clone Specification

> **Purpose:** Build a modern calorie, nutrition, fitness, and habit-tracking mobile app inspired by the overall product architecture and UX patterns of MyFitnessPal.
>
> **Important:** This is an original implementation specification. Do **not** copy MyFitnessPal's proprietary source code, logos, trademarks, illustrations, exact assets, or pixel-for-pixel screens. Use the documented feature concepts and create an original visual identity.

---

# TABLE OF CONTENTS

1. [Product Vision](#1-product-vision)
2. [Top Features](#2-top-features)
3. [App Navigation](#3-app-navigation)
4. [Screen Map](#4-screen-map)
5. [Design System](#5-design-system)
6. [UX Rules](#6-ux-rules)
7. [Core User Flows](#7-core-user-flows)
8. [Food Logging](#8-food-logging)
9. [Nutrition & Macro System](#9-nutrition--macro-system)
10. [Exercise Tracking](#10-exercise-tracking)
11. [Weight & Progress](#11-weight--progress)
12. [Water & Healthy Habits](#12-water--healthy-habits)
13. [Barcode Scanner](#13-barcode-scanner)
14. [Meal Scan / Camera Logging](#14-meal-scan--camera-logging)
15. [Voice Logging](#15-voice-logging)
16. [Recipes & Meals](#16-recipes--meals)
17. [Meal Planner](#17-meal-planner)
18. [Grocery Lists](#18-grocery-lists)
19. [Intermittent Fasting](#19-intermittent-fasting)
20. [Dashboard & Analytics](#20-dashboard--analytics)
21. [Goals & Onboarding](#21-goals--onboarding)
22. [Profile & Settings](#22-profile--settings)
23. [Authentication](#23-authentication)
24. [Data Model](#24-data-model)
25. [API / Backend Architecture](#25-api--backend-architecture)
26. [Offline-First Behavior](#26-offline-first-behavior)
27. [Notifications](#27-notifications)
28. [Subscription / Premium Architecture](#28-subscription--premium-architecture)
29. [Accessibility](#29-accessibility)
30. [Security & Privacy](#30-security--privacy)
31. [Performance Requirements](#31-performance-requirements)
32. [Testing Requirements](#32-testing-requirements)
33. [Implementation Phases](#33-implementation-phases)
34. [Claude.md](#34-claudemd)
35. [Skills.md](#35-skillsmd)
36. [Definition of Done](#36-definition-of-done)

---

# 1. PRODUCT VISION

## Working Name

**NutriTrack** (temporary name)

Use a completely original app name, logo, icons, illustrations, and brand identity.

## Product Goal

Create an all-in-one personal health tracking application where users can:

- Track daily calories
- Track protein, carbohydrates, and fat
- Log meals quickly
- Search a food database
- Scan product barcodes
- Scan meals using the camera
- Log food using voice
- Create recipes
- Save favorite foods
- Track water
- Track exercise
- Track body weight and measurements
- Monitor progress
- Set calorie and macro goals
- Plan meals
- Generate grocery lists
- Track intermittent fasting
- Review nutrition trends
- Build consistent healthy habits

The product should feel:

- Fast
- Clean
- Friendly
- Data-driven
- Mobile-first
- Easy for beginners
- Powerful enough for advanced users

---

# 2. TOP FEATURES

The product should cover the major feature categories associated with modern calorie/nutrition trackers.

## Tier 1 — Core

These are the most important features.

### Daily Tracking

- Calories consumed
- Calories remaining
- Daily calorie goal
- Protein
- Carbohydrates
- Fat
- Breakfast
- Lunch
- Dinner
- Snacks
- Exercise calories
- Water
- Weight

### Food Search

- Search food
- Search by name
- Search by brand
- Search by restaurant
- Recent foods
- Frequent foods
- Favorite foods
- Saved meals
- Saved recipes
- Portion selector
- Unit selector

### Food Logging

- Add food
- Edit food
- Delete food
- Copy food
- Copy meal
- Copy previous meal
- Copy previous day
- Multi-item selection
- Quick add calories
- Quick add macros

### Progress

- Weight chart
- Calorie chart
- Macro chart
- Nutrition trends
- Weekly summary
- Monthly summary
- Goal progress

### Goals

- Lose weight
- Maintain weight
- Gain weight
- Custom calorie target
- Macro percentage targets
- Macro gram targets
- Weekly goals
- Activity level

---

# 3. APP NAVIGATION

Use a 5-tab mobile navigation.

## Bottom Navigation

1. **Today**
2. **Diary**
3. **Progress**
4. **Plans**
5. **Profile**

A persistent floating **+** action should provide rapid logging.

## Quick Add Menu

Tapping `+` opens:

- Food
- Barcode
- Meal Scan
- Voice Log
- Water
- Exercise
- Weight
- Quick Calories
- Quick Macros

The quick action sheet should be one of the fastest parts of the application.

---

# 4. SCREEN MAP

## Onboarding

- Splash
- Welcome
- Create Account
- Login
- Goal Selection
- Gender
- Age
- Height
- Current Weight
- Target Weight
- Activity Level
- Weekly Goal
- Calorie Goal
- Macro Goal
- Water Goal
- Notification Preferences
- Completion

## Main

- Today
- Diary
- Add Food
- Food Search
- Food Details
- Barcode Scanner
- Meal Scanner
- Voice Logger
- Add Exercise
- Exercise Details
- Water
- Weight
- Progress
- Nutrition
- Goals
- Plans
- Meal Planner
- Recipe Builder
- Grocery List
- Fasting
- Profile
- Settings

---

# 5. DESIGN SYSTEM

## Design Direction

Create an original design inspired by modern health and fitness apps.

### Characteristics

- Clean white/light surfaces
- Strong primary brand color
- Rounded cards
- Large numerical metrics
- Circular progress indicators
- Soft dividers
- Compact bottom navigation
- High information density without clutter
- Strong hierarchy
- Large tap targets
- Minimal decorative elements

## Suggested Original Brand

### Primary

`#16A34A`

### Primary Dark

`#15803D`

### Background

`#F7F8F6`

### Surface

`#FFFFFF`

### Text

`#172019`

### Secondary Text

`#66736A`

### Border

`#E4E9E5`

### Success

`#22C55E`

### Warning

`#F59E0B`

### Danger

`#EF4444`

### Protein

`#8B5CF6`

### Carbohydrates

`#F59E0B`

### Fat

`#EF4444`

> These colors are intentionally different from MyFitnessPal branding. Treat them as the starting design system and adjust if a different original brand direction is selected.

---

## Typography

Recommended:

- Inter
- SF Pro
- Roboto

### Hierarchy

Display:
- 32–40px
- Bold

Page title:
- 24–28px
- Bold

Section title:
- 18–20px
- Semibold

Body:
- 15–16px

Caption:
- 12–14px

Numbers:
- Use tabular/monospaced numerals where appropriate.

---

## Radius

- Small: 8px
- Medium: 12px
- Large: 16px
- Card: 18px
- Pill: 999px

---

## Spacing

Base unit: 4px

Common spacing:

- 4
- 8
- 12
- 16
- 20
- 24
- 32

---

# 6. UX RULES

## Rule 1 — Logging must be fast

A user should be able to add a common food in as few interactions as possible.

## Rule 2 — Show useful information immediately

Avoid hiding calorie and macro information behind multiple screens.

## Rule 3 — Make today the center of the product

Today should answer:

> "How am I doing today?"

It should show:

- Calories
- Macros
- Meals
- Water
- Exercise
- Weight
- Streak
- Goal progress

## Rule 4 — Never overload a screen

Use expandable cards and secondary pages for deeper information.

## Rule 5 — Every important action needs feedback

Examples:

- Food added → confirmation
- Water added → progress updates
- Weight saved → chart updates
- Barcode scanned → result preview
- Sync completed → subtle status

---

# 7. CORE USER FLOWS

## Flow A — Add Food

Today
→ Tap +
→ Food
→ Search
→ Select Food
→ Select Serving
→ Select Meal
→ Review Nutrition
→ Add

## Flow B — Barcode

Today
→ +
→ Barcode
→ Camera
→ Scan
→ Food Result
→ Serving
→ Meal
→ Add

## Flow C — Meal Scan

Today
→ +
→ Meal Scan
→ Camera
→ Detect Food
→ Review Suggested Foods
→ Correct Items
→ Portion
→ Add

## Flow D — Weight

Today
→ +
→ Weight
→ Enter Weight
→ Save
→ Update Chart

## Flow E — Exercise

Today
→ +
→ Exercise
→ Search Exercise
→ Duration / Sets / Reps
→ Calories
→ Add

## Flow F — Recipe

Plans
→ Recipes
→ Create Recipe
→ Add Ingredients
→ Set Servings
→ Calculate Nutrition
→ Save

---

# 8. FOOD LOGGING

## Food Database Entity

Each food should contain:

- id
- name
- brand
- barcode
- serving_size
- serving_unit
- calories
- protein
- carbs
- fat
- fiber
- sugar
- sodium
- cholesterol
- saturated_fat
- trans_fat
- micronutrients
- verified
- source
- created_at
- updated_at

## Search

Support:

- Exact match
- Prefix match
- Fuzzy match
- Brand match
- Barcode match

## Search Results

Each result should show:

**Food Name**

Brand / Serving

`250 kcal`

`Protein 20g • Carbs 25g • Fat 8g`

## Recent

Store recent foods locally and remotely.

## Favorites

Allow:

- Favorite
- Unfavorite
- Favorite folders/categories

---

# 9. NUTRITION & MACRO SYSTEM

## Calories

Formula:

`Calories Remaining = Daily Goal + Exercise Calories - Food Calories`

Allow configuration for whether exercise calories contribute to the available calorie budget.

## Macro Targets

Support:

### Percentage

Example:

- Protein 30%
- Carbs 40%
- Fat 30%

### Grams

Example:

- Protein 150g
- Carbs 200g
- Fat 65g

## Macro Card

Display:

- Protein consumed / goal
- Carbs consumed / goal
- Fat consumed / goal

Use circular or horizontal progress visuals.

## Micronutrients

Optional:

- Fiber
- Sugar
- Sodium
- Potassium
- Calcium
- Iron
- Vitamin D
- Vitamin C
- Cholesterol
- Saturated Fat

---

# 10. EXERCISE TRACKING

## Exercise Categories

- Cardio
- Strength
- Sports
- Walking
- Running
- Cycling
- Swimming
- HIIT
- Yoga
- Custom

## Cardio Fields

- Exercise
- Duration
- Intensity
- Calories

## Strength Fields

- Exercise
- Sets
- Reps
- Weight
- Rest time

## Workout Routine

Allow users to create reusable routines.

Example:

**Push Day**

- Bench Press
- Shoulder Press
- Lateral Raise
- Triceps Extension

---

# 11. WEIGHT & PROGRESS

## Weight Entry

Fields:

- Weight
- Date
- Time
- Optional note

## Progress Chart

Support:

- 7 days
- 30 days
- 90 days
- 1 year
- All time

## Metrics

- Starting weight
- Current weight
- Goal weight
- Weight change
- Remaining to goal
- Average weekly change

## Body Measurements

Optional:

- Waist
- Chest
- Hips
- Neck
- Arms
- Thighs

---

# 12. WATER & HEALTHY HABITS

## Water Tracker

Allow:

- Quick +250ml
- +500ml
- Custom amount

Display:

`1.5L / 2.5L`

Use a visually obvious progress indicator.

## Habits

Track:

- Water
- Steps
- Exercise
- Food logging
- Weight logging
- Sleep
- Fasting

## Streak

Track consecutive days where the user completed their chosen logging habit.

---

# 13. BARCODE SCANNER

## UX

Open camera immediately.

Overlay:

- Scan frame
- Flash
- Close
- Manual entry

After scanning:

1. Detect barcode
2. Query local cache
3. Query backend
4. Query external food database if configured
5. Show result
6. Allow correction
7. Log food

## Unknown Barcode

Show:

> "We couldn't find this food."

Actions:

- Create Food
- Scan Again
- Search Manually

## Product Creation

Allow:

- Product name
- Brand
- Barcode
- Serving size
- Calories
- Macros
- Micronutrients

---

# 14. MEAL SCAN / CAMERA LOGGING

This feature should be modular.

## Pipeline

Camera
→ Image Capture
→ Image Processing
→ Food Recognition
→ Candidate Foods
→ User Confirmation
→ Portion Selection
→ Nutrition Calculation
→ Diary Entry

Never automatically commit uncertain recognition.

## Confirmation UI

Example:

**Detected**

- Grilled Chicken
- Rice
- Broccoli

Each item:

`✓ Confirm`

`Edit`

`Remove`

`Change Portion`

---

# 15. VOICE LOGGING

Example:

> "Two eggs, two slices of toast and one banana."

Convert speech into structured candidates:

```text
Food:
Egg
Quantity:
2

Food:
Toast
Quantity:
2 slices

Food:
Banana
Quantity:
1
```

Then show confirmation before logging.

Never silently add uncertain foods.

---

# 16. RECIPES & MEALS

## Recipe Builder

Fields:

- Recipe name
- Description
- Photo
- Ingredients
- Quantity
- Units
- Servings

Automatically calculate:

- Calories
- Protein
- Carbs
- Fat
- Fiber
- Other available nutrients

## Saved Meals

Example:

**Breakfast Combo**

- 2 eggs
- 2 toast
- 1 banana
- Coffee

Allow one-tap logging.

---

# 17. MEAL PLANNER

## Weekly Planner

Display:

Monday
- Breakfast
- Lunch
- Dinner
- Snack

Tuesday
- Breakfast
- Lunch
- Dinner
- Snack

etc.

## Planner Actions

- Add meal
- Swap meal
- Duplicate meal
- Remove meal
- Generate plan
- Log meal
- Generate grocery list

## Planning Inputs

- Calorie goal
- Macro goal
- Dietary preference
- Allergies
- Disliked foods
- Cuisine
- Budget
- Cooking time
- Number of people

---

# 18. GROCERY LISTS

Automatically extract ingredients from planned recipes.

## Grocery List

Categories:

- Produce
- Meat
- Dairy
- Grains
- Pantry
- Frozen
- Other

Features:

- Checkbox
- Quantity
- Unit
- Add item
- Delete item
- Share list
- Clear completed

---

# 19. INTERMITTENT FASTING

## Fast Setup

Presets:

- 12:12
- 14:10
- 16:8
- 18:6
- 20:4
- Custom

## Fasting Screen

Show:

- Start time
- End time
- Time remaining
- Elapsed time
- Current state

Example:

`FASTING`

`14h 22m`

`01h 38m remaining`

## History

- Fast duration
- Start
- End
- Completed / incomplete

---

# 20. DASHBOARD & ANALYTICS

## Today Dashboard

Priority order:

1. Daily calorie summary
2. Macro summary
3. Meals
4. Exercise
5. Water
6. Weight
7. Healthy habits
8. Streak

## Calorie Ring

Center:

`1,420`

Label:

`Calories Remaining`

Under it:

`1,850 Goal`

`+220 Exercise`

`650 Food Remaining`

## Weekly Analytics

Show:

- Average calories
- Average protein
- Average carbs
- Average fat
- Weight trend
- Exercise frequency
- Logging consistency

## Nutrition Page

Tabs:

- Calories
- Macros
- Nutrients
- Meals
- Trends

---

# 21. GOALS & ONBOARDING

## Goal Selection

Options:

- Lose weight
- Maintain
- Gain weight
- Improve nutrition
- Build healthy habits

## Profile Inputs

- Age
- Sex
- Height
- Weight
- Goal weight
- Activity level

## Activity Levels

- Sedentary
- Light
- Moderate
- Very Active
- Extra Active

## Goal Calculation

Use a transparent calculation service.

Do not hard-code nutrition advice into UI components.

Keep formulas in:

`services/nutrition/`

Recommended architecture:

```text
BMR Calculator
        ↓
TDEE Calculator
        ↓
Goal Adjustment
        ↓
Daily Calorie Goal
        ↓
Macro Distribution
```

Include a disclaimer that calorie targets are estimates and users should consult a qualified professional for medical/dietary conditions.

---

# 22. PROFILE & SETTINGS

## Profile

Show:

- Name
- Profile photo
- Goal
- Current weight
- Target weight
- Daily calorie goal
- Macro goal

## Settings

### Account

- Email
- Password
- Delete account
- Export data

### Preferences

- Units
- Language
- Theme
- Start of week

### Nutrition

- Calories
- Macros
- Exercise calories
- Meal goals

### Notifications

- Meal reminders
- Water reminders
- Weigh-in reminders
- Fasting reminders
- Weekly report

### Privacy

- Analytics
- Data sharing
- Connected apps

---

# 23. AUTHENTICATION

Support:

- Email/password
- Google
- Apple
- Guest mode

## Guest Mode

Allow:

- Local diary
- Local food favorites
- Local weight history

Prompt account creation before cloud synchronization.

---

# 24. DATA MODEL

Recommended database: PostgreSQL / Supabase.

## users

```text
id
email
display_name
avatar_url
created_at
updated_at
```

## profiles

```text
id
user_id
age
sex
height
height_unit
current_weight
weight_unit
goal_weight
activity_level
goal_type
daily_calorie_goal
water_goal
created_at
updated_at
```

## macro_goals

```text
id
user_id
protein_grams
carbs_grams
fat_grams
protein_percentage
carbs_percentage
fat_percentage
goal_mode
created_at
updated_at
```

## foods

```text
id
name
brand
barcode
serving_size
serving_unit
calories
protein
carbs
fat
fiber
sugar
sodium
source
verified
created_at
updated_at
```

## food_logs

```text
id
user_id
food_id
meal_type
quantity
serving_size
calories
protein
carbs
fat
fiber
sugar
sodium
logged_at
created_at
```

## meals

```text
id
user_id
name
description
created_at
updated_at
```

## meal_items

```text
id
meal_id
food_id
quantity
serving_size
```

## recipes

```text
id
user_id
name
description
servings
image_url
created_at
updated_at
```

## recipe_items

```text
id
recipe_id
food_id
quantity
unit
```

## exercises

```text
id
name
category
met_value
```

## exercise_logs

```text
id
user_id
exercise_id
duration
sets
reps
weight
calories_burned
logged_at
```

## weight_logs

```text
id
user_id
weight
unit
note
logged_at
```

## water_logs

```text
id
user_id
amount
unit
logged_at
```

## fasting_sessions

```text
id
user_id
start_time
end_time
target_duration
status
created_at
```

## meal_plans

```text
id
user_id
week_start
created_at
updated_at
```

## meal_plan_items

```text
id
meal_plan_id
date
meal_type
recipe_id
```

## grocery_lists

```text
id
user_id
name
created_at
updated_at
```

## grocery_items

```text
id
grocery_list_id
name
quantity
unit
category
completed
```

---

# 25. API / BACKEND ARCHITECTURE

Use a service-oriented frontend architecture.

```text
src/
├── app/
├── components/
├── features/
│   ├── auth/
│   ├── diary/
│   ├── foods/
│   ├── barcode/
│   ├── meal-scan/
│   ├── voice-log/
│   ├── exercise/
│   ├── weight/
│   ├── water/
│   ├── progress/
│   ├── recipes/
│   ├── meal-planner/
│   ├── grocery/
│   ├── fasting/
│   └── profile/
├── services/
│   ├── api/
│   ├── nutrition/
│   ├── barcode/
│   ├── recognition/
│   └── notifications/
├── store/
├── hooks/
├── utils/
├── types/
└── theme/
```

## API Categories

```text
/auth
/profile
/goals
/foods
/food-search
/food-logs
/meals
/recipes
/exercises
/exercise-logs
/weight
/water
/progress
/fasting
/meal-plans
/grocery
/notifications
```

---

# 26. OFFLINE-FIRST BEHAVIOR

The diary must work when internet is unavailable.

## Local Data

Cache:

- Today's diary
- Recent foods
- Favorite foods
- Saved meals
- User goals
- Water
- Weight
- Exercise

## Sync Queue

When offline:

```text
Local Action
    ↓
Local Database
    ↓
Sync Queue
    ↓
Internet Available
    ↓
API Sync
    ↓
Mark Synced
```

Every mutation should have:

- local_id
- server_id
- operation
- created_at
- sync_status

---

# 27. NOTIFICATIONS

Examples:

### Meal

"Don't forget to log lunch."

### Water

"You're 500ml away from today's water goal."

### Weight

"Ready for your weekly weigh-in?"

### Fasting

"Your fasting window starts in 30 minutes."

### Progress

"You've completed 7 days of consistent logging."

Notifications must be configurable.

---

# 28. SUBSCRIPTION / PREMIUM ARCHITECTURE

Build entitlement logic even if subscriptions are added later.

## Free

Potentially include:

- Food search
- Diary
- Calories
- Basic macros
- Exercise
- Weight
- Water
- Basic progress
- Recipes

## Premium

Potentially include:

- Barcode scanning
- Meal scanning
- Voice logging
- Advanced analytics
- Advanced macro controls
- Fasting
- Custom goals
- Advanced reports
- Ad-free experience

## Premium+

Potentially include:

- Meal planning
- Automated grocery lists
- Advanced recipe planning
- Budget-aware meal planning

Do not hard-code subscription checks throughout UI.

Use:

```text
EntitlementService
```

Example:

```ts
can("BARCODE_SCAN")
can("ADVANCED_ANALYTICS")
can("MEAL_PLANNER")
```

---

# 29. ACCESSIBILITY

Requirements:

- WCAG-conscious contrast
- Minimum 44x44px touch targets
- Dynamic font sizing
- Screen reader labels
- No color-only indicators
- Accessible charts
- Accessible scanner controls
- Reduced-motion option

---

# 30. SECURITY & PRIVACY

Health/nutrition data should be treated as sensitive personal data.

Requirements:

- Secure authentication
- Row-level security
- Encrypted transport
- No health data in logs
- No API keys in client code
- Secure token storage
- User data export
- User account deletion
- Minimal analytics collection

Never expose:

- Passwords
- Auth tokens
- Private nutrition records
- Private weight history

---

# 31. PERFORMANCE REQUIREMENTS

Targets:

- App launch: < 2.5 seconds on a normal device
- Today screen: < 500ms from local cache
- Food search: responsive while typing
- Barcode result: show loading state immediately
- Charts: render smoothly
- Offline diary: instant interaction
- Avoid unnecessary full-screen re-renders

Use pagination for large food datasets.

---

# 32. TESTING REQUIREMENTS

## Unit Tests

Test:

- BMR
- TDEE
- Calories
- Macro calculations
- Serving conversions
- Recipe nutrition
- Goal calculations
- Water totals
- Fasting durations

## Integration Tests

Test:

- Authentication
- Food logging
- Food search
- Barcode
- Recipe creation
- Exercise logging
- Weight logging
- Sync

## E2E

Test:

```text
Signup
→ Goal setup
→ Add breakfast
→ Add lunch
→ Add exercise
→ Add water
→ Add weight
→ View progress
```

---

# 33. IMPLEMENTATION PHASES

## Phase 1 — Foundation

Build:

- Project
- Theme
- Navigation
- Auth
- Database
- Profile
- Onboarding

## Phase 2 — Core Diary

Build:

- Today
- Diary
- Food search
- Food details
- Food logging
- Calories
- Macros

## Phase 3 — Progress

Build:

- Weight
- Charts
- Nutrition analytics
- Goals
- Water

## Phase 4 — Advanced Logging

Build:

- Barcode
- Quick Add
- Saved meals
- Recipes
- Exercise routines

## Phase 5 — Smart Logging

Build:

- Meal Scan
- Voice Logging

Keep these as replaceable services so recognition providers can be changed.

## Phase 6 — Planning

Build:

- Meal Planner
- Recipes
- Grocery Lists
- Fasting

## Phase 7 — Premium

Build:

- Entitlements
- Subscription UI
- Premium feature gates

## Phase 8 — Production

- Testing
- Performance
- Security
- Analytics
- Crash reporting
- Store preparation

---

# 34. CLAUDE.MD

# Claude Code Instructions

## Project Identity

You are building an original mobile nutrition and fitness tracker inspired by established calorie-tracking apps.

Do not reproduce proprietary source code, copyrighted assets, logos, trademarks, or pixel-perfect copies of another company's application.

Use the feature specification in this document as the source of truth.

---

## Development Principles

### 1. Build in small vertical slices

Never create 30 unfinished screens at once.

Preferred sequence:

```text
Navigation
→ Today
→ Food Search
→ Food Details
→ Food Logging
→ Nutrition Summary
→ Progress
```

Each slice should be functional before starting the next.

### 2. Keep business logic out of UI

Bad:

```tsx
function Today() {
  const calories = ...
  const protein = ...
}
```

Preferred:

```ts
const summary = nutritionService.calculateDailySummary(...)
```

### 3. Reusable components

Create reusable components:

```text
CalorieRing
MacroCard
FoodRow
MealSection
ProgressBar
MetricCard
EmptyState
LoadingState
ErrorState
PrimaryButton
BottomSheet
SearchInput
```

### 4. Type everything

Avoid:

```ts
any
```

Use explicit types for:

- Food
- FoodLog
- Meal
- Recipe
- Exercise
- WeightLog
- WaterLog
- UserProfile
- Goals

### 5. Database-first thinking

Before building a complex feature:

1. Define data model
2. Define API/service
3. Define state
4. Build UI
5. Add tests

### 6. Never lose user data

Any destructive operation must require confirmation.

### 7. Optimistic UI

Food logging should feel immediate.

```text
Tap Add
→ Update local UI
→ Save locally
→ Sync remotely
```

### 8. Offline support

The app must remain useful without internet.

---

## Code Quality

Every feature should have:

```text
components/
hooks/
services/
types/
utils/
```

where appropriate.

Avoid huge files.

Preferred component size:

- 50–250 lines
- Extract when complexity increases

---

## Error Handling

Every API operation needs:

- loading
- success
- empty
- error
- retry

Do not show raw backend errors to users.

---

## UI Rules

Use the design system defined in this document.

Do not randomly introduce colors.

Do not randomly introduce border radii.

Do not create inconsistent button styles.

---

## Navigation Rules

Use centralized route definitions.

Never hard-code navigation strings across dozens of components.

---

## State Management

Separate:

### Server state

- Foods
- User profile
- Diary
- Recipes
- Progress

### Local UI state

- Search text
- Modal visibility
- Selected tab
- Temporary form values

### Persistent local state

- Offline diary
- Recent foods
- Preferences
- Sync queue

---

## Before Writing Code

For every major feature, Claude should first output:

```text
Feature:
Goal:
Files to create:
Files to modify:
Database changes:
API/service changes:
Testing plan:
```

Then implement.

---

## After Writing Code

Run:

```text
typecheck
lint
tests
build
```

Fix errors before moving forward.

---

## Do Not

- Replace working code unnecessarily
- Rewrite the entire project for one bug
- Add dependencies without reason
- Put secrets in source code
- Put API keys in mobile client code
- Ignore TypeScript errors
- Ignore failed tests
- Create fake backend success states
- Automatically log uncertain AI food recognition

---

# 35. SKILLS.MD

# Development Skills

This project should be developed using modular skills.

---

## Skill 01 — UI / Design System

Responsibilities:

- Build reusable UI
- Maintain spacing
- Maintain typography
- Maintain colors
- Maintain component consistency
- Build responsive mobile layouts

Components:

```text
CalorieRing
MacroCard
MealCard
FoodRow
MetricCard
ProgressChart
BottomSheet
SearchBar
PrimaryButton
SecondaryButton
```

---

## Skill 02 — Nutrition Engine

Responsibilities:

- BMR
- TDEE
- Calorie goals
- Macro goals
- Serving conversions
- Recipe calculations
- Daily summaries

Must be pure/testable functions.

---

## Skill 03 — Food Database

Responsibilities:

- Search
- Filtering
- Favorites
- Recent foods
- Barcode lookup
- Food creation
- Food editing

---

## Skill 04 — Diary

Responsibilities:

- Breakfast
- Lunch
- Dinner
- Snacks
- Food logs
- Copy meals
- Copy days
- Edit logs
- Delete logs

---

## Skill 05 — Barcode

Responsibilities:

- Camera permissions
- Barcode detection
- Lookup
- Unknown product flow
- Manual food creation

---

## Skill 06 — Camera Food Recognition

Responsibilities:

- Camera
- Image capture
- Recognition provider
- Candidate results
- User confirmation
- Portion selection

Recognition must never be trusted blindly.

---

## Skill 07 — Voice Logging

Responsibilities:

- Microphone permission
- Speech recognition
- Parse food entities
- Show confirmation
- Create logs

---

## Skill 08 — Progress

Responsibilities:

- Weight
- Measurements
- Charts
- Trends
- Goal progress
- Weekly summaries

---

## Skill 09 — Exercise

Responsibilities:

- Exercise database
- Cardio
- Strength
- Workouts
- Exercise history
- Calories burned

---

## Skill 10 — Meal Planning

Responsibilities:

- Weekly meal plan
- Meal swaps
- Recipe assignment
- Goal matching
- Grocery extraction

---

## Skill 11 — Grocery

Responsibilities:

- Grocery lists
- Ingredient aggregation
- Categories
- Check-off
- Sharing

---

## Skill 12 — Fasting

Responsibilities:

- Fasting timer
- Fasting history
- Presets
- Notifications

---

## Skill 13 — Offline Sync

Responsibilities:

- Local persistence
- Queue
- Conflict handling
- Retry
- Background sync

---

## Skill 14 — Authentication

Responsibilities:

- Login
- Signup
- OAuth
- Guest mode
- Password reset
- Session management

---

## Skill 15 — Security

Responsibilities:

- RLS
- Authentication
- Secure storage
- API security
- Data deletion
- Data export

---

## Skill 16 — Testing

Responsibilities:

- Unit tests
- Integration tests
- E2E
- Regression tests
- Database tests

---

# 36. DEFINITION OF DONE

A feature is not complete when the screen exists.

A feature is complete when:

- UI works
- Loading works
- Empty state works
- Error state works
- Data persists
- Offline behavior is considered
- Validation exists
- Types are correct
- Tests exist
- Accessibility is considered
- Navigation works
- Back navigation works
- No console errors
- No TypeScript errors
- No obvious performance issues

---

# RECOMMENDED FIRST BUILD

If development needs to start immediately, build only this first:

```text
1. Splash
2. Onboarding
3. Login
4. Today
5. Food Search
6. Food Details
7. Add Food
8. Diary
9. Macro Summary
10. Weight
11. Progress
12. Water
13. Exercise
14. Profile
```

Then add:

```text
15. Barcode
16. Recipes
17. Saved Meals
18. Fasting
19. Meal Scan
20. Voice Log
21. Meal Planner
22. Grocery
23. Premium
```

This creates a usable MVP before advanced AI/camera functionality.

---

# REFERENCE FEATURE NOTES

The current MyFitnessPal product publicly documents capabilities including food/exercise logging, calorie and macro tracking, a large food database, barcode scanning, Meal Scan, Voice Log, nutrition insights, intermittent fasting, custom goals, progress tracking, meal planning, recipes, and grocery-related functionality. Its current "Today" experience emphasizes diary logging, calories, macros, meals, water, exercise, steps, and streaks.

Use those concepts as functional inspiration, but implement the product with an original name, original design system, original components, and original code.

---

# FINAL BUILD PHILOSOPHY

The app should feel like:

> **"Open the app → immediately understand today's nutrition → log something in seconds → see progress → know what to do next."**

The most important experience is not the number of features.

It is the speed and clarity of the daily tracking loop:

```text
OPEN
  ↓
TODAY
  ↓
SEE CALORIES + MACROS
  ↓
LOG FOOD
  ↓
SEE UPDATED PROGRESS
  ↓
LOG WATER / EXERCISE
  ↓
CHECK GOAL
  ↓
RETURN TOMORROW
```

Everything else supports this loop.
