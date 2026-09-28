package com.gymtracker.app.domain.usecase

import com.gymtracker.app.data.local.entity.ExerciseEntity
import com.gymtracker.app.data.local.entity.MuscleGroup
import com.gymtracker.app.data.local.entity.WorkoutEntity
import com.gymtracker.app.data.local.entity.WorkoutExerciseEntity
import com.gymtracker.app.domain.model.DynamicWarmUpRoutine
import com.gymtracker.app.domain.model.WarmUpBadgeType
import com.gymtracker.app.domain.model.WarmUpMovement

object DynamicWarmUpCatalog {

    fun suggestRoutineForWorkout(
        workout: WorkoutEntity?,
        exercises: List<ExerciseEntity>,
        workoutExercises: List<WorkoutExerciseEntity> = emptyList(),
    ): DynamicWarmUpRoutine {
        if (workout == null) {
            // General Full-Body / Rest Day mobility
            return getWarmUpRoutine(MuscleGroup.FULL_BODY, emptyList(), "Rest & Recovery")
        }

        val (primary, secondaries) = resolveTargetMuscleGroups(workout, exercises, workoutExercises)
        return getWarmUpRoutine(primary, secondaries, workout.name)
    }

    fun resolveTargetMuscleGroups(
        workout: WorkoutEntity,
        exercises: List<ExerciseEntity>,
        workoutExercises: List<WorkoutExerciseEntity>,
    ): Pair<MuscleGroup, List<MuscleGroup>> {
        val exerciseMap = exercises.associateBy { it.id }
        val matchingWorkoutExercises = workoutExercises.filter { it.workoutId == workout.id }

        val musclesFromExercises = matchingWorkoutExercises
            .mapNotNull { exerciseMap[it.exerciseId]?.primaryMuscle }

        if (musclesFromExercises.isNotEmpty()) {
            val frequencyMap = musclesFromExercises.groupingBy { it }.eachCount()
            val sorted = frequencyMap.entries.sortedByDescending { it.value }.map { it.key }
            val primary = sorted.first()
            val secondaries = sorted.drop(1)
            return Pair(primary, secondaries)
        }

        // Fallback: Infer from workout name or splitType
        val combinedText = "${workout.name} ${workout.splitType}".lowercase()
        return when {
            combinedText.contains("chest") || combinedText.contains("push") -> {
                Pair(MuscleGroup.CHEST, listOf(MuscleGroup.SHOULDERS, MuscleGroup.TRICEPS))
            }
            combinedText.contains("pull") || combinedText.contains("back") || combinedText.contains("lat") -> {
                Pair(MuscleGroup.BACK, listOf(MuscleGroup.BICEPS, MuscleGroup.SHOULDERS))
            }
            combinedText.contains("leg") || combinedText.contains("squat") || combinedText.contains("quad") -> {
                Pair(MuscleGroup.LEGS, listOf(MuscleGroup.GLUTES))
            }
            combinedText.contains("shoulder") || combinedText.contains("delt") || combinedText.contains("press") -> {
                Pair(MuscleGroup.SHOULDERS, listOf(MuscleGroup.TRICEPS, MuscleGroup.CHEST))
            }
            combinedText.contains("glute") || combinedText.contains("hinge") || combinedText.contains("deadlift") -> {
                Pair(MuscleGroup.GLUTES, listOf(MuscleGroup.LEGS, MuscleGroup.BACK))
            }
            combinedText.contains("arm") || combinedText.contains("bicep") || combinedText.contains("tricep") -> {
                Pair(MuscleGroup.BICEPS, listOf(MuscleGroup.TRICEPS))
            }
            combinedText.contains("upper") -> {
                Pair(MuscleGroup.CHEST, listOf(MuscleGroup.BACK, MuscleGroup.SHOULDERS))
            }
            combinedText.contains("lower") -> {
                Pair(MuscleGroup.LEGS, listOf(MuscleGroup.GLUTES, MuscleGroup.ABS))
            }
            combinedText.contains("core") || combinedText.contains("ab") -> {
                Pair(MuscleGroup.ABS, emptyList())
            }
            combinedText.contains("cardio") || combinedText.contains("hiit") || combinedText.contains("run") -> {
                Pair(MuscleGroup.CARDIO, listOf(MuscleGroup.LEGS))
            }
            else -> {
                Pair(MuscleGroup.FULL_BODY, emptyList())
            }
        }
    }

    fun getWarmUpRoutine(
        primary: MuscleGroup,
        secondaries: List<MuscleGroup> = emptyList(),
        workoutName: String = "",
    ): DynamicWarmUpRoutine {
        return when (primary) {
            MuscleGroup.CHEST -> chestWarmUp(workoutName, secondaries)
            MuscleGroup.BACK -> backWarmUp(workoutName, secondaries)
            MuscleGroup.SHOULDERS -> shouldersWarmUp(workoutName, secondaries)
            MuscleGroup.LEGS -> legsWarmUp(workoutName, secondaries)
            MuscleGroup.GLUTES -> glutesWarmUp(workoutName, secondaries)
            MuscleGroup.BICEPS, MuscleGroup.TRICEPS -> armsWarmUp(workoutName, secondaries)
            MuscleGroup.ABS -> coreWarmUp(workoutName, secondaries)
            MuscleGroup.CARDIO -> cardioWarmUp(workoutName, secondaries)
            MuscleGroup.FULL_BODY -> fullBodyWarmUp(workoutName, secondaries)
        }
    }

    fun getAllTargetRoutines(): List<DynamicWarmUpRoutine> {
        return listOf(
            chestWarmUp("", listOf(MuscleGroup.SHOULDERS, MuscleGroup.TRICEPS)),
            backWarmUp("", listOf(MuscleGroup.BICEPS)),
            legsWarmUp("", listOf(MuscleGroup.GLUTES)),
            shouldersWarmUp("", listOf(MuscleGroup.TRICEPS)),
            glutesWarmUp("", listOf(MuscleGroup.LEGS)),
            armsWarmUp("", listOf(MuscleGroup.TRICEPS)),
            coreWarmUp("", emptyList()),
            fullBodyWarmUp("", emptyList()),
        )
    }

    // -------------------------------------------------------------
    // CHEST / PUSH DAY WARM-UP
    // -------------------------------------------------------------
    private fun chestWarmUp(workoutName: String, secondaries: List<MuscleGroup>): DynamicWarmUpRoutine {
        val movements = listOf(
            WarmUpMovement(
                id = "ch_1",
                name = "Arm Circles & Dynamic Pec Openers",
                targetMuscleGroup = MuscleGroup.CHEST,
                jointMobilityFocus = "Glenohumeral joint & pectoralis major dynamic stretch",
                durationSeconds = 40,
                instructions = "Stand tall with arms outstretched. Begin with small circular rotations, progressively enlarging the circle. Transition into wide horizontal arm swings, gently hugging across the chest and opening wide to stretch pecs.",
                coachingCue = "Keep your core lightly braced so movement comes from your shoulder capsules, not lumbar arching.",
                equipment = "Bodyweight",
                badgeType = WarmUpBadgeType.MOBILITY,
            ),
            WarmUpMovement(
                id = "ch_2",
                name = "Scapular Push-Ups with Protraction",
                targetMuscleGroup = MuscleGroup.CHEST,
                jointMobilityFocus = "Serratus anterior activation & scapular rhythm",
                reps = 12,
                instructions = "Assume a rigid high plank position. Keeping elbows locked, let your shoulder blades glide together to lower your chest 2 inches, then press the floor away aggressively to round your upper back.",
                coachingCue = "Focus entirely on the shoulder blade pinch and spread. Do not let hips sag.",
                equipment = "Bodyweight",
                badgeType = WarmUpBadgeType.ACTIVATION,
            ),
            WarmUpMovement(
                id = "ch_3",
                name = "Inchworm to World's Greatest Stretch with Chest Twist",
                targetMuscleGroup = MuscleGroup.CHEST,
                jointMobilityFocus = "Thoracic spine extension, pec minor & hip flexor",
                reps = 6,
                isPerSide = true,
                instructions = "From standing, hinge at hips and walk hands out to a push-up plank. Step your right foot outside your right hand. Rotate your right arm and chest toward the ceiling, looking at your hand. Return and alternate sides.",
                coachingCue = "Exhale as you twist to deepen the stretch across your chest and mid-back.",
                equipment = "Bodyweight",
                badgeType = WarmUpBadgeType.MOBILITY,
            ),
            WarmUpMovement(
                id = "ch_4",
                name = "Banded Pull-Aparts & Diagonal Openers",
                targetMuscleGroup = MuscleGroup.SHOULDERS,
                jointMobilityFocus = "Posterior deltoid & mid-trap antagonistic stabilizer",
                reps = 15,
                instructions = "Hold a light resistance band or towel at chest height with palms facing down. Pull hands apart horizontally until the band touches your upper chest, squeezing your shoulder blades together.",
                coachingCue = "Keep shoulders depressed away from your ears. Don't shrug.",
                equipment = "Resistance Band / Bodyweight",
                badgeType = WarmUpBadgeType.JOINT_PREP,
            ),
            WarmUpMovement(
                id = "ch_5",
                name = "Explosive Dynamic Floor Push-Backs",
                targetMuscleGroup = MuscleGroup.CHEST,
                jointMobilityFocus = "Shoulder stability & central nervous system potentiation",
                reps = 8,
                instructions = "From a push-up plank, sit your hips straight back toward your heels in a loaded beast position with knees hovering. Then drive explosively forward into the plank, decelerating with controlled chest control.",
                coachingCue = "Think of springs compressing and releasing. Primes upper body motor units.",
                equipment = "Bodyweight",
                badgeType = WarmUpBadgeType.POTENTIATION,
            ),
        )

        return DynamicWarmUpRoutine(
            id = "routine_chest",
            title = "Dynamic Chest & Anterior Deltoid Primer",
            primaryMuscleGroup = MuscleGroup.CHEST,
            secondaryMuscleGroups = secondaries.ifEmpty { listOf(MuscleGroup.SHOULDERS, MuscleGroup.TRICEPS) },
            estimatedDurationMinutes = 6,
            movementsCount = movements.size,
            benefitsSummary = "Lubricates the glenohumeral joint, fires up the serratus anterior to stabilize the scapulae on the bench, and primes pectoralis motor recruitment.",
            targetWorkoutName = workoutName,
            movements = movements,
        )
    }

    // -------------------------------------------------------------
    // BACK / PULL DAY WARM-UP
    // -------------------------------------------------------------
    private fun backWarmUp(workoutName: String, secondaries: List<MuscleGroup>): DynamicWarmUpRoutine {
        val movements = listOf(
            WarmUpMovement(
                id = "bk_1",
                name = "Cat-Cow Dynamic Spinal Waves",
                targetMuscleGroup = MuscleGroup.BACK,
                jointMobilityFocus = "Thoracic and lumbar articulation & spinal synovial fluid",
                durationSeconds = 45,
                instructions = "On all fours, inhale as you tilt your pelvis forward, arch your spine, and draw your chest forward between your shoulders. Exhale as you tuck your chin, round your upper back toward the ceiling, and engage your abs.",
                coachingCue = "Initiate movement vertebrae by vertebrae rather than hinging in one stiff segment.",
                equipment = "Bodyweight",
                badgeType = WarmUpBadgeType.MOBILITY,
            ),
            WarmUpMovement(
                id = "bk_2",
                name = "Thread the Needle (Quadruped Thoracic Twist)",
                targetMuscleGroup = MuscleGroup.BACK,
                jointMobilityFocus = "Mid-back rotational mobility & rhomboids",
                reps = 8,
                isPerSide = true,
                instructions = "From table-top, slide one arm underneath your chest along the floor until your shoulder touches lightly. Then sweep that same arm up toward the ceiling, opening your chest wide.",
                coachingCue = "Keep your hips square over your knees so the twist happens purely in your thoracic spine.",
                equipment = "Bodyweight",
                badgeType = WarmUpBadgeType.MOBILITY,
            ),
            WarmUpMovement(
                id = "bk_3",
                name = "Banded Face Pulls with External Rotation",
                targetMuscleGroup = MuscleGroup.SHOULDERS,
                jointMobilityFocus = "Infraspinatus, teres minor & rear deltoid activation",
                reps = 15,
                instructions = "Anchor a light band at eye level. Grasp the ends with thumbs facing back. Pull toward your forehead, separating your hands and rotating elbows high and wide.",
                coachingCue = "Pause for 1 full second at peak contraction. Feel your upper back ignite.",
                equipment = "Resistance Band",
                badgeType = WarmUpBadgeType.ACTIVATION,
            ),
            WarmUpMovement(
                id = "bk_4",
                name = "Scapular Pull-Downs / Barbell Lat Sweeps",
                targetMuscleGroup = MuscleGroup.BACK,
                jointMobilityFocus = "Latissimus dorsi & lower trapezius depression",
                reps = 10,
                instructions = "Hang from a pull-up bar (or hold a band overhead). Without bending elbows, pull your shoulder blades down into your back pockets, lifting your chest 2-3 inches. Hold for 1 second, then lower.",
                coachingCue = "Keep arms straight like ropes. Let the lats and lower traps do 100% of the movement.",
                equipment = "Pull-up Bar or Band",
                badgeType = WarmUpBadgeType.ACTIVATION,
            ),
            WarmUpMovement(
                id = "bk_5",
                name = "Bird-Dog Contralateral Holds",
                targetMuscleGroup = MuscleGroup.BACK,
                jointMobilityFocus = "Posterior oblique sling & spinal anti-rotational stability",
                reps = 8,
                isPerSide = true,
                instructions = "From all fours, extend opposite arm and leg straight out parallel to the ground. Squeeze your glute and lower trap at peak reach for 2 seconds, then return smoothly.",
                coachingCue = "Imagine balancing a hot cup of coffee on your lower back. No hip tilting.",
                equipment = "Bodyweight",
                badgeType = WarmUpBadgeType.POTENTIATION,
            ),
        )

        return DynamicWarmUpRoutine(
            id = "routine_back",
            title = "Lat Mobilization & Thoracic Spine Dynamic Prep",
            primaryMuscleGroup = MuscleGroup.BACK,
            secondaryMuscleGroups = secondaries.ifEmpty { listOf(MuscleGroup.BICEPS, MuscleGroup.SHOULDERS) },
            estimatedDurationMinutes = 6,
            movementsCount = movements.size,
            benefitsSummary = "Restores multi-planar thoracic rotation, activates lower traps and rear delts to stabilize the spine, and primes latissimus contraction for heavy pulling.",
            targetWorkoutName = workoutName,
            movements = movements,
        )
    }

    // -------------------------------------------------------------
    // LEGS / LOWER BODY WARM-UP
    // -------------------------------------------------------------
    private fun legsWarmUp(workoutName: String, secondaries: List<MuscleGroup>): DynamicWarmUpRoutine {
        val movements = listOf(
            WarmUpMovement(
                id = "leg_1",
                name = "Dynamic Leg Swings (Front-to-Back & Lateral)",
                targetMuscleGroup = MuscleGroup.LEGS,
                jointMobilityFocus = "Hamstring dynamic elasticity, adductors & hip capsule",
                reps = 10,
                isPerSide = true,
                instructions = "Hold onto a rack or wall for balance. Swing one leg forward and back smoothly, letting momentum stretch the hamstrings and hip flexors. Turn facing the wall and swing side-to-side across the midline.",
                coachingCue = "Start at 50% range of motion and let each swing reach slightly higher without forcing.",
                equipment = "Wall or Rack",
                badgeType = WarmUpBadgeType.MOBILITY,
            ),
            WarmUpMovement(
                id = "leg_2",
                name = "Deep Bodyweight Squat Pry with Overhead Reach",
                targetMuscleGroup = MuscleGroup.LEGS,
                jointMobilityFocus = "Ankle dorsiflexion, adductor length & thoracic extension",
                reps = 8,
                instructions = "Drop into a deep bodyweight squat. Place elbows inside knees to gently pry hips open. Shift weight slightly side-to-side to mobilize ankles, then extend one arm to ceiling, rotating chest up.",
                coachingCue = "Keep heels glued to the floor. If heels lift, widen stance slightly.",
                equipment = "Bodyweight",
                badgeType = WarmUpBadgeType.MOBILITY,
            ),
            WarmUpMovement(
                id = "leg_3",
                name = "Cossack Squats (Alternating Lateral Lunges)",
                targetMuscleGroup = MuscleGroup.LEGS,
                jointMobilityFocus = "Frontal plane hip mobility & groin lengthening",
                reps = 6,
                isPerSide = true,
                instructions = "Stand in a wide straddle. Shift hips back and down over one heel while keeping the opposite leg straight with toes rotated up. Push through the bent leg to return to center and switch.",
                coachingCue = "Keep torso upright and chest open. Sink as low as your hip mobility comfortably allows.",
                equipment = "Bodyweight",
                badgeType = WarmUpBadgeType.MOBILITY,
            ),
            WarmUpMovement(
                id = "leg_4",
                name = "Walking High Knees to Dynamic Quad Pulls",
                targetMuscleGroup = MuscleGroup.LEGS,
                jointMobilityFocus = "Hip flexor drive & dynamic quadriceps stretch",
                durationSeconds = 40,
                instructions = "Step forward, hug knee to chest for 1 second. Step forward two paces, grab ankle behind you to gently pull heel toward glute while extending opposite arm high. Alternate continuously.",
                coachingCue = "Tuck pelvis slightly during the quad stretch to feel deep lengthening in the rectus femoris.",
                equipment = "Bodyweight",
                badgeType = WarmUpBadgeType.ACTIVATION,
            ),
            WarmUpMovement(
                id = "leg_5",
                name = "Ankle Dorsiflexion Wall Rocks",
                targetMuscleGroup = MuscleGroup.LEGS,
                jointMobilityFocus = "Talocrural joint clearance for deep squat depth",
                reps = 12,
                isPerSide = true,
                instructions = "Place toes 3 to 4 inches away from a wall in a staggered stance. Drive knee straight forward over middle toe until it gently taps the wall, keeping the heel grounded. Hold for 1 second, then release.",
                coachingCue = "Avoid letting your knee collapse inward. Track knee directly over the second and third toes.",
                equipment = "Wall",
                badgeType = WarmUpBadgeType.JOINT_PREP,
            ),
        )

        return DynamicWarmUpRoutine(
            id = "routine_legs",
            title = "Lower Body Dynamic Primer & Hip Mobilizer",
            primaryMuscleGroup = MuscleGroup.LEGS,
            secondaryMuscleGroups = secondaries.ifEmpty { listOf(MuscleGroup.GLUTES) },
            estimatedDurationMinutes = 6,
            movementsCount = movements.size,
            benefitsSummary = "Increases ankle dorsiflexion for deeper squat mechanics, opens the hip capsule to prevent groin pinches, and dynamically prepares knee tendons for heavy loading.",
            targetWorkoutName = workoutName,
            movements = movements,
        )
    }

    // -------------------------------------------------------------
    // SHOULDERS WARM-UP
    // -------------------------------------------------------------
    private fun shouldersWarmUp(workoutName: String, secondaries: List<MuscleGroup>): DynamicWarmUpRoutine {
        val movements = listOf(
            WarmUpMovement(
                id = "sh_1",
                name = "Arm Swings & Cross-Body Dynamic Hugs",
                targetMuscleGroup = MuscleGroup.SHOULDERS,
                jointMobilityFocus = "Anterior/posterior deltoid dynamic elasticity",
                durationSeconds = 40,
                instructions = "Swing arms across the chest in horizontal motion, alternating which arm crosses on top. Keep shoulders relaxed away from ears.",
                coachingCue = "Breathe rhythmically. Let arms swing effortlessly to pump blood into deltoids.",
                equipment = "Bodyweight",
                badgeType = WarmUpBadgeType.MOBILITY,
            ),
            WarmUpMovement(
                id = "sh_2",
                name = "Scapular Wall Slides with Forearm Pressure",
                targetMuscleGroup = MuscleGroup.SHOULDERS,
                jointMobilityFocus = "Serratus anterior & upward scapular rotation",
                reps = 10,
                instructions = "Stand with back, head, and elbows against a wall in a 'W' position. Slide arms up into a 'Y' shape, keeping forearms and wrists against the wall without arching your lower back.",
                coachingCue = "Press outward with forearms throughout. Essential for pain-free overhead pressing.",
                equipment = "Wall",
                badgeType = WarmUpBadgeType.ACTIVATION,
            ),
            WarmUpMovement(
                id = "sh_3",
                name = "Prone Y-T-W Rotator Cuff Raises",
                targetMuscleGroup = MuscleGroup.SHOULDERS,
                jointMobilityFocus = "Supraspinatus, infraspinatus & lower trapezius",
                reps = 6,
                instructions = "Lie face down with thumbs pointed up. Raise arms into a Y position for 6 reps, then T position for 6 reps, then W position squeezing shoulder blades together.",
                coachingCue = "Initiate lift from your mid-back shoulder blades, not by craning your neck.",
                equipment = "Bodyweight",
                badgeType = WarmUpBadgeType.ACTIVATION,
            ),
            WarmUpMovement(
                id = "sh_4",
                name = "Banded Overhead Pass-Throughs (Dislocates)",
                targetMuscleGroup = MuscleGroup.SHOULDERS,
                jointMobilityFocus = "Subacromial space clearance & anterior capsule",
                reps = 12,
                instructions = "Hold a light resistance band with a wide grip in front of thighs. With straight arms, bring band up overhead and back behind your hips in a continuous sweeping circle.",
                coachingCue = "Widen your grip if you feel any pinching. Movement should feel completely fluid.",
                equipment = "Resistance Band / Towel",
                badgeType = WarmUpBadgeType.JOINT_PREP,
            ),
            WarmUpMovement(
                id = "sh_5",
                name = "Bear Crawl Hover with Shoulder Taps",
                targetMuscleGroup = MuscleGroup.SHOULDERS,
                jointMobilityFocus = "Glenohumeral co-contraction & rotational stability",
                durationSeconds = 30,
                instructions = "Start on hands and knees, lift knees 1-2 inches off floor. Slowly lift right hand to tap left shoulder without swaying your hips. Alternate sides with control.",
                coachingCue = "Keep back as flat as a table. Creates rock-solid overhead pressing foundation.",
                equipment = "Bodyweight",
                badgeType = WarmUpBadgeType.POTENTIATION,
            ),
        )

        return DynamicWarmUpRoutine(
            id = "routine_shoulders",
            title = "Shoulder Capsule & Overhead Mobility Routine",
            primaryMuscleGroup = MuscleGroup.SHOULDERS,
            secondaryMuscleGroups = secondaries.ifEmpty { listOf(MuscleGroup.TRICEPS, MuscleGroup.CHEST) },
            estimatedDurationMinutes = 5,
            movementsCount = movements.size,
            benefitsSummary = "Clears the subacromial space to eliminate impingement, fires up rotator cuff stabilizers, and establishes smooth upward scapular rotation for overhead and lateral lifts.",
            targetWorkoutName = workoutName,
            movements = movements,
        )
    }

    // -------------------------------------------------------------
    // GLUTES / POSTERIOR CHAIN WARM-UP
    // -------------------------------------------------------------
    private fun glutesWarmUp(workoutName: String, secondaries: List<MuscleGroup>): DynamicWarmUpRoutine {
        val movements = listOf(
            WarmUpMovement(
                id = "gl_1",
                name = "Glute Bridge with 2-Second Peak Squeeze",
                targetMuscleGroup = MuscleGroup.GLUTES,
                jointMobilityFocus = "Gluteus maximus motor unit recruitment & hip extension",
                reps = 12,
                instructions = "Lie on back with knees bent and feet flat on floor shoulder-width apart. Drive through heels, lift hips until knees, hips, and shoulders form a straight line. Squeeze glutes hard at the top.",
                coachingCue = "Do not hyperextend lower back. Focus on maximum glute contraction at top lockout.",
                equipment = "Bodyweight",
                badgeType = WarmUpBadgeType.ACTIVATION,
            ),
            WarmUpMovement(
                id = "gl_2",
                name = "90/90 Hip Mobility Transitions",
                targetMuscleGroup = MuscleGroup.GLUTES,
                jointMobilityFocus = "Hip internal & external rotational capacity",
                reps = 8,
                instructions = "Sit on floor with both knees bent at 90-degree angles (front leg externally rotated, rear leg internally rotated). Rotate knees up and across to the opposite side smoothly without shifting torso forward.",
                coachingCue = "Keep tall posture through the crown of your head as you pivot across.",
                equipment = "Bodyweight",
                badgeType = WarmUpBadgeType.MOBILITY,
            ),
            WarmUpMovement(
                id = "gl_3",
                name = "Single-Leg Romanian Deadlift Reach",
                targetMuscleGroup = MuscleGroup.GLUTES,
                jointMobilityFocus = "Hamstring eccentric control & pelvic stabilization",
                reps = 8,
                isPerSide = true,
                instructions = "Stand on one leg with a soft knee bend. Hinge at hips, reaching rear leg straight back and hands toward the floor. Stand tall by squeezing your supporting glute.",
                coachingCue = "Keep back toes pointed straight down at floor to prevent hip from rotating open.",
                equipment = "Bodyweight",
                badgeType = WarmUpBadgeType.ACTIVATION,
            ),
            WarmUpMovement(
                id = "gl_4",
                name = "Fire Hydrants with Circular Hip Rotations",
                targetMuscleGroup = MuscleGroup.GLUTES,
                jointMobilityFocus = "Gluteus medius & acetabulofemoral synovial fluid",
                reps = 8,
                isPerSide = true,
                instructions = "On all fours, lift one knee out to the side at a 90-degree angle, then draw smooth circular loops forward and backward through full hip range.",
                coachingCue = "Keep core braced to avoid leaning weight excessively onto the opposite arm.",
                equipment = "Bodyweight",
                badgeType = WarmUpBadgeType.JOINT_PREP,
            ),
            WarmUpMovement(
                id = "gl_5",
                name = "Banded Monster Walks / Lateral Shuffles",
                targetMuscleGroup = MuscleGroup.GLUTES,
                jointMobilityFocus = "Abductor potentiation before squats and deadlifts",
                reps = 15,
                isPerSide = true,
                instructions = "Place band around ankles or above knees. Lower into a quarter squat and step laterally 15 paces right, then 15 paces left, maintaining band tension throughout.",
                coachingCue = "Step with control; never let knees collapse inside the line of your feet.",
                equipment = "Resistance Band / Bodyweight",
                badgeType = WarmUpBadgeType.POTENTIATION,
            ),
        )

        return DynamicWarmUpRoutine(
            id = "routine_glutes",
            title = "Glute Activation & Posterior Chain Hinge Prep",
            primaryMuscleGroup = MuscleGroup.GLUTES,
            secondaryMuscleGroups = secondaries.ifEmpty { listOf(MuscleGroup.LEGS, MuscleGroup.BACK) },
            estimatedDurationMinutes = 6,
            movementsCount = movements.size,
            benefitsSummary = "Wakes up dormant gluteus medius and maximus motor units to prevent lower back compensation, opens hip capsules, and pre-tenses posterior kinetic chain.",
            targetWorkoutName = workoutName,
            movements = movements,
        )
    }

    // -------------------------------------------------------------
    // ARMS (BICEPS / TRICEPS) WARM-UP
    // -------------------------------------------------------------
    private fun armsWarmUp(workoutName: String, secondaries: List<MuscleGroup>): DynamicWarmUpRoutine {
        val movements = listOf(
            WarmUpMovement(
                id = "arm_1",
                name = "Dynamic Wrist Waves & Forearm Extensor Rolls",
                targetMuscleGroup = MuscleGroup.BICEPS,
                jointMobilityFocus = "Carpal bones, flexor & extensor tendon lubrication",
                durationSeconds = 40,
                instructions = "Interlock fingers and roll wrists in fluid figure-8 motions. Press palms away with straight arms, then pull fingers gently back to stretch forearms dynamically.",
                coachingCue = "Relieves tight flexors and prepares wrists to bear heavy barbell and dumbbell loads.",
                equipment = "Bodyweight",
                badgeType = WarmUpBadgeType.JOINT_PREP,
            ),
            WarmUpMovement(
                id = "arm_2",
                name = "Light Band Overhead Triceps Extensions",
                targetMuscleGroup = MuscleGroup.TRICEPS,
                jointMobilityFocus = "Triceps tendon conditioning & elbow joint lubrication",
                reps = 15,
                instructions = "Stand on one end of a light band, press other end overhead. Keeping elbows pinned in near ears, lower band behind head and extend fully to contract triceps.",
                coachingCue = "Use very light tension. This is about circulating blood and warming the elbow joint.",
                equipment = "Resistance Band",
                badgeType = WarmUpBadgeType.ACTIVATION,
            ),
            WarmUpMovement(
                id = "arm_3",
                name = "Banded Biceps Curls with Pronation to Supination",
                targetMuscleGroup = MuscleGroup.BICEPS,
                jointMobilityFocus = "Biceps brachii & brachialis blood flow",
                reps = 15,
                instructions = "Step on band with arms at sides. Start with palms facing thighs, curl upward while rotating wrists outward so palms face shoulders at the top contraction.",
                coachingCue = "Squeeze peak contraction for 1 second to fire the biceps short and long heads.",
                equipment = "Resistance Band",
                badgeType = WarmUpBadgeType.ACTIVATION,
            ),
            WarmUpMovement(
                id = "arm_4",
                name = "Push-Up to Downward Dog Pump",
                targetMuscleGroup = MuscleGroup.TRICEPS,
                jointMobilityFocus = "Scapular glide & triceps eccentric stretch",
                reps = 8,
                instructions = "Perform a strict push-up, then push chest back toward feet into a downward dog, actively pressing floor away through palm heels to stretch triceps and shoulders.",
                coachingCue = "Drop heels toward floor in downward dog to get bonus calf and hamstring stretch.",
                equipment = "Bodyweight",
                badgeType = WarmUpBadgeType.MOBILITY,
            ),
        )

        return DynamicWarmUpRoutine(
            id = "routine_arms",
            title = "Arm Synergy, Elbow Joint & Grip Primer",
            primaryMuscleGroup = MuscleGroup.BICEPS,
            secondaryMuscleGroups = secondaries.ifEmpty { listOf(MuscleGroup.TRICEPS) },
            estimatedDurationMinutes = 5,
            movementsCount = movements.size,
            benefitsSummary = "Floods elbow connective tissues and tendon sheaths with blood, protects against golfer's/tennis elbow, and warms the forearm flexors for peak grip strength.",
            targetWorkoutName = workoutName,
            movements = movements,
        )
    }

    // -------------------------------------------------------------
    // CORE / ABS WARM-UP
    // -------------------------------------------------------------
    private fun coreWarmUp(workoutName: String, secondaries: List<MuscleGroup>): DynamicWarmUpRoutine {
        val movements = listOf(
            WarmUpMovement(
                id = "ab_1",
                name = "Deadbugs with Controlled Cross-Extension",
                targetMuscleGroup = MuscleGroup.ABS,
                jointMobilityFocus = "Transverse abdominis activation & anterior pelvic control",
                reps = 10,
                isPerSide = true,
                instructions = "Lie on back with arms straight up and knees bent at 90 degrees. Flatten lower back into floor. Slowly extend opposite arm and leg toward floor while exhaling all air.",
                coachingCue = "If lower back lifts off floor even slightly, reduce the extension distance.",
                equipment = "Bodyweight",
                badgeType = WarmUpBadgeType.ACTIVATION,
            ),
            WarmUpMovement(
                id = "ab_2",
                name = "Bird-Dog Contralateral Holds",
                targetMuscleGroup = MuscleGroup.ABS,
                jointMobilityFocus = "Posterior sling & anti-rotational stability",
                reps = 8,
                isPerSide = true,
                instructions = "From quadruped position, reach right arm forward and left leg back. Hold for 2 seconds with spine neutral, then return and switch sides.",
                coachingCue = "Brace core as if expecting a punch to the stomach.",
                equipment = "Bodyweight",
                badgeType = WarmUpBadgeType.ACTIVATION,
            ),
            WarmUpMovement(
                id = "ab_3",
                name = "Plank to Downward Dog Shin Taps",
                targetMuscleGroup = MuscleGroup.ABS,
                jointMobilityFocus = "Anterior abdominal chain & dynamic hamstring stretch",
                reps = 8,
                isPerSide = true,
                instructions = "From high plank, push hips up and back into downward dog while reaching right hand across to tap left shin. Return to plank and repeat opposite.",
                coachingCue = "Pause in solid high plank for 1 second on each repetition.",
                equipment = "Bodyweight",
                badgeType = WarmUpBadgeType.MOBILITY,
            ),
            WarmUpMovement(
                id = "ab_4",
                name = "Dynamic Side Plank Hip Pulses",
                targetMuscleGroup = MuscleGroup.ABS,
                jointMobilityFocus = "Internal/external obliques & quadratus lumborum",
                reps = 10,
                isPerSide = true,
                instructions = "In side plank with forearm under shoulder and feet stacked, dip hip 3 inches toward floor, then drive hip back up engaging bottom oblique.",
                coachingCue = "Keep chest and hips open facing straight ahead throughout the pulse.",
                equipment = "Bodyweight",
                badgeType = WarmUpBadgeType.POTENTIATION,
            ),
        )

        return DynamicWarmUpRoutine(
            id = "routine_core",
            title = "Core Temperature & Anti-Extension Primer",
            primaryMuscleGroup = MuscleGroup.ABS,
            secondaryMuscleGroups = secondaries,
            estimatedDurationMinutes = 5,
            movementsCount = movements.size,
            benefitsSummary = "Activates deep transverse abdominis bracing, secures neutral spine positioning, and prepares rotational stabilizers for compound resistance.",
            targetWorkoutName = workoutName,
            movements = movements,
        )
    }

    // -------------------------------------------------------------
    // CARDIO WARM-UP
    // -------------------------------------------------------------
    private fun cardioWarmUp(workoutName: String, secondaries: List<MuscleGroup>): DynamicWarmUpRoutine {
        val movements = listOf(
            WarmUpMovement(
                id = "cd_1",
                name = "High Knees & Butt Kicks Transition",
                targetMuscleGroup = MuscleGroup.CARDIO,
                jointMobilityFocus = "Heart rate elevation & knee flexion/extension",
                durationSeconds = 45,
                instructions = "Jog in place driving knees up to hip height for 20 seconds, immediately transition to kicking heels up to glutes for 25 seconds.",
                coachingCue = "Land on ball of foot with springy ankle stiffness.",
                equipment = "Bodyweight",
                badgeType = WarmUpBadgeType.CNS_WAKE,
            ),
            WarmUpMovement(
                id = "cd_2",
                name = "Skater Hops with Balance Hold",
                targetMuscleGroup = MuscleGroup.CARDIO,
                jointMobilityFocus = "Lateral power, knee stabilization & ankle reactivity",
                reps = 12,
                instructions = "Bound laterally from left foot to right foot, sweeping trailing leg behind like a speed skater. Stick the landing for a half second before exploding back.",
                coachingCue = "Absorb the landing softly by flexing hip, knee, and ankle simultaneously.",
                equipment = "Bodyweight",
                badgeType = WarmUpBadgeType.POTENTIATION,
            ),
            WarmUpMovement(
                id = "cd_3",
                name = "Walking Lunges with Torso Twist",
                targetMuscleGroup = MuscleGroup.CARDIO,
                jointMobilityFocus = "Hip extension & thoracic rotation",
                reps = 10,
                instructions = "Step forward into a lunge, rotate upper body toward the front knee side. Push up through lead heel and step into next lunge.",
                coachingCue = "Stay tall and keep knee tracking directly above ankle.",
                equipment = "Bodyweight",
                badgeType = WarmUpBadgeType.MOBILITY,
            ),
            WarmUpMovement(
                id = "cd_4",
                name = "Ankle Bounces & Calf Springs",
                targetMuscleGroup = MuscleGroup.CARDIO,
                jointMobilityFocus = "Achilles tendon elasticity & CNS readiness",
                durationSeconds = 30,
                instructions = "Hop lightly on both feet like jumping rope, emphasizing quick ground contact time with minimal knee bend.",
                coachingCue = "Keep feet quick and light. Breathe through nose.",
                equipment = "Bodyweight",
                badgeType = WarmUpBadgeType.POTENTIATION,
            ),
        )

        return DynamicWarmUpRoutine(
            id = "routine_cardio",
            title = "Aerobic Pulse Raiser & Elasticity Prep",
            primaryMuscleGroup = MuscleGroup.CARDIO,
            secondaryMuscleGroups = secondaries.ifEmpty { listOf(MuscleGroup.LEGS) },
            estimatedDurationMinutes = 5,
            movementsCount = movements.size,
            benefitsSummary = "Gradually raises core body temperature and cardiac output, conditions Achilles tendons for impact, and optimizes neuromuscular reactivity.",
            targetWorkoutName = workoutName,
            movements = movements,
        )
    }

    // -------------------------------------------------------------
    // FULL BODY WARM-UP (Compound / Rest Day Mobility)
    // -------------------------------------------------------------
    private fun fullBodyWarmUp(workoutName: String, secondaries: List<MuscleGroup>): DynamicWarmUpRoutine {
        val movements = listOf(
            WarmUpMovement(
                id = "fb_1",
                name = "Inchworm Walkout to World's Greatest Stretch",
                targetMuscleGroup = MuscleGroup.FULL_BODY,
                jointMobilityFocus = "Full-body multi-joint kinetic chain mobilizer",
                reps = 6,
                isPerSide = true,
                instructions = "Stand tall, hinge at hips with soft knees, walk hands out to plank. Step right foot outside right hand. Rotate right arm to ceiling opening chest, return hand, and step foot back. Walk hands back to standing.",
                coachingCue = "A complete head-to-toe mobilizer. Take deep breaths in the lunge stretch.",
                equipment = "Bodyweight",
                badgeType = WarmUpBadgeType.MOBILITY,
            ),
            WarmUpMovement(
                id = "fb_2",
                name = "Bootstrapper Squats (Hamstring Stretch to Deep Squat)",
                targetMuscleGroup = MuscleGroup.LEGS,
                jointMobilityFocus = "Posterior chain lengthening & hip adductor opening",
                reps = 10,
                instructions = "Stand shoulder-width, reach down and hold onto your toes in a forward fold. Pull your hips down into a deep squat, lifting your chest tall. Hold 1 second, then press hips back up to ceiling.",
                coachingCue = "Keep elbows inside knees during the bottom squat to gently pry hips open.",
                equipment = "Bodyweight",
                badgeType = WarmUpBadgeType.MOBILITY,
            ),
            WarmUpMovement(
                id = "fb_3",
                name = "Dynamic Arm Circles & Torso Twists",
                targetMuscleGroup = MuscleGroup.FULL_BODY,
                jointMobilityFocus = "Shoulder range of motion & rotational spinal waves",
                durationSeconds = 45,
                instructions = "Perform 10 forward arm circles, 10 backward arm circles, then let arms swing freely side to side in horizontal torso twists, allowing back heel to pivot naturally.",
                coachingCue = "Relax shoulders, shake out arms, and allow spine to rotate smoothly.",
                equipment = "Bodyweight",
                badgeType = WarmUpBadgeType.MOBILITY,
            ),
            WarmUpMovement(
                id = "fb_4",
                name = "Reverse Lunge with Overhead Lateral Reach",
                targetMuscleGroup = MuscleGroup.LEGS,
                jointMobilityFocus = "Psoas, rectus femoris & latissimus stretch",
                reps = 6,
                isPerSide = true,
                instructions = "Step back into a lunge with your left leg. Reach your left arm straight overhead and lean gently to the right side, feeling the stretch through your left hip flexor and ribcage. Switch sides.",
                coachingCue = "Squeeze the glute of your rear leg to accentuate the hip flexor opening.",
                equipment = "Bodyweight",
                badgeType = WarmUpBadgeType.ACTIVATION,
            ),
            WarmUpMovement(
                id = "fb_5",
                name = "Glute Bridge to Scapular Push-Up Combo",
                targetMuscleGroup = MuscleGroup.FULL_BODY,
                jointMobilityFocus = "Anterior & posterior neuromuscular co-activation",
                reps = 8,
                instructions = "Perform 8 glute bridges holding 2 seconds at the top, then flip over to a plank position and perform 8 scapular push-ups focusing on spreading and pinching shoulder blades.",
                coachingCue = "Connects the glutes and upper back stabilizers for total-body compound readiness.",
                equipment = "Bodyweight",
                badgeType = WarmUpBadgeType.POTENTIATION,
            ),
        )

        return DynamicWarmUpRoutine(
            id = "routine_full_body",
            title = "Full-Body Kinetic Chain Potentiation",
            primaryMuscleGroup = MuscleGroup.FULL_BODY,
            secondaryMuscleGroups = secondaries,
            estimatedDurationMinutes = 7,
            movementsCount = movements.size,
            benefitsSummary = "Synchronizes upper and lower body neuromuscular firing patterns, promotes synovial fluid distribution through all major joints, and activates the core cylinder.",
            targetWorkoutName = workoutName,
            movements = movements,
        )
    }
}
