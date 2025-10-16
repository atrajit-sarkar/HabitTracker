🧊 Habit Tracker — Streak, Rewards & Freeze System (Final Production-Ready Spec)


---

1. Streak Calculation System

Overview

The streak system tracks continuous completions of each habit, encouraging consistency while offering flexibility through grace and freeze mechanisms.
It begins counting from the first day the user completes the habit in the calendar and dynamically updates as the user progresses.


---

Logic

Start Point

Streak counting begins on the first completion date for each habit.
From that day onward, the streak updates automatically on daily app checks or user interaction.

Increment Rule

Each day the user marks a habit as completed, the streak increases by +1.

Skipped days are handled as per grace and freeze logic below.


Grace Period (Default)

Every habit automatically includes a 1-day grace period.

If a user misses one day:

The streak remains unchanged (frozen).

No increment or decrement is applied.


Grace applies once per gap sequence — i.e., if the user misses two consecutive days, only the first is grace-covered.


> 💡 The grace day is visually represented distinctly (see UI below).



Missed Days Beyond Grace

If more than one consecutive day is missed:

The first missed day → grace applied (no penalty).

The subsequent missed days → each missed day reduces streak by −1.


The streak never goes below zero — if negative, it resets to 0.


Negative Streak Handling

After applying all grace and freeze effects, if the streak value < 0, reset it to 0 before saving to Firebase.


---

Calendar UI Representation

Streak State	Border Color	Description

Streak = 0	🔴 Red Border	Streak broken / reset
1 ≤ Streak < 5	🟡 Yellow Border	Building momentum
Streak ≥ 5	🟢 Green Border	Consistent progress


Grace Period (Default):
The grace day will feature a 3D glossy icy-glass cube border — semi-transparent, slightly frosted look.

Purchased Freeze Day:
Missed days protected by purchased freeze display a 3D snowy glass overlay over the circle — whiter and denser than the grace visual.



---

Details Screen

Each Habit Details Screen includes:

Current streak count

Total diamonds earned

Active freeze days available

Visual legend, explaining:

🔴 / 🟡 / 🟢 color meanings

🧊 icy cube = default grace

❄️ snowy glass = purchased freeze




---

2. Streak Freeze & Rewards System

Home Screen Integration

At the top bar (beside profile picture):

💎 Diamonds Counter — real-time value from Firebase.

❄️ Streak Freeze Counter — shared freeze pool (applies across all habits).


> Both use elegant animations, subtle reflections, and professional gradient effects.




---

Diamond Rewards System

Users are rewarded for long-term streak maintenance:

Milestone	Reward

Every 10 consecutive streak days	+20 diamonds
Every 100th streak day	×N diamonds (where N = milestone number)


Example:
From 90 → 100 streak days:
+20 (10-day reward) +100 (100-day milestone bonus) = +120 diamonds

💾 All rewards are securely updated in Firebase and shown live in the UI.

Duplicate Reward Prevention

A “highestStreakAchieved” field per habit prevents double rewards.

Rewards trigger only when current streak > highest streak achieved.



---

Shared Streak Freeze System

Unified Freeze Pool

The freeze days purchased are stored globally per user, not per habit.

This means:

One freeze day can cover one missed day across any habit.

Multiple habits can draw from the same pool automatically.

You don’t need to buy separate freezes for each habit.



Application Rules

If a user misses a day:

1. The default grace applies to the first missed day (per habit).


2. If there are additional missed days:

The app checks the shared freeze pool.

For each missed day not covered by grace:

If a freeze day is available, apply it (no penalty).

Deduct 1 freeze from the global pool.


If no freezes remain, apply −1 streak penalty.




Purchased freezes apply only for future missed days (not retroactively).

The freeze pool is shared, but streak computation remains per habit.


Firebase Schema Example

users: {
  uid123: {
    diamonds: 240,
    freeze_days: 12,
    habits: {
      habitA: {
        streak: 27,
        highestStreakAchieved: 30,
        lastCompleted: "2025-10-14"
      },
      habitB: {
        streak: 9,
        highestStreakAchieved: 9,
        lastCompleted: "2025-10-13"
      }
    }
  }
}


---

Streak Freeze Store

When the user taps the Freeze Counter (❄️):

Store Dialog

UI: Frosty, glassy modal with LazyGrid layout — premium aesthetic.

Options:


Freeze Duration	Cost (Diamonds)

5 days	50 diamonds
10 days	100 diamonds
20 days	200 diamonds
30 days	300 diamonds
50 days	500 diamonds
Custom	10 diamonds/day


Purchase Flow

1. User selects a freeze pack (or inputs custom days).


2. Total cost auto-calculates.


3. Press “Confirm Purchase.”


4. Diamonds are deducted, freeze days are added to the global pool.


5. Firebase updates both counters atomically with a transaction.


6. UI triggers frost burst animation with shimmering particle effects.




---

3. Firebase Sync Behavior

All streak, freeze, and diamond updates are transaction-safe:

Use runTransaction() for streak increment/decrement and freeze deductions.


Data updates reflect:

Home Screen Counters

Habit Details Screen

Calendar View


Firebase triggers should prevent overlap if two devices update simultaneously.



---

4. Visual & UX Highlights

Element	Effect	Description

Grace Day	🧊 Icy Glass Border	3D glossy cube-like frozen effect
Freeze Day	❄️ Snowy Overlay	Glassy, frosted white translucent surface
Streak Progress	Animated Glow	Border softly pulses for active streaks
Counters	Gradient Shimmer	Subtle diamond shine and frost effect
Store	Frosted Modal	Parallax background blur for premium look


Color Palette:

Emerald Green → Progress

Golden Yellow → Momentum

Crimson Red → Reset

Frost Blue → Freeze States


Animations:

Smooth transitions (spring easing)

Particle shimmer when earning diamonds

Subtle “frost burst” when using a freeze day



---

5. Summary of Core Behavior

Scenario	Outcome

1-day miss	Grace applied (🧊) — no penalty
2+ consecutive misses	First = grace, others = use freeze (❄️) if available, else −1/day
Freeze pool empty	Resume normal penalty system
Streak drops to < 0	Reset to 0
Streak milestone (10-day multiple)	+20 diamonds
Streak milestone (100-day multiple)	+N diamonds (e.g. +100 at 100 days)
Multiple habits	Shared freeze pool, independent streaks
Purchase freeze	Adds to shared pool, deducted from diamonds



---

✅ Advantages of This Final Logic

Fair & Motivating: Users aren’t over-penalized for one or two misses.

Scalable: Works for daily, monthly, or yearly habits uniformly.

Economical for Users: Single freeze pool across habits.

Visually Clear: Grace and purchased freezes are distinct yet consistent.

Firebase-safe: Transactional logic prevents conflicts.

Extensible: Easy to expand into premium packs or tiered streak rewards later.


