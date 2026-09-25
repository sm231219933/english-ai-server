package com.TeacherTinkl.myapplication.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Question::class,
        Formula::class,
        SpeakingLesson::class,
        LessonSentence::class,
        SpeakingReport::class,
        SpeakingGymSentence::class
    ],
    version = 15,
    exportSchema = false
)
abstract class TinklDatabase : RoomDatabase() {
    abstract fun tinklDao(): TinklDao

    companion object {
        @Volatile
        private var INSTANCE: TinklDatabase? = null

        fun getDatabase(context: Context): TinklDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TinklDatabase::class.java,
                    "tinkl_database"
                )
                .fallbackToDestructiveMigration()
                .addCallback(TinklDatabaseCallback())
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class TinklDatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateDatabase(database.tinklDao())
                    }
                }
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateTopicLessonsIfEmpty(database.tinklDao())
                        populateGymSentencesIfEmpty(database.tinklDao())
                    }
                }
            }

            private suspend fun populateTopicLessonsIfEmpty(dao: TinklDao) {
                if (dao.getSpeakingLessonCount() > 0) return

                // Topic 1: Morning Routine & Waking Up ⏰
                val t1Id = dao.insertSpeakingLesson(SpeakingLesson(
                    category = "Waking Up",
                    title = "Morning Routine & Waking Up ⏰",
                    description = "Master daily English phrases about waking up early, alarms, and morning habits.",
                    level = "Beginner",
                    iconName = "Alarm"
                )).toInt()

                val t1_1stPerson = listOf(
                    "Main roz subah jaldi uth jata hoon." to "I wake up early in the morning every day.",
                    "Aaj meri aankh der se khuli." to "I woke up late today.",
                    "Main chahta hoon ki main subah 5 baje uthu." to "I want to wake up at 5 AM in the morning.",
                    "Mujhe bina alarm ke uthne me aalas aata hai." to "I feel lazy to wake up without an alarm.",
                    "Main abhi utha hoon." to "I have just woken up.",
                    "Mujhe uthne me dus minute lagte hain." to "It takes me ten minutes to wake up.",
                    "Meri nind achanak khul gayi." to "I woke up suddenly.",
                    "Main uth kar garm paani peeta hoon." to "I drink warm water after waking up.",
                    "Mujhe alarm bajne se pehle uthne ki aadat hai." to "I have a habit of waking up before the alarm rings.",
                    "Main aaj raat jaldi sounga." to "I will sleep early tonight.",
                    "Jab main uthta hoon, mujhe bhookh lagti hai." to "When I wake up, I feel hungry.",
                    "Mujhe aaj uthne ka mann nahi kar raha tha." to "I didn't feel like waking up today.",
                    "Hum chhutti ke din aaram se uthte hain." to "We wake up comfortably on holidays.",
                    "Main koshish kar raha hoon jaldi uthne ki." to "I am trying to wake up early.",
                    "Main uthne ke baad sidha nahaane jata hoon." to "I go straight to bathe after waking up.",
                    "Mujhe subah jaldi uthna pasand nahi hai." to "I do not like to wake up early in the morning.",
                    "Main apne aap uth gaya." to "I woke up by myself.",
                    "Main thodi der aur sona chahta hoon." to "I want to sleep for a little longer.",
                    "Hum roz subah uth kar yoga karte hain." to "We do yoga every morning after waking up.",
                    "Main sapna dekh raha tha tabhi uth gaya." to "I was dreaming when I woke up."
                )
                t1_1stPerson.forEach { (hindi, eng) ->
                    dao.insertLessonSentence(LessonSentence(
                        lessonId = t1Id,
                        personTag = "1st Person",
                        promptHindi = hindi,
                        promptQuestion = "Say in English: $hindi",
                        expectedText = eng
                    ))
                }

                val t1_2ndPerson = listOf(
                    "Tum aaj itni jaldi kaise uth gaye?" to "How did you wake up so early today?",
                    "Tu abhi tak soya hua hai? Jaldi uth!" to "Are you still sleeping? Wake up quickly!",
                    "Aapko subah kitne baje uthna hota hai?" to "What time do you have to wake up in the morning?",
                    "Agar tumhe kal jaldi uthna hai, toh abhi so jao." to "If you have to wake up early tomorrow, go to sleep now.",
                    "Uth jao, subah ho gayi hai." to "Wake up, it is morning.",
                    "Tumhara alarm baj raha hai, uth jao." to "Your alarm is ringing, wake up.",
                    "Tum bistar se kab bahar aoge?" to "When will you get out of bed?",
                    "Tum abhi tak kyu nahi uthe?" to "Why haven't you woken up yet?",
                    "Bhai, uth ja office jana hai." to "Brother, wake up, you have to go to the office.",
                    "Tum uthne me itna nakhra kyu karte ho?" to "Why do you fuss so much to wake up?",
                    "Kya tumhe uthne me takleef hoti hai?" to "Do you have trouble waking up?",
                    "Tumhe aaj kisne uthaya?" to "Who woke you up today?",
                    "Chalo uth jao aur brush kar lo." to "Come on, wake up and brush your teeth.",
                    "Tumhe kitni baar uthana padega?" to "How many times do I have to wake you up?",
                    "Tumhe uthane me mera dimaag kharab ho jata hai." to "It ruins my mind to wake you up.",
                    "Tum kal se jaldi uthne ki koshish karna." to "You try to wake up early from tomorrow.",
                    "Uth ja beta, kab tak soyega?" to "Wake up son, how long will you sleep?",
                    "Tum itni der tak kaise so sakte ho?" to "How can you sleep for so long?",
                    "Tum uthte hi chai kyu mangte ho?" to "Why do you ask for tea as soon as you wake up?",
                    "Kya tum roz subah uth kar nahate ho?" to "Do you bathe every morning after waking up?"
                )
                t1_2ndPerson.forEach { (hindi, eng) ->
                    dao.insertLessonSentence(LessonSentence(
                        lessonId = t1Id,
                        personTag = "2nd Person",
                        promptHindi = hindi,
                        promptQuestion = "Ask in English: $hindi",
                        expectedText = eng
                    ))
                }

                val t1_3rdPerson = listOf(
                    "Wo abhi tak utha nahi kya? Main kab se uska wait kar raha hoon." to "Hasn't he woken up yet? I have been waiting for him for so long.",
                    "Nidhi ko subah jaldi uthne ki aadat hai." to "Nidhi has a habit of waking up early in the morning.",
                    "Usko uthana bohot mushkil kaam hai." to "Waking him up is a very difficult task.",
                    "Jab tak mummy usko awaaz nahi lagati, wo bistar se nahi nikalta." to "Until mom calls him, he doesn't get out of bed.",
                    "Wo roz subah chhah baje uthta hai." to "He wakes up at six AM every day.",
                    "Uska alarm kab bajega?" to "When will his alarm ring?",
                    "Wo apne aap kabhi nahi uthta." to "He never wakes up by himself.",
                    "Jab wo uthta hai, toh gusse me rehta hai." to "When he wakes up, he is angry.",
                    "Wo hamesha sabse pehle uthti hai." to "She always wakes up first.",
                    "Usne mujhe raat ko 2 baje utha diya." to "He woke me up at 2 AM in the night.",
                    "Wo aaj bina uthe lamba soya." to "He slept for a long time today without waking up.",
                    "Wo uthte hi apna phone check karta hai." to "He checks his phone as soon as he wakes up.",
                    "Wo shayad abhi uth raha hoga." to "He might be waking up right now.",
                    "Usko uthane ki zimmedari meri nahi hai." to "It is not my responsibility to wake him up.",
                    "Mujhe nahi pata wo kab uthega." to "I don't know when he will wake up.",
                    "Uske papa ne usko daant kar uthaya." to "His father woke him up by scolding.",
                    "Wo chhutti ke din bhi jaldi uth jata hai." to "He wakes up early even on holidays.",
                    "Jab tak suraj nahi nikalta, wo nahi uthta." to "He doesn't wake up until the sun rises.",
                    "Main usko uthana bhool gaya." to "I forgot to wake him up.",
                    "Us bacche ko mat uthao, wo roega." to "Do not wake that child up, he will cry."
                )
                t1_3rdPerson.forEach { (hindi, eng) ->
                    dao.insertLessonSentence(LessonSentence(
                        lessonId = t1Id,
                        personTag = "3rd Person",
                        promptHindi = hindi,
                        promptQuestion = "Say about third person: $hindi",
                        expectedText = eng
                    ))
                }

                // Topic 2: Tea & Breakfast ☕
                val t2Id = dao.insertSpeakingLesson(SpeakingLesson(
                    category = "Tea & Breakfast",
                    title = "Tea & Breakfast Phrases ☕",
                    description = "Learn how to talk about tea, breakfast habits, and morning snacks in daily life.",
                    level = "Beginner",
                    iconName = "FreeBreakfast"
                )).toInt()

                val t2_1stPerson = listOf(
                    "Main uthte hi chai peeta hoon." to "I drink tea as soon as I wake up.",
                    "Mujhe bina cheeni ki chai pasand hai." to "I like tea without sugar.",
                    "Main nashte me parathe khata hoon." to "I eat parathas for breakfast.",
                    "Aaj main nashta nahi karunga." to "I will not have breakfast today.",
                    "Main chai me biscuit dubo kar khata hoon." to "I eat biscuits by dipping them in tea.",
                    "Mujhe chai thodi kadak chahiye." to "I need the tea a bit strong.",
                    "Main nashte ke liye late ho raha hoon." to "I am getting late for breakfast.",
                    "Maine abhi tak nashta nahi kiya hai." to "I haven't had breakfast yet.",
                    "Main subah khali pet chai nahi peeta." to "I don't drink tea on an empty stomach in the morning.",
                    "Aaj nashte me main kya khaun?" to "What should I eat for breakfast today?",
                    "Mujhe thandi chai bilkul pasand nahi." to "I don't like cold tea at all.",
                    "Main apna nashta khud banata hoon." to "I make my breakfast myself.",
                    "Main sirf ek cup chai lunga." to "I will take only one cup of tea.",
                    "Mujhe nashte me kuch halka chahiye." to "I need something light for breakfast.",
                    "Main chai chhan raha hoon." to "I am straining the tea.",
                    "Hum roz sath me nashta karte hain." to "We have breakfast together every day.",
                    "Meri chai me cheeni kam hai." to "There is less sugar in my tea.",
                    "Main nashta karke office nikalta hoon." to "I leave for the office after having breakfast.",
                    "Mujhe chai ki aadat pad gayi hai." to "I have gotten used to tea.",
                    "Main chai ubaal raha hoon." to "I am boiling the tea."
                )
                t2_1stPerson.forEach { (hindi, eng) ->
                    dao.insertLessonSentence(LessonSentence(
                        lessonId = t2Id,
                        personTag = "1st Person",
                        promptHindi = hindi,
                        promptQuestion = "Say in English: $hindi",
                        expectedText = eng
                    ))
                }

                val t2_2ndPerson = listOf(
                    "Tum nashte me kya khaoge?" to "What will you eat for breakfast?",
                    "Kya tumne nashta kar liya?" to "Have you had breakfast?",
                    "Tumhe chai me kitni cheeni chahiye?" to "How much sugar do you want in tea?",
                    "Tum subah kitne cup chai peete ho?" to "How many cups of tea do you drink in the morning?",
                    "Apni chai thandi hone se pehle pee lo." to "Drink your tea before it gets cold.",
                    "Tumhe nashta kabhi skip nahi karna chahiye." to "You should never skip breakfast.",
                    "Kya tum mere sath chai pioge?" to "Will you drink tea with me?",
                    "Tum roj poha kyu khate ho?" to "Why do you eat poha every day?",
                    "Tumhare nashte me aaj kya hai?" to "What do you have for breakfast today?",
                    "Tum chai banana jante ho kya?" to "Do you know how to make tea?",
                    "Tum chai bohot zyada peete ho." to "You drink too much tea.",
                    "Aap kya lena pasand karenge, chai ya coffee?" to "What would you like to have, tea or coffee?",
                    "Tum nashta itni jaldi me kyu kha rahe ho?" to "Why are you eating breakfast in such a hurry?",
                    "Apne kapdo par chai mat gira lena." to "Don't spill tea on your clothes.",
                    "Tumhe aisi fiki chai kaise pasand aati hai?" to "How do you like such tasteless tea?",
                    "Kya tum aur parathe loge?" to "Will you take more parathas?",
                    "Tumhe nashta karke bahar jana chahiye." to "You should go out after having breakfast.",
                    "Tumne chai me adrak nahi dali kya?" to "Didn't you put ginger in the tea?",
                    "Aap nashte ke liye kya banwa rahe hain?" to "What are you getting made for breakfast?",
                    "Tum roz nashte me itna nakhra kyu karte ho?" to "Why do you fuss so much over breakfast every day?"
                )
                t2_2ndPerson.forEach { (hindi, eng) ->
                    dao.insertLessonSentence(LessonSentence(
                        lessonId = t2Id,
                        personTag = "2nd Person",
                        promptHindi = hindi,
                        promptQuestion = "Ask in English: $hindi",
                        expectedText = eng
                    ))
                }

                val t2_3rdPerson = listOf(
                    "Wo roz subah chai khud banati hai." to "She makes tea herself every morning.",
                    "Wo nashta karke hi ghar se nikalta hai." to "He leaves the house only after having breakfast.",
                    "Nidhi ko adrak wali chai bohot pasand hai." to "Nidhi likes ginger tea very much.",
                    "Wo chai me biscuit dubokar kha raha hai." to "He is eating by dipping biscuits in tea.",
                    "Wo kabhi subah ka nashta nahi karta." to "He never eats morning breakfast.",
                    "Use fiki chai peene ki aadat hai." to "He has a habit of drinking tea without sugar.",
                    "Wo nashte me sirf fruits khati hai." to "She eats only fruits for breakfast.",
                    "Use chai thodi aur garm chahiye." to "He needs the tea a bit hotter.",
                    "Wo nashta karte waqt phone dekhta hai." to "He looks at his phone while having breakfast.",
                    "Usne abhi tak apni chai kyu nahi pi?" to "Why hasn't he drunk his tea yet?",
                    "Wo log nashte ki table par wait kar rahe hain." to "They are waiting at the breakfast table.",
                    "Use uthne ke baad do cup chai ki zaroorat padti hai." to "He needs two cups of tea after waking up.",
                    "Wo chai banate waqt jal gaya." to "He got burnt while making tea.",
                    "Uska nashta thanda ho raha hai." to "His breakfast is getting cold.",
                    "Wo padosi ke yahan chai peene gaya hai." to "He has gone to the neighbor's house to drink tea.",
                    "Mummy sabke liye nashta bana rahi hain." to "Mom is making breakfast for everyone.",
                    "Use nashte me kachori khana pasand hai." to "He likes to eat kachoris for breakfast.",
                    "Wo nashta karne me bohot time lagata hai." to "He takes a lot of time to have breakfast.",
                    "Usne aaj nashte me kya banaya hai?" to "What has she made for breakfast today?",
                    "Wo chai pi kar turant office nikal gaya." to "He left for the office immediately after drinking tea."
                )
                t2_3rdPerson.forEach { (hindi, eng) ->
                    dao.insertLessonSentence(LessonSentence(
                        lessonId = t2Id,
                        personTag = "3rd Person",
                        promptHindi = hindi,
                        promptQuestion = "Say about third person: $hindi",
                        expectedText = eng
                    ))
                }

                // Topic 3: School & Education 🏫
                val t3Id = dao.insertSpeakingLesson(SpeakingLesson(
                    category = "School",
                    title = "School & Education 🏫",
                    description = "Master phrases used at school, classes, exams, homework, and with teachers.",
                    level = "Beginner",
                    iconName = "School"
                )).toInt()

                val t3_1stPerson = listOf(
                    "Main aaj school ke liye late ho gaya." to "I got late for school today.",
                    "Humari morning prayer shuru ho gayi hai." to "Our morning prayer has started.",
                    "Maine apna homework poora nahi kiya hai." to "I haven't completed my homework.",
                    "Mujhe principal sir se darr lagta hai." to "I am scared of the principal sir.",
                    "Main apna tiffin share nahi karunga." to "I will not share my lunchbox.",
                    "Aaj main tiffin me kachori laya hoon." to "I have brought kachori in my lunchbox today.",
                    "Main tumhe class ke baad WhatsApp karunga." to "I will WhatsApp you after the class.",
                    "Mujhe maths wale sir ka padhane ka style pasand hai." to "I like the teaching style of the maths teacher.",
                    "Main aaj tuition nahi jaunga." to "I will not go to tuition today.",
                    "Maine apni copy check karwa li hai." to "I have got my notebook checked.",
                    "Mujhe samajh nahi aaya jo sir ne padhaya." to "I didn't understand what sir taught.",
                    "Main exam ke liye bohot nervous hoon." to "I am very nervous about the exam.",
                    "Humara pehla period English ka hai." to "Our first period is English.",
                    "Maine black shoes nahi pehne aaj." to "I am not wearing black shoes today.",
                    "Mujhe aaj teacher ne bohot daanta." to "The teacher scolded me a lot today.",
                    "Hum lunch break me ground me khelenge." to "We will play in the ground during the lunch break.",
                    "Maine board se sab copy kar liya hai." to "I have copied everything from the board.",
                    "Main kal school nahi aunga." to "I will not come to school tomorrow.",
                    "Humare science ke naye teacher aaye hain." to "We have got a new science teacher.",
                    "Mujhe apna bag bohot bhari lag raha hai." to "My bag is feeling very heavy to me."
                )
                t3_1stPerson.forEach { (hindi, eng) ->
                    dao.insertLessonSentence(LessonSentence(
                        lessonId = t3Id,
                        personTag = "1st Person",
                        promptHindi = hindi,
                        promptQuestion = "Say in English: $hindi",
                        expectedText = eng
                    ))
                }

                val t3_2ndPerson = listOf(
                    "Tum aaj school late kyu aaye?" to "Why did you come late to school today?",
                    "Kya tumne apna homework kar liya?" to "Have you done your homework?",
                    "Tum apne tiffin me kya laye ho?" to "What have you brought in your lunchbox?",
                    "Tum mujhe homework send kar dena." to "You send me the homework.",
                    "Tumne aaj black shoes kyu nahi pehne?" to "Why haven't you worn black shoes today?",
                    "Tum notes mujhse copy kar lena." to "You copy the notes from me.",
                    "Kya tumhe naye teacher ka padhana samajh aata hai?" to "Do you understand the new teacher's teaching?",
                    "Tum kal tuition kyu nahi aaye the?" to "Why didn't you come to tuition yesterday?",
                    "Tum exam me cheating mat karna." to "Do not cheat in the exam.",
                    "Tumhe principal sir ne office me bulaya hai." to "The principal sir has called you to the office.",
                    "Tum lunch mere sath karoge kya?" to "Will you have lunch with me?",
                    "Tum apni copy check karwane kab jaoge?" to "When will you go to get your notebook checked?",
                    "Tum itni jaldi me kyu likh rahe ho?" to "Why are you writing in such a hurry?",
                    "Tum bench par khade ho jao." to "Stand on the bench.",
                    "Tum kal tiffin me kya laoge?" to "What will you bring in the lunchbox tomorrow?",
                    "Tumhara roll number kya hai?" to "What is your roll number?",
                    "Tum aaj chup kyu baithe ho class me?" to "Why are you sitting quietly in the class today?",
                    "Kya tumhe homework na karne par punishment mili?" to "Did you get punished for not doing the homework?",
                    "Tumne exam ke liye taiyari kar li?" to "Have you prepared for the exam?",
                    "Tum apna pen mujhe thodi der ke liye de do." to "Give me your pen for a while."
                )
                t3_2ndPerson.forEach { (hindi, eng) ->
                    dao.insertLessonSentence(LessonSentence(
                        lessonId = t3Id,
                        personTag = "2nd Person",
                        promptHindi = hindi,
                        promptQuestion = "Ask in English: $hindi",
                        expectedText = eng
                    ))
                }

                val t3_3rdPerson = listOf(
                    "Wo aaj bina homework kiye aaya hai." to "He has come without doing his homework today.",
                    "Teacher ne usko class me bohot daanta." to "The teacher scolded him a lot in the class.",
                    "Rahul apna tiffin kisi se share nahi karta." to "Rahul doesn't share his lunchbox with anyone.",
                    "Nidhi ko principal office jana pada." to "Nidhi had to go to the principal's office.",
                    "Naye teacher ka padhane ka tarika accha hai." to "The new teacher's teaching method is good.",
                    "Wo hamesha exam me top karta hai." to "He always tops in the exam.",
                    "Usne mujhe class ke baad WhatsApp nahi kiya." to "He didn't WhatsApp me after the class.",
                    "Uski copy abhi tak check nahi hui hai." to "His notebook hasn't been checked yet.",
                    "Wo prayer me line tod raha tha." to "He was breaking the line in the prayer.",
                    "Wo tuition me bilkul padhai nahi karta." to "He doesn't study at all in tuition.",
                    "Sir ne uski copy gusse me faad di." to "Sir tore his notebook in anger.",
                    "Wo aaj pt shoes pehan kar aaya hai." to "He has come wearing PT shoes today.",
                    "Usko bina shoes pehne class ke bahar nikal diya gaya." to "He was thrown out of the class for not wearing shoes.",
                    "Wo piche wali bench par baatein kar raha tha." to "He was talking on the back bench.",
                    "Uski handwriting class me sabse achi hai." to "His handwriting is the best in the class."
                )
                t3_3rdPerson.forEach { (hindi, eng) ->
                    dao.insertLessonSentence(LessonSentence(
                        lessonId = t3Id,
                        personTag = "3rd Person",
                        promptHindi = hindi,
                        promptQuestion = "Say about third person: $hindi",
                        expectedText = eng
                    ))
                }

                // Topic 4: Office & Work 💼
                val t4Id = dao.insertSpeakingLesson(SpeakingLesson(
                    category = "Office",
                    title = "Office & Corporate Daily Work 💼",
                    description = "Master corporate conversations, emails, meetings, targets, deadlines, and boss dialogues.",
                    level = "Intermediate",
                    iconName = "Work"
                )).toInt()

                val t4_1stPerson = listOf(
                    "Main apna system login kar raha hoon." to "I am logging into my system.",
                    "Maine sabhi zaroori emails ka reply kar diya hai." to "I have replied to all important emails.",
                    "Mujhe aaj ek presentation deni hai." to "I have to give a presentation today.",
                    "Main abhi ek meeting me hoon." to "I am in a meeting right now.",
                    "Mera aaj ka target poora ho gaya." to "My today's target is complete.",
                    "Mujhe boss ne cabin me bulaya hai." to "The boss has called me in the cabin.",
                    "Main lunch break ke baad is file par kaam karunga." to "I will work on this file after the lunch break.",
                    "Main aaj thoda jaldi log out karunga." to "I will log out a bit early today.",
                    "Mujhe is project ke liye extra time chahiye." to "I need extra time for this project.",
                    "Main kal chhutti par rahunga." to "I will be on leave tomorrow.",
                    "Maine HR ko apna leave email bhej diya hai." to "I have sent my leave email to HR.",
                    "Mujhe meri salary slip abhi tak nahi mili." to "I haven't received my salary slip yet.",
                    "Main is report ko review kar raha hoon." to "I am reviewing this report.",
                    "Hum coffee machine ke paas milte hain." to "Let's meet near the coffee machine.",
                    "Maine apna ID card desk par chhod diya." to "I left my ID card on the desk.",
                    "Mujhe internet connection me problem aa rahi hai." to "I am facing an issue with the internet connection.",
                    "Main aaj ghar se kaam kar raha hoon." to "I am working from home today."
                )
                t4_1stPerson.forEach { (hindi, eng) ->
                    dao.insertLessonSentence(LessonSentence(
                        lessonId = t4Id,
                        personTag = "1st Person",
                        promptHindi = hindi,
                        promptQuestion = "Say in English: $hindi",
                        expectedText = eng
                    ))
                }

                val t4_2ndPerson = listOf(
                    "Kya tumne wo report send kar di?" to "Have you sent that report?",
                    "Tum aaj meeting me kyu nahi aaye?" to "Why didn't you attend the meeting today?",
                    "Apna system lock karke jaya karo." to "Always lock your system before leaving.",
                    "Tumhara appraisal kaisa raha?" to "How was your appraisal?",
                    "Kya aap meri is excel sheet me madad kar sakte hain?" to "Can you help me with this excel sheet?",
                    "Tumhe client se kab baat karni hai?" to "When do you have to talk to the client?",
                    "Tum aaj itne pareshan kyu lag rahe ho?" to "Why are you looking so stressed today?",
                    "Apni timesheet aaj hi bhar dena." to "Fill your timesheet today itself.",
                    "Tumhara is project me kya role hai?" to "What is your role in this project?",
                    "Kya tumhe boss ka email mila?" to "Did you receive the boss's email?",
                    "Tum lunch me aaj kya laye ho?" to "What have you brought for lunch today?",
                    "Tumhe kal time par office aana hoga." to "You will have to come to the office on time tomorrow.",
                    "Kya aap thodi der ke liye apna pen de sakte hain?" to "Can you lend me your pen for a while?",
                    "Tum itni jaldi log out kyu kar rahe ho?" to "Why are you logging out so early?",
                    "Tumhe HR manager se milna chahiye." to "You should meet the HR manager.",
                    "Apna laptop IT department me dikha lo." to "Get your laptop checked in the IT department.",
                    "Tum is task ko kab tak poora kar loge?" to "By when will you complete this task?"
                )
                t4_2ndPerson.forEach { (hindi, eng) ->
                    dao.insertLessonSentence(LessonSentence(
                        lessonId = t4Id,
                        personTag = "2nd Person",
                        promptHindi = hindi,
                        promptQuestion = "Ask in English: $hindi",
                        expectedText = eng
                    ))
                }

                val t4_3rdPerson = listOf(
                    "Boss aaj bohot gusse me hain." to "The boss is very angry today.",
                    "Priya aaj office nahi aayi hai." to "Priya hasn't come to the office today.",
                    "Wo apni desk par nahi hai." to "He is not at his desk.",
                    "Naya employee kal se join karega." to "The new employee will join from tomorrow.",
                    "IT team ne server theek kar diya hai." to "The IT team has fixed the server.",
                    "Usne kal resignation letter de diya." to "He submitted his resignation letter yesterday.",
                    "Manager sabki performance review kar rahe hain." to "The manager is reviewing everyone's performance.",
                    "Uska promotion ho gaya hai." to "He has got a promotion.",
                    "Client ne deadline badha di hai." to "The client has extended the deadline.",
                    "Wo hamesha coffee break par rehta hai." to "He is always on a coffee break.",
                    "HR team aaj ek event organize kar rahi hai." to "The HR team is organizing an event today.",
                    "Usne galti se sabko email bhej diya." to "He accidentally sent the email to everyone.",
                    "Guard ne main gate lock kar diya hai." to "The guard has locked the main gate.",
                    "Cleaning staff abhi meeting room saaf kar raha hai." to "The cleaning staff is cleaning the meeting room right now.",
                    "Wo apne targets kabhi poore nahi karta." to "He never completes his targets.",
                    "Team leader kal project ki presentation denge." to "The team leader will give the project presentation tomorrow."
                )
                t4_3rdPerson.forEach { (hindi, eng) ->
                    dao.insertLessonSentence(LessonSentence(
                        lessonId = t4Id,
                        personTag = "3rd Person",
                        promptHindi = hindi,
                        promptQuestion = "Say about third person: $hindi",
                        expectedText = eng
                    ))
                }

                // Topic 5: Travel, Transport & Commute 🎒🚗
                val t5Id = dao.insertSpeakingLesson(SpeakingLesson(
                    category = "Travel & Commute",
                    title = "Travel, Transport & Commute 🎒🚗",
                    description = "Master phrases for traffic, keys, driving, auto, buses, trains, and daily commute.",
                    level = "Beginner",
                    iconName = "DirectionsCar"
                )).toInt()

                val t5_1stPerson = listOf(
                    "Main apna bag pack kar raha hoon." to "I am packing my bag.",
                    "Mujhe meri car ki chabi nahi mil rahi." to "I can't find my car keys.",
                    "Main office ke liye late ho raha hoon." to "I am getting late for the office.",
                    "Main aaj apni bike se office jaunga." to "I will go to the office by my bike today.",
                    "Mera tiffin kahan rakha hai?" to "Where is my lunchbox kept?",
                    "Main apne joote pehan raha hoon." to "I am putting on my shoes.",
                    "Mujhe aaj jaldi nikalna padega." to "I will have to leave early today.",
                    "Main traffic me fasa hua hoon." to "I am stuck in traffic.",
                    "Hum school bus ka wait kar rahe hain." to "We are waiting for the school bus.",
                    "Main apni id card bhool gaya." to "I forgot my ID card.",
                    "Mujhe auto leni padegi aaj." to "I will have to take an auto today.",
                    "Main apni botal bhar raha hoon." to "I am filling my bottle.",
                    "Main baccho ko chhodne ja raha hoon." to "I am going to drop the kids.",
                    "Mera laptop bag gaadi me rakh do." to "Put my laptop bag in the car.",
                    "Main bas nikal hi raha hoon." to "I am just leaving.",
                    "Mujhe aaj ek zaruri meeting attend karni hai." to "I have to attend an important meeting today.",
                    "Main aaj metro se jaunga." to "I will go by metro today.",
                    "Meri shirt par daag lag gaya hai." to "I got a stain on my shirt.",
                    "Main apna wallet check kar raha hoon." to "I am checking my wallet.",
                    "Mujhe apna project school me submit karna hai." to "I have to submit my project in school."
                )
                t5_1stPerson.forEach { (hindi, eng) ->
                    dao.insertLessonSentence(LessonSentence(
                        lessonId = t5Id,
                        personTag = "1st Person",
                        promptHindi = hindi,
                        promptQuestion = "Say in English: $hindi",
                        expectedText = eng
                    ))
                }

                val t5_2ndPerson = listOf(
                    "Tumne apna bag pack kar liya kya?" to "Have you packed your bag?",
                    "Jaldi karo, school bus aane wali hai." to "Hurry up, the school bus is about to arrive.",
                    "Tumhare joote kahan hain?" to "Where are your shoes?",
                    "Aaj office me koi meeting hai kya?" to "Is there any meeting in the office today?",
                    "Apni paani ki botal sath le jao." to "Take your water bottle with you.",
                    "Tum roz late kyu ho jate ho?" to "Why do you get late every day?",
                    "Apna tiffin table par se utha lo." to "Pick up your lunchbox from the table.",
                    "Tum aaj kis gaadi se jaoge?" to "Which vehicle will you go by today?",
                    "Tumne apni id card pehni hai?" to "Are you wearing your ID card?",
                    "Apne baal thik se banao." to "Comb your hair properly.",
                    "Tumhara homework poora ho gaya?" to "Is your homework complete?",
                    "Tum chhatri le jana mat bhoolna." to "Don't forget to carry an umbrella.",
                    "Mujhe bata dena jab tum office pahunch jao." to "Let me know when you reach the office.",
                    "Tum aaj jaldi wapas aoge kya?" to "Will you come back early today?",
                    "Apne joote ke feete bandh lo." to "Tie your shoelaces.",
                    "Tum apni geometry box rakhna bhool gaye." to "You forgot to keep your geometry box.",
                    "Kya tumhe station tak chhod du?" to "Should I drop you to the station?",
                    "Tumhare shirt ki button khuli hai." to "Your shirt button is open.",
                    "Tum phone charge karna bhool gaye kya?" to "Did you forget to charge your phone?",
                    "Dhyan se road cross karna." to "Cross the road carefully."
                )
                t5_2ndPerson.forEach { (hindi, eng) ->
                    dao.insertLessonSentence(LessonSentence(
                        lessonId = t5Id,
                        personTag = "2nd Person",
                        promptHindi = hindi,
                        promptQuestion = "Ask in English: $hindi",
                        expectedText = eng
                    ))
                }

                val t5_3rdPerson = listOf(
                    "Bacche school jane ke liye taiyar hain." to "The kids are ready to go to school.",
                    "Wo apna tiffin pack kar rahi hai." to "She is packing her lunchbox.",
                    "Nidhi office ke liye nikal chuki hai." to "Nidhi has already left for the office.",
                    "Uska bus abhi nahi aaya hai." to "His bus hasn't arrived yet.",
                    "Wo roz apna ID card bhool jata hai." to "He forgets his ID card every day.",
                    "Wo apne joote ke feete khud bandhti hai." to "She ties her shoelaces herself.",
                    "Aaj wo bahut late utha." to "He woke up very late today.",
                    "Unhe traffic me fasne ki aadat hai." to "They are used to getting stuck in traffic.",
                    "Wo apna bag hamesha bhari rakhta hai." to "He always keeps his bag heavy.",
                    "Usko aaj half-day mil gaya." to "He got a half-day today.",
                    "Uske papa use chhodne jayenge." to "His father will go to drop him.",
                    "Wo stairs se tez bhag kar niche gaya." to "He ran down the stairs quickly.",
                    "Usne apni project file ghar pe chhod di." to "He left his project file at home.",
                    "Wo gate par dosto ka wait kar raha hai." to "He is waiting for his friends at the gate.",
                    "Uski class 8 baje shuru hoti hai." to "His class starts at 8 o'clock."
                )
                t5_3rdPerson.forEach { (hindi, eng) ->
                    dao.insertLessonSentence(LessonSentence(
                        lessonId = t5Id,
                        personTag = "3rd Person",
                        promptHindi = hindi,
                        promptQuestion = "Say about third person: $hindi",
                        expectedText = eng
                    ))
                }

                // Topic 6: Food, Fruits & Vegetables 🍎🥦
                val t6Id = dao.insertSpeakingLesson(SpeakingLesson(
                    category = "Food & Veggies",
                    title = "Food, Fruits & Vegetables 🍎🥦",
                    description = "Practice English phrases about vegetables, fruits, recipes, cooking, and daily meals.",
                    level = "Beginner",
                    iconName = "Restaurant"
                )).toInt()

                val t6_1stPerson = listOf(
                    "Main aaj aloo ki sabji banaunga." to "I will make potato curry today.",
                    "Fridge me kuch bhi nahi hai, main kya pakaun?" to "There is nothing in the fridge, what should I cook?",
                    "Mujhe bhindi ki sabji bilkul pasand nahi." to "I don't like okra curry at all.",
                    "Main market se taze tamatar la raha hoon." to "I am bringing fresh tomatoes from the market.",
                    "Hum aaj raat bahar khana khayenge." to "We will eat out tonight.",
                    "Main gajar ka halwa bana rahi hoon." to "I am making carrot pudding.",
                    "Mujhe bhookh lag rahi hai, main kela khaunga." to "I am feeling hungry, I will eat a banana.",
                    "Maine aaj baingan ka bharta banaya hai." to "I have made eggplant mash today.",
                    "Hum nashte me aam kha rahe hain." to "We are eating mangoes for breakfast.",
                    "Main palak paneer kha kar thak gaya hoon." to "I am tired of eating spinach paneer.",
                    "Mujhe seb khana bohot pasand hai." to "I love eating apples very much.",
                    "Main angoor dho kar la raha hoon." to "I am bringing grapes after washing them.",
                    "Hum roz ek santra khate hain." to "We eat an orange every day.",
                    "Main pyaaz chheel raha hoon." to "I am peeling onions.",
                    "Mujhe shimla mirch ka swaad accha lagta hai." to "I like the taste of capsicum.",
                    "Main tarbooz kaatne ja raha hoon." to "I am going to cut the watermelon.",
                    "Maine aaj matar paneer banaya hai." to "I have made peas paneer today.",
                    "Main aaj lauki ki sabji nahi khaunga." to "I will not eat bottle gourd curry today.",
                    "Mujhe kaddu thoda meetha lagta hai." to "I find pumpkin a bit sweet.",
                    "Main mooli ka paratha bana raha hoon." to "I am making radish paratha."
                )
                t6_1stPerson.forEach { (hindi, eng) ->
                    dao.insertLessonSentence(LessonSentence(
                        lessonId = t6Id,
                        personTag = "1st Person",
                        promptHindi = hindi,
                        promptQuestion = "Say in English: $hindi",
                        expectedText = eng
                    ))
                }

                val t6_2ndPerson = listOf(
                    "Tum aaj khane me kya banaoge?" to "What will you make for meals today?",
                    "Kya tumne aloo ubaal diye hain?" to "Have you boiled the potatoes?",
                    "Tum sabji me pyaaz kyu nahi dalte?" to "Why don't you put onions in the curry?",
                    "Fridge check karo, kya usme tamatar hain?" to "Check the fridge, are there tomatoes in it?",
                    "Tumhe gobi ki sabji kaisi lagti hai?" to "How do you like cauliflower curry?",
                    "Tum roz karela kyu banate ho?" to "Why do you make bitter gourd every day?",
                    "Kya tum mere liye ek kela la sakte ho?" to "Can you bring a banana for me?",
                    "Tumhe gajar kaccha khana pasand hai kya?" to "Do you like eating raw carrots?",
                    "Tum baingan kyu nahi khate?" to "Why don't you eat eggplant?",
                    "Tum aam kaat kar fridge me rakh do." to "You cut the mango and keep it in the fridge.",
                    "Tum raat ko chawal mat khaya karo." to "You shouldn't eat rice at night.",
                    "Tumhe seb chheel kar khana chahiye." to "You should eat the apple after peeling it.",
                    "Tum tarbooz ke beej kyu nahi nikalte?" to "Why don't you remove the watermelon seeds?",
                    "Kya tum thode angoor khaoge?" to "Will you eat some grapes?",
                    "Tum shimla mirch choti choti kaatna." to "You chop the capsicum very small.",
                    "Tum khane me namak hamesha tez rakhte ho." to "You always keep the salt high in the food.",
                    "Tumne palak theek se nahi dhoya." to "You didn't wash the spinach properly.",
                    "Kya tumhe lauki ka juice peena hai?" to "Do you want to drink bottle gourd juice?",
                    "Tum santra kha kar chilka phek dena." to "You throw the peel after eating the orange.",
                    "Tum sabji me nimbu kyu daal rahe ho?" to "Why are you putting lemon in the curry?"
                )
                t6_2ndPerson.forEach { (hindi, eng) ->
                    dao.insertLessonSentence(LessonSentence(
                        lessonId = t6Id,
                        personTag = "2nd Person",
                        promptHindi = hindi,
                        promptQuestion = "Ask in English: $hindi",
                        expectedText = eng
                    ))
                }

                val t6_3rdPerson = listOf(
                    "Mummy aaj chole bhature bana rahi hain." to "Mom is making chole bhature today.",
                    "Wo kabhi bhi karela nahi khata." to "He never eats bitter gourd.",
                    "Usne poora tarbooz akele kha liya." to "He ate the whole watermelon alone.",
                    "Nidhi ne aam ka juice banaya hai." to "Nidhi has made mango juice.",
                    "Wo palak dekh kar muh banata hai." to "He makes a face after seeing spinach.",
                    "Usne fridge me bacha hua khana rakh diya hai." to "He has kept the leftover food in the fridge.",
                    "Wo gajar aur mooli ka salad kha rahi hai." to "She is eating carrot and radish salad.",
                    "Bacche ne seb adha chhod diya." to "The kid left the apple half-eaten.",
                    "Wo sabji me bohot mirchi dalta hai." to "He puts a lot of chili in the curry.",
                    "Usne lauki ki sabji me pani zyada daal diya." to "He put too much water in the bottle gourd curry.",
                    "Papa bazaar se anar aur santra laye hain." to "Dad has brought pomegranates and oranges from the market.",
                    "Wo roz angoor khata hai." to "He eats grapes every day.",
                    "Usne gobi ka paratha banaya tha kal." to "He had made cauliflower paratha yesterday.",
                    "Wo bina aloo ke koi sabji nahi khata." to "He doesn't eat any curry without potatoes.",
                    "Usne bhindi jalakar kharab kar di." to "He ruined the okra by burning it."
                )
                t6_3rdPerson.forEach { (hindi, eng) ->
                    dao.insertLessonSentence(LessonSentence(
                        lessonId = t6Id,
                        personTag = "3rd Person",
                        promptHindi = hindi,
                        promptQuestion = "Say about third person: $hindi",
                        expectedText = eng
                    ))
                }

                // Topic 7: Back Home from School & Office 🏠🎒
                val t7Id = dao.insertSpeakingLesson(SpeakingLesson(
                    category = "Back Home",
                    title = "Back Home from School & Office 🏠🎒",
                    description = "Master daily English phrases when returning home from school, office, homework & tiredness.",
                    level = "Beginner",
                    iconName = "Home"
                )).toInt()

                val t7_1stPerson = listOf(
                    "Main aaj school me gir gaya tha." to "I fell down in the school today.",
                    "Mujhe bohot bhookh lagi hai, khane me kya hai?" to "I am very hungry, what is there to eat?",
                    "Main aaj office me bohot thak gaya." to "I got very tired in the office today.",
                    "Mera aaj boss se jhagda ho gaya." to "I had a fight with my boss today.",
                    "Main apna lunch box school me bhool aaya." to "I forgot my lunch box at school.",
                    "Mujhe nind aa rahi hai, main thodi der sounga." to "I am feeling sleepy, I will sleep for a while.",
                    "Humara kal maths ka test hai." to "We have a maths test tomorrow.",
                    "Main aaj tiffin poora khatam kar ke aaya hoon." to "I have come after finishing my tiffin completely today.",
                    "Mujhe aaj teacher ne 'Good' diya." to "The teacher gave me a 'Good' today.",
                    "Main apne kapde khud change karunga." to "I will change my clothes myself.",
                    "Main thodi der TV dekhna chahta hoon." to "I want to watch TV for a while.",
                    "Mera bag bohot bhari tha aaj." to "My bag was very heavy today.",
                    "Main aaj dosto ke sath cricket khela." to "I played cricket with my friends today.",
                    "Mujhe office me ek naya project mila hai." to "I have got a new project in the office.",
                    "Main kal se thoda jaldi office jaunga." to "I will go to the office a little early from tomorrow.",
                    "Humare principal ne aaj sabko danta." to "Our principal scolded everyone today.",
                    "Main aaj traffic me bohot der fasa raha." to "I was stuck in traffic for a long time today.",
                    "Mujhe aaj bohot saara homework mila hai." to "I have got a lot of homework today.",
                    "Maine aaj raste me samosa khaya tha." to "I ate a samosa on the way today.",
                    "Main sham ko park me khelne jaunga." to "I will go to play in the park in the evening.",
                    "Mujhe kal P.T. dress pehan kar jani hai." to "I have to wear the P.T. dress tomorrow.",
                    "Hum log aaj jaldi chhutti milne se khush the." to "We were happy getting an early off today.",
                    "Main nahane ja raha hoon paani garm kar do." to "I am going to bathe, heat the water.",
                    "Mera aaj padhne ka bilkul mann nahi hai." to "I don't feel like studying at all today.",
                    "Maine aate hi apna homework khatam kar liya." to "I finished my homework as soon as I came."
                )
                t7_1stPerson.forEach { (hindi, eng) ->
                    dao.insertLessonSentence(LessonSentence(
                        lessonId = t7Id,
                        personTag = "1st Person",
                        promptHindi = hindi,
                        promptQuestion = "Say in English: $hindi",
                        expectedText = eng
                    ))
                }

                val t7_2ndPerson = listOf(
                    "Tu school dress me kab tak rahega, jaldi utar de!" to "How long will you stay in your school uniform, take it off quickly!",
                    "Tune kal fir homework nahi kiya tha, teacher ka phone aaya tha." to "You didn't do your homework again yesterday, the teacher had called.",
                    "Aaj aap bohot thake thake lag rahe hain." to "You are looking very tired today.",
                    "Beta kya hua, office me kuch hua hai kya?" to "Son what happened, did something happen at the office?",
                    "Tera muh kyu latka hua hai?" to "Why are you pulling a long face?",
                    "Tumne aaj tiffin poora kyu nahi khaya?" to "Why didn't you eat your whole tiffin today?",
                    "Apne joote wahan rack par theek se rakho." to "Keep your shoes properly on the rack there.",
                    "Tumhare kapdo par ye mitti kaise lagi?" to "How did this dirt get on your clothes?",
                    "Jao pehle hath muh dho kar aao, fir khana dungi." to "Go wash your hands and face first, then I will give you food.",
                    "Tumhe aaj kitna homework mila hai?" to "How much homework did you get today?",
                    "Aaj school me kya kya padhaya gaya?" to "What all was taught in school today?",
                    "Tumhara aaj ka din kaisa raha office me?" to "How was your day at the office today?",
                    "Aap chai lenge ya thanda paani laun?" to "Will you have tea or should I bring cold water?",
                    "Tum subah apna pen table par hi chhod gaye the." to "You had left your pen on the table itself in the morning.",
                    "Tum apni bottle school me kyu chhod aaye?" to "Why did you leave your bottle at school?",
                    "Tumhe bhookh lagi hai kya?" to "Are you hungry?",
                    "Apna school bag sofe par mat pheko." to "Don't throw your school bag on the sofa.",
                    "Aaj boss ne kuch bola kya tumhe?" to "Did the boss say anything to you today?",
                    "Tum itni jaldi kyu aa gaye aaj?" to "Why did you come so early today?",
                    "Tum ro kyu rahe ho, kisi ne mara kya?" to "Why are you crying, did someone hit you?",
                    "Tumhare baal itne bikhre hue kyu hain?" to "Why is your hair so messy?",
                    "Tum aaj dosto ke sath kyu nahi khele?" to "Why didn't you play with your friends today?",
                    "Tumhe pata hai kal tumhari PTM hai?" to "Do you know you have a PTM tomorrow?",
                    "Aaj jaldi so jana, kal test hai tumhara." to "Sleep early today, you have a test tomorrow.",
                    "Aap barish me bheeg kyu gaye, chhatri nahi thi?" to "Why did you get wet in the rain, didn't you have an umbrella?",
                    "Tum aaj tuition kitne baje jaoge?" to "What time will you go to tuition today?",
                    "Tumne aaj diary me sign kyu nahi karwaye?" to "Why didn't you get the diary signed today?",
                    "Tum apni tie utar kar theek se kyu nahi rakhte?" to "Why don't you take off your tie and keep it properly?",
                    "Tum lunch me sabji fir chhod aaye." to "You left the vegetable in your lunch again.",
                    "Tum aaj office se seedha ghar kyu nahi aaye?" to "Why didn't you come straight home from the office today?"
                )
                t7_2ndPerson.forEach { (hindi, eng) ->
                    dao.insertLessonSentence(LessonSentence(
                        lessonId = t7Id,
                        personTag = "2nd Person",
                        promptHindi = hindi,
                        promptQuestion = "Ask in English: $hindi",
                        expectedText = eng
                    ))
                }

                val t7_3rdPerson = listOf(
                    "Wo ghar aate hi TV chalu kar deta hai." to "He turns on the TV as soon as he comes home.",
                    "Aaj Nidhi ka office me bohot bura din tha." to "Today was a very bad day for Nidhi at the office.",
                    "Uska aaj math ka paper bekar gaya." to "His math paper went bad today.",
                    "Papa aaj bohot gusse me office se aaye hain." to "Dad has come from the office very angry today.",
                    "Bhaiya ne aate hi khana manga." to "Brother asked for food as soon as he came.",
                    "Uske boss ne use meeting me danta." to "His boss scolded him in the meeting.",
                    "Wo apna sara lunch dosto ko de deta hai." to "He gives all his lunch to his friends.",
                    "Uski shirt ka button school me toot gaya." to "His shirt button broke in school.",
                    "Wo hamesha apne joote idhar udhar phek deti hai." to "She always throws her shoes here and there.",
                    "Didi aaj college se bohot thak kar aayi hain." to "Sister has come from college very tired today.",
                    "Use aaj khelte waqt chot lag gayi." to "He got hurt while playing today.",
                    "Uska promotion ruk gaya hai isliye udas hai." to "His promotion has been stopped, that's why he is sad.",
                    "Wo bag rakhte hi khelne bhag gaya." to "He ran to play as soon as he put the bag down.",
                    "Nidhi ke office me aaj koi party thi." to "There was some party in Nidhi's office today.",
                    "Wo kal se naye school me jayega." to "He will go to a new school from tomorrow.",
                    "Uske teacher hamesha uski shikayat karte hain." to "His teacher always complains about him.",
                    "Aaj wo bina roye school se wapas aaya hai." to "Today he has returned from school without crying.",
                    "Wo aaj meeting ki वजह se late aayi." to "She came late today because of the meeting.",
                    "Wo school bus me thak kar so gaya tha." to "He had fallen asleep in the school bus due to tiredness.",
                    "Usne aaj tiffin me kuch nahi khaya." to "He ate nothing in his tiffin today.",
                    "Uska aaj ka test bohot accha gaya." to "His test today went very well.",
                    "Wo roz school se aakar nakhre karti hai." to "She throws tantrums every day after coming from school.",
                    "Papa aaj office se jaldi aa gaye." to "Dad came early from the office today.",
                    "Use aaj koi homework nahi mila hai." to "He hasn't got any homework today.",
                    "Wo aaj khush lag raha hai, shayad boss ne tareef ki." to "He is looking happy today, maybe the boss praised him."
                )
                t7_3rdPerson.forEach { (hindi, eng) ->
                    dao.insertLessonSentence(LessonSentence(
                        lessonId = t7Id,
                        personTag = "3rd Person",
                        promptHindi = hindi,
                        promptQuestion = "Say about third person: $hindi",
                        expectedText = eng
                    ))
                }

                // Topic 8: Defects & Home Repairs 🔧📺
                val t8Id = dao.insertSpeakingLesson(SpeakingLesson(
                    category = "Home Repairs",
                    title = "Defects & Home Repairs 🔧📺",
                    description = "Learn phrases about broken appliances, repairs, mechanics, tools, and fixing stuff at home.",
                    level = "Intermediate",
                    iconName = "Build"
                )).toInt()

                val t8_1stPerson = listOf(
                    "Main kal nayi TV lene ki soch raha hoon." to "I am thinking of buying a new TV tomorrow.",
                    "Maine pechkas table par hi rakha tha." to "I had kept the screwdriver on the table itself.",
                    "Main diwar par keel thok raha hoon." to "I am hammering a nail into the wall.",
                    "Mujhe lagta hai TV ki screen kharab ho gayi hai." to "I think the TV screen is ruined.",
                    "Main is kharab pankhe ko mechanic ke paas le jaunga." to "I will take this broken fan to the mechanic.",
                    "Maine TV ka remote nahi giraya!" to "I didn't drop the TV remote!",
                    "Hum scenery lagane ke liye hathodi dhund rahe hain." to "We are looking for a hammer to hang the scenery.",
                    "Main market ja kar naya bulb la raha hoon." to "I am going to the market to bring a new bulb.",
                    "Mujhe ye plaas pakda dena." to "Hand me these pliers.",
                    "Main switch off karna bhool gaya tha." to "I had forgotten to switch it off.",
                    "Main naya iron le aaunga, ye poori tarah jal chuka hai." to "I will bring a new iron, this one is completely burnt.",
                    "Mujhe pata tha ki ye mixer kal hi kharab ho gaya tha." to "I knew that this mixer had broken down yesterday itself.",
                    "Hum is jale hue bulb ko abhi badal denge." to "We will replace this burnt bulb right now.",
                    "Main is pipe ko theek karne ki koshish kar raha hoon." to "I am trying to fix this pipe.",
                    "Maine washing machine me kapde nahi fasaye hain!" to "I haven't stuck clothes in the washing machine!",
                    "Mujhe thoda lamba wire chahiye isko jodne ke liye." to "I need a slightly longer wire to connect this.",
                    "Main pankha khud theek kar lunga, mechanic mat bulao." to "I will fix the fan myself, don't call a mechanic.",
                    "Hum TV theek karwane ke bajaye naya TV le lete hain." to "Instead of getting the TV fixed, let's buy a new TV.",
                    "Maine drill machine se chhed kar diya hai." to "I have made a hole with the drill machine.",
                    "Mujhe bas ek choti keel ki zaroorat hai." to "I just need one small nail.",
                    "Main ye toota hua kaanch saaf kar raha hoon." to "I am cleaning this broken glass.",
                    "Maine fridge ka darwaza zor se band nahi kiya!" to "I didn't close the fridge door forcefully!",
                    "Hum aaj andhere me hi khana khayenge kya?" to "Will we eat food in the dark today?",
                    "Main kal mechanic ko ghar par bula lunga." to "I will call the mechanic home tomorrow.",
                    "Maine fuse wire theek kar diya hai." to "I have fixed the fuse wire.",
                    "Mujhe thoda tape aur kainchi laakar do." to "Bring me some tape and scissors.",
                    "Main ye purana saman kabaadi wale ko de dunga." to "I will give this old stuff to the scrap dealer."
                )
                t8_1stPerson.forEach { (hindi, eng) ->
                    dao.insertLessonSentence(LessonSentence(
                        lessonId = t8Id,
                        personTag = "1st Person",
                        promptHindi = hindi,
                        promptQuestion = "Say in English: $hindi",
                        expectedText = eng
                    ))
                }

                val t8_2ndPerson = listOf(
                    "Bhaiya, tumne hi kharab kiya hoga pakka, mujhe pata hai!" to "Brother, you must have definitely broken it, I know!",
                    "Tumne pechkas kahan chhupa diya?" to "Where did you hide the screwdriver?",
                    "Chalo jao market aur ye bulb lekar aao." to "Come on, go to the market and bring this bulb.",
                    "Tum ye hathodi dhyan se chalana." to "You use this hammer carefully.",
                    "Tumhe hamesha cheezein todne ki aadat hai." to "You always have a habit of breaking things.",
                    "Aap TV sudharwa lao ya fir hum naya TV lele kya?" to "You get the TV fixed or else should we buy a new TV?",
                    "Tumne remote paani me gira diya kya?" to "Did you drop the remote in the water?",
                    "Tum is deewar par scenery kyu nahi laga dete?" to "Why don't you hang the scenery on this wall?",
                    "Tumne zarur mixer me zaroorat se zyada load daala hoga." to "You must have definitely put more load than necessary in the mixer.",
                    "Tum plaas laaye bina wire kaise katoge?" to "How will you cut the wire without bringing pliers?",
                    "Tumhe pata tha ye socket kharab hai, fir bhi tumne charger lagaya!" to "You knew this socket is broken, still you plugged the charger!",
                    "Tum akele pankha theek mat karna, current lag jayega." to "Don't fix the fan alone, you will get an electric shock.",
                    "Tum roz fridge itni der tak khula kyu chhodte ho?" to "Why do you leave the fridge open for so long every day?",
                    "Kya tumne mechanic ko call kiya?" to "Did you call the mechanic?",
                    "Tum is keel ko thoda seedha thoko." to "You hammer this nail a bit straight.",
                    "Tum ye wire galat switch me laga rahe ho." to "You are plugging this wire into the wrong switch.",
                    "Tumhara phone screen fir se toot gaya kya?" to "Did your phone screen break again?",
                    "Aap jara is bulb ko holder me laga denge?" to "Will you please fit this bulb in the holder?",
                    "Tum kal TV theek karwane kab jaoge?" to "When will you go to get the TV fixed tomorrow?",
                    "Tumne pakka AC ka temperature galat set kiya hoga." to "You must have definitely set the wrong AC temperature.",
                    "Tum bas khade rahoge ya meri thodi madad karoge?" to "Will you just keep standing or will you help me a little?",
                    "Tumne hathodi kal raat kahan rakhi thi?" to "Where did you keep the hammer last night?",
                    "Tum naya geyser kharidne market chaloge kya?" to "Will you go to the market to buy a new geyser?",
                    "Tumhe drill machine chalana aati bhi hai?" to "Do you even know how to operate a drill machine?",
                    "Tum hamesha dusro par blame dalte ho jabki galti tumhari hoti hai." to "You always blame others when the fault is yours.",
                    "Tum ye kharab ghadi kab theek karwaoge?" to "When will you get this broken clock fixed?",
                    "Tum apni cycle kharab hone par khud theek karte ho." to "You fix your cycle yourself when it breaks down."
                )
                t8_2ndPerson.forEach { (hindi, eng) ->
                    dao.insertLessonSentence(LessonSentence(
                        lessonId = t8Id,
                        personTag = "2nd Person",
                        promptHindi = hindi,
                        promptQuestion = "Ask in English: $hindi",
                        expectedText = eng
                    ))
                }

                val t8_3rdPerson = listOf(
                    "Papa TV theek karne ki koshish kar rahe hain." to "Dad is trying to fix the TV.",
                    "Usne poora pankha khol kar neeche rakh diya." to "He opened the entire fan and kept it down.",
                    "Chhotu ne kal raat TV ka remote tod diya." to "Chhotu broke the TV remote last night.",
                    "Usne gusse me phone zameen par patak diya." to "He slammed the phone on the floor in anger.",
                    "Nidhi kal mechanic ko bula kar layegi." to "Nidhi will call and bring the mechanic tomorrow.",
                    "Wo pechkas se laptop kholne me laga hai." to "He is busy opening the laptop with a screwdriver.",
                    "Usne bina dekhe keel thoki aur diwar kharab kar di." to "He hammered the nail without looking and ruined the wall.",
                    "Wo roz TV chalu chhod kar so jata hai, isliye kharab ho gaya." to "He sleeps leaving the TV on every day, that's why it got ruined.",
                    "Mummy bol rahi hain ki washing machine kaam nahi kar rahi." to "Mom is saying that the washing machine is not working.",
                    "Uska dhyan nahi tha aur bulb uske hath se chhoot gaya." to "He was not paying attention and the bulb slipped from his hand.",
                    "Usne purani TV bech kar nayi TV kharid li." to "He sold the old TV and bought a new TV.",
                    "Wo plaas lene market gaya hai." to "He has gone to the market to get pliers.",
                    "Kisi ne toh iron ki wire kaat di hai." to "Someone has cut the wire of the iron.",
                    "Wo scenery lagane ke liye stool par khada hai." to "He is standing on the stool to hang the scenery.",
                    "Usne galat plug lagaya aur short circuit ho gaya." to "He put the wrong plug and a short circuit happened.",
                    "Mechanic ne kaha ki isme hazar rupaye ka kharcha aayega." to "The mechanic said that it will cost a thousand rupees.",
                    "Wo sab cheezon ko tape se chipkane ki koshish karta hai." to "He tries to stick everything with tape.",
                    "Usne AC ka remote bacche ko de diya tha khilone ki tarah." to "He had given the AC remote to the kid like a toy.",
                    "Wo naya bulb laya par wo bhi fuse nikla." to "He brought a new bulb but that also turned out to be fused.",
                    "Uska bhai hamesha apna laptop girata rehta hai." to "His brother always keeps dropping his laptop.",
                    "Wo diwar par drill machine chala raha hai." to "He is running a drill machine on the wall.",
                    "Nidhi ne pichle mahine hi naya mixer liya tha." to "Nidhi had bought a new mixer just last month.",
                    "Usne jale hue switch ko hath lagaya aur use jhatka laga." to "He touched the burnt switch and got a shock.",
                    "Wo bina tools ke kundi theek karne baith gaya." to "He sat down to fix the latch without tools.",
                    "Use lagta hai ki har kharab cheez maarne se theek ho jati hai." to "He thinks that every broken thing gets fixed by hitting it.",
                    "Unhone kal hi saara toota hua saman kabaadi ko bech diya." to "They sold all the broken stuff to the scrap dealer just yesterday."
                )
                t8_3rdPerson.forEach { (hindi, eng) ->
                    dao.insertLessonSentence(LessonSentence(
                        lessonId = t8Id,
                        personTag = "3rd Person",
                        promptHindi = hindi,
                        promptQuestion = "Say about third person: $hindi",
                        expectedText = eng
                    ))
                }

                // Topic 9: Shopping, Bargaining & Market 🛍️👕
                val t9Id = dao.insertSpeakingLesson(SpeakingLesson(
                    category = "Shopping",
                    title = "Shopping, Bargaining & Market 🛍️👕",
                    description = "Master phrases for buying clothes, sizes, bargaining, returns, prices, and bills.",
                    level = "Beginner",
                    iconName = "ShoppingBag"
                )).toInt()

                val t9_1stPerson = listOf(
                    "Main aaj shopping ke liye ja raha hoon." to "I am going shopping today.",
                    "Mujhe ye shirt wapas karni hai." to "I want to return this shirt.",
                    "Main itne mehenge kapde nahi kharidunga." to "I will not buy such expensive clothes.",
                    "Mujhe iska dusra size chahiye." to "I need a different size of this.",
                    "Hum dukaandar se theek theek lagwayenge." to "We will get the shopkeeper to give a fair price.",
                    "Mujhe ye rang thoda fika lag raha hai." to "This color is looking a bit dull to me.",
                    "Maine kal yahan se ek jeans li thi." to "I had bought a pair of jeans from here yesterday.",
                    "Main thoda sasta wala dekhna chahta hoon." to "I want to see a slightly cheaper one.",
                    "Mujhe ye fitting sahi nahi aa rahi." to "This fitting is not right for me.",
                    "Main card se payment karunga." to "I will make the payment by card.",
                    "Mujhe chutte paise chahiye." to "I need loose change.",
                    "Maine is kapde ki quality theek se check kar li hai." to "I have checked the quality of this cloth properly.",
                    "Main do lungi, toh theek rate lagaoge?" to "If I take two, will you give a fair price?",
                    "Mujhe iska pakka bill zarur dena." to "Definitely give me its valid bill.",
                    "Hum kal sham ko sabji mandi gaye the." to "We went to the vegetable market yesterday evening.",
                    "Main trial room me try kar leta hoon." to "I will try it on in the trial room.",
                    "Mujhe aisi hi same design chahiye thi." to "I wanted the exact same design.",
                    "Maine purane kapde wapas kar diye." to "I returned the old clothes.",
                    "Main cash laya hi nahi hoon." to "I haven't brought cash at all.",
                    "Mujhe ye kurta kandhe se tight ho raha hai." to "This kurta is getting tight on me from the shoulders.",
                    "Hum bina bargain kiye kuch nahi kharidtē." to "We don't buy anything without bargaining.",
                    "Main discount ke bina ye nahi lunga." to "I will not take this without a discount.",
                    "Mujhe is par pachas rupaye kam karwane hain." to "I want to get fifty rupees reduced on this.",
                    "Maine do din pehle hi ye joota kharida tha." to "I had bought this shoe just two days ago.",
                    "Main sirf window shopping kar raha hoon." to "I am just window shopping.",
                    "Mujhe is rang me kuch aur behtar dikhao." to "Show me something better in this color."
                )
                t9_1stPerson.forEach { (hindi, eng) ->
                    dao.insertLessonSentence(LessonSentence(
                        lessonId = t9Id,
                        personTag = "1st Person",
                        promptHindi = hindi,
                        promptQuestion = "Say in English: $hindi",
                        expectedText = eng
                    ))
                }

                val t9_2ndPerson = listOf(
                    "Bhaiya, thoda theek theek lagao." to "Brother, give a fair price.",
                    "Kya ye rang dhone ke baad utrega toh nahi?" to "Will this color not fade after washing?",
                    "Aapne iska daam bohot zyada bataya hai." to "You have quoted a very high price for this.",
                    "Tum mere sath bargaining me madad karna." to "You help me in bargaining.",
                    "Aap is par kitna discount doge?" to "How much discount will you give on this?",
                    "Tum ye wali t-shirt kyu nahi try karte?" to "Why don't you try this t-shirt?",
                    "Kya aap kharida hua saman wapas lete hain?" to "Do you take back purchased items?",
                    "Bhaiya, isme M size milega kya?" to "Brother, will I get an M size in this?",
                    "Aap apna thela doge ya main nikalu?" to "Will you give your bag or should I take out mine?",
                    "Tum khariddari me kitna time lagaoge?" to "How much time will you take in shopping?",
                    "Bhaiya, thoda aur acchi quality me dikhao na." to "Brother, show something in a slightly better quality.",
                    "Aap online payment accept karte ho ya sirf cash?" to "Do you accept online payment or only cash?",
                    "Tum is trouser ko kal wapas kar aana." to "You return this trouser tomorrow.",
                    "Kya aap iski colour ki guarantee de rahe hain?" to "Are you giving a guarantee for its color?",
                    "Tumne kitne ka kharida ye lehenga?" to "For how much did you buy this lehenga?",
                    "Aap ise ek box me pack kar dijiye." to "Please pack this in a box.",
                    "Tum hamesha mehengi cheezein pasand karte ho." to "You always like expensive things.",
                    "Bhaiya, isme aur kaun kaun se colours available hain?" to "Brother, what other colors are available in this?",
                    "Aapne mujhe pichli baar bohot theek rate diya tha." to "You had given me a very fair price last time.",
                    "Tum inke aage mat jhukna, sasta karke hi denge." to "Don't yield in front of them, they will give it cheaper.",
                    "Aap iska final daam bataiye, tabhi lunga." to "Tell me its final price, only then I will take it.",
                    "Tumhe wo local market se lena chahiye tha, sasta milta." to "You should have bought that from the local market, you would get it cheaper.",
                    "Kya aap ye shirt abhi alter kar ke de sakte hain?" to "Can you alter and give this shirt right now?",
                    "Tum bargaining karte waqt sharma kyu rahe ho?" to "Why are you feeling shy while bargaining?",
                    "Aapka dukan subah theek kitne baje khulta hai?" to "At exactly what time does your shop open in the morning?",
                    "Tumne bargain karke pachas rupaye bacha liye." to "You saved fifty rupees by bargaining.",
                    "Bhaiya, hum roz ke customer hain, kuch toh sasta karo." to "Brother, we are daily customers, make it a bit cheaper."
                )
                t9_2ndPerson.forEach { (hindi, eng) ->
                    dao.insertLessonSentence(LessonSentence(
                        lessonId = t9Id,
                        personTag = "2nd Person",
                        promptHindi = hindi,
                        promptQuestion = "Ask in English: $hindi",
                        expectedText = eng
                    ))
                }

                val t9_3rdPerson = listOf(
                    "Dukaandar ne rate ek rupiya bhi kam nahi kiya." to "The shopkeeper did not reduce the rate by even one rupee.",
                    "Wo har kapde ki quality theek se kheench kar check kar raha hai." to "He is checking the quality of every cloth properly by pulling it.",
                    "Usne kal yehi same shirt aadhi keemat me li." to "He bought this exact same shirt at half the price yesterday.",
                    "Nidhi apna lehenga alter karwane tailer ke paas gayi hai." to "Nidhi has gone to the tailor to get her lehenga altered.",
                    "Ye wala dukaandar hamesha theek theek rate lagata hai." to "This particular shopkeeper always gives a fair price.",
                    "Usne bina trial liye kapde pack karwa liye." to "He got the clothes packed without taking a trial.",
                    "Dukaandar bola ki washing machine me bhi ye rang nahi utrega." to "The shopkeeper said that this color will not fade even in the washing machine.",
                    "Wo sale ke time dher saari shopping karta hai." to "He does a lot of shopping during sale time.",
                    "Uski mummy ko bohot acchi bargaining karni aati hai." to "His mother knows how to do very good bargaining.",
                    "Usne GST wala bill manga par shopkeeper ne mana kar diya." to "He asked for the bill with GST but the shopkeeper refused.",
                    "Ye cotton ka kapda dhone ke baad bohot jaldi shrink ho jayega." to "This cotton cloth will shrink very quickly after washing.",
                    "Wo size badalne kal dukaan par wapas aayega." to "He will come back to the shop tomorrow to exchange the size.",
                    "Sale ki वजह se dukaan par bohot bheed thi." to "There was a lot of crowd at the shop because of the sale.",
                    "Usne saste ke chakkar me kharab material le liya." to "He bought bad material in the greed of a low price.",
                    "Dukaandar ne tag tootne ki वजह se saman wapas lene se mana kar diya." to "The shopkeeper refused to take the item back because the tag was broken.",
                    "Wo Diwali ki sale ka wait kar rahi hai sasta lene ke liye." to "She is waiting for the Diwali sale to buy cheaper.",
                    "Shopping karte waqt uska phone chori ho gaya." to "Her phone got stolen while shopping.",
                    "Salesman ne usko sabse mehangi wali saree dikhayi." to "The salesman showed her the most expensive saree.",
                    "Wo pichle ek ghante se jootey ka design select kar raha hai." to "He has been selecting the shoe design for the last one hour.",
                    "Us branded dukaan me kapde return karne ki policy nahi hai." to "There is no policy of returning clothes in that branded shop.",
                    "Wo thele wale se sabji ke rate ke liye jhagad raha hai." to "He is arguing with the cart vendor over the vegetable rates.",
                    "Nidhi hamesha pure cotton ke kapde hi kharidti hai." to "Nidhi always buys only pure cotton clothes.",
                    "Customer mehnga rate sun kar gusse me dukan se nikal gaya." to "The customer left the shop in anger after hearing the expensive rate.",
                    "Ye wala cloth market Mondays ko theek se nahi khulta." to "This particular cloth market doesn't open properly on Mondays.",
                    "Uska bada bhai uske liye naya winter jacket laya hai." to "His elder brother has brought a new winter jacket for him.",
                    "Dukaandar ne abhi flat 50% discount ka offer lagaya hua hai." to "The shopkeeper has put up a flat 50% discount offer right now.",
                    "Usne apna size na milne par dusri dukaan se khariddari ki." to "He shopped from another shop upon not finding his size."
                )
                t9_3rdPerson.forEach { (hindi, eng) ->
                    dao.insertLessonSentence(LessonSentence(
                        lessonId = t9Id,
                        personTag = "3rd Person",
                        promptHindi = hindi,
                        promptQuestion = "Say about third person: $hindi",
                        expectedText = eng
                    ))
                }

                // Topic 10: Health, Sickness & Doctor 🤒🩺
                val t10Id = dao.insertSpeakingLesson(SpeakingLesson(
                    category = "Health",
                    title = "Health, Sickness & Doctor 🤒🩺",
                    description = "Master phrases for fever, doctors, medicines, home remedies, remedies, and feeling unwell.",
                    level = "Beginner",
                    iconName = "LocalHospital"
                )).toInt()

                val t10_1stPerson = listOf(
                    "Mera sarr dard se phata ja raha hai." to "My head is splitting with pain.",
                    "Mujhe kal raat se halka bukhar hai." to "I have a mild fever since last night.",
                    "Main khansi ki wajah se so nahi paya." to "I couldn't sleep because of the cough.",
                    "Mujhe subah se chakkar aa rahe hain." to "I have been feeling dizzy since morning.",
                    "Main adrak aur tulsi ki chai pi raha hoon." to "I am drinking ginger and holy basil tea.",
                    "Meri naak poori tarah band hai." to "My nose is completely blocked.",
                    "Mujhe doctor ke sath appointment book karni hai." to "I want to book an appointment with the doctor.",
                    "Maine apni blood test report online check kar li hai." to "I have checked my blood test report online.",
                    "Main ye dawai khali pet lunga." to "I will take this medicine on an empty stomach.",
                    "Mere gale me bohot kharash hai." to "I have a very sore throat.",
                    "Mujhe ulti jaisa mehsus ho raha hai." to "I am feeling nauseous.",
                    "Main haldi wala doodh pi kar sounga." to "I will sleep after drinking turmeric milk.",
                    "Meri aankhein dhoop me dard kar rahi hain." to "My eyes are hurting in the sunlight.",
                    "Mujhe lagta hai mujhe food poisoning ho gayi hai." to "I think I have got food poisoning.",
                    "Main vicks laga kar steam lunga." to "I will take steam after applying Vicks.",
                    "Mera pet subah se kharab hai." to "My stomach is upset since morning.",
                    "Maine dard ki goli kha li hai." to "I have taken a painkiller.",
                    "Mujhe sardi zukaam ho gaya hai." to "I have caught a cold.",
                    "Main apna blood pressure check karwa raha hoon." to "I am getting my blood pressure checked.",
                    "Mujhe is dawai se neend aati hai." to "This medicine makes me sleepy.",
                    "Mera badan dard se toot raha hai." to "My body is aching all over.",
                    "Main doctor ko apni takleef theek se bataunga." to "I will explain my problem properly to the doctor.",
                    "Hum subah sham kaadha peete hain." to "We drink herbal decoction morning and evening.",
                    "Meri tabyet kal se thodi theek hai." to "My health is a bit better since yesterday.",
                    "Maine thande pani se nahana chhod diya hai." to "I have stopped bathing with cold water.",
                    "Mujhe doctor ne aaram karne ko kaha hai." to "The doctor has told me to rest.",
                    "Maine abhi tak thermometer se bukhar nahi napa." to "I haven't measured the fever with a thermometer yet."
                )
                t10_1stPerson.forEach { (hindi, eng) ->
                    dao.insertLessonSentence(LessonSentence(
                        lessonId = t10Id,
                        personTag = "1st Person",
                        promptHindi = hindi,
                        promptQuestion = "Say in English: $hindi",
                        expectedText = eng
                    ))
                }

                val t10_2ndPerson = listOf(
                    "Tumhari naak beh rahi hai, jao rumaal lao." to "Your nose is runny, go get a handkerchief.",
                    "Tum toh poore time chheenkate rehte ho." to "You keep sneezing all the time.",
                    "Kya ye dawai khali pet leni hai?" to "Should this medicine be taken on an empty stomach?",
                    "Aapko apni test reports kal mil jayengi." to "You will get your test reports tomorrow.",
                    "Tumne haldi wala doodh kyu nahi piya?" to "Why didn't you drink turmeric milk?",
                    "Tumhari aawaz kyu baithi hui hai?" to "Why is your voice hoarse?",
                    "Tumhe barish me bheegne ki kya zaroorat thi?" to "What was the need for you to get wet in the rain?",
                    "Doctor sahab, kya mujhe koi serious bimari hai?" to "Doctor, do I have any serious illness?",
                    "Aap theek se aaram kyu nahi karte?" to "Why don't you rest properly?",
                    "Tum garm pani ke garare kar lo." to "You should gargle with warm water.",
                    "Tumhari naak se fawara chhut raha hai kya?" to "Is a fountain shooting from your nose?",
                    "Tumhe din me kitni baar dawai leni hai?" to "How many times a day do you have to take the medicine?",
                    "Kya tumne apna sugar check karwaya?" to "Did you get your sugar checked?",
                    "Tum itni khansi me ice cream kyu kha rahe ho?" to "Why are you eating ice cream with such a bad cough?",
                    "Apna sarr garm kapde se dhak lo." to "Cover your head with a warm cloth.",
                    "Aapko kitne dino se bukhar aa raha hai?" to "Since how many days have you been getting a fever?",
                    "Tumhe doctor ke paas jana hi padega." to "You will have to go to the doctor.",
                    "Tumhari aankhein ekdum laal ho rahi hain." to "Your eyes are getting completely red.",
                    "Tum bina sweater ke ghumoge toh bimar padoge hi." to "If you roam without a sweater, you will definitely fall sick.",
                    "Tumne Vicks kahan chhupa kar rakhi hai?" to "Where have you hidden the Vicks?",
                    "Aap apna khayal theek se nahi rakhte." to "You don't take care of yourself properly.",
                    "Tumhe dawai garm pani ke sath leni padegi." to "You will have to take the medicine with warm water.",
                    "Kya tumhara pet dard abhi bhi ho raha hai?" to "Is your stomach ache still happening?",
                    "Tumhe kisi ne sardi me AC chalane ko kaha tha?" to "Did someone tell you to turn on the AC in the winter?",
                    "Tum chupchap bistar me let jao." to "Lie down quietly in the bed.",
                    "Tumhari bimari ka natak ab nahi chalega." to "Your fake illness drama won't work now.",
                    "Aap kal sham ko clinic aa jaiyega." to "Please come to the clinic tomorrow evening."
                )
                t10_2ndPerson.forEach { (hindi, eng) ->
                    dao.insertLessonSentence(LessonSentence(
                        lessonId = t10Id,
                        personTag = "2nd Person",
                        promptHindi = hindi,
                        promptQuestion = "Ask in English: $hindi",
                        expectedText = eng
                    ))
                }

                val t10_3rdPerson = listOf(
                    "Uska sarr dard se phata ja raha hai." to "His head is splitting with pain.",
                    "Wo subah se naak ragad raha hai." to "He has been rubbing his nose since morning.",
                    "Uski naak itni beh rahi hai ki rumaal bheeg gaya." to "His nose is running so much that the handkerchief got wet.",
                    "Mummy uske liye adrak wali chai bana rahi hain." to "Mom is making ginger tea for him.",
                    "Doctor ne use teen din ka bed rest diya hai." to "The doctor has given him bed rest for three days.",
                    "Usne test reports hospital se collect kar li hain." to "He has collected the test reports from the hospital.",
                    "Uski tabyet achanak kharab ho gayi." to "His health suddenly deteriorated.",
                    "Wo bimar hone ka bahana bana raha hai." to "He is making an excuse of being sick.",
                    "Dadi hamesha kaadha peene ki salah deti hain." to "Grandma always advises to drink herbal decoction.",
                    "Uska bukhar dawai khane ke baad utar gaya." to "His fever came down after taking the medicine.",
                    "Wo injection lagwane se bohot darta hai." to "He is very scared of getting an injection.",
                    "Usko dengue ka machhar kaat gaya tha." to "He was bitten by a dengue mosquito.",
                    "Padosi wale uncle abhi hospital me admit hain." to "The neighborhood uncle is currently admitted in the hospital.",
                    "Uski aawaz zukaam ke karan badal gayi hai." to "His voice has changed due to the cold.",
                    "Usne galat dawai kha li thi." to "He had taken the wrong medicine.",
                    "Wo sardi me bina jacket ke bahar gaya tha." to "He had gone out without a jacket in the cold.",
                    "Compounder ne uski patti theek se nahi bandhi." to "The compounder didn't tie his bandage properly.",
                    "Usko khansi rukne ka naam hi nahi le rahi." to "His cough is not showing any signs of stopping.",
                    "Uska pet kal raat ka bahar ka khana khane se kharab hua." to "His stomach got upset by eating outside food last night.",
                    "Usko har mausam badalne par sardi ho jati hai." to "He gets a cold every time the season changes.",
                    "Uski test report me hemoglobin kam aaya hai." to "His hemoglobin has come low in the test report.",
                    "Wo dawai theek time par nahi khata." to "He doesn't take medicine on the right time.",
                    "Uska dard abhi thoda kam hua hai." to "His pain has reduced a bit now.",
                    "Wo muh me thermometer daba kar baitha hai." to "He is sitting with a thermometer pressed in his mouth.",
                    "Nurse ne use kal aane ko kaha hai." to "The nurse has told him to come tomorrow.",
                    "Uski behti naak dekh kar sab hasne lage." to "Everyone started laughing looking at his runny nose."
                )
                t10_3rdPerson.forEach { (hindi, eng) ->
                    dao.insertLessonSentence(LessonSentence(
                        lessonId = t10Id,
                        personTag = "3rd Person",
                        promptHindi = hindi,
                        promptQuestion = "Say about third person: $hindi",
                        expectedText = eng
                    ))
                }

                // Topic 11: Guests, Relatives & Neighbors ☕🏠
                val t11Id = dao.insertSpeakingLesson(SpeakingLesson(
                    category = "Guests & Neighbors",
                    title = "Guests, Relatives & Neighbors ☕🏠",
                    description = "Master daily English phrases for hosting guests, relatives, neighbors, and family events.",
                    level = "Beginner",
                    iconName = "People"
                )).toInt()

                val t11_1stPerson = listOf(
                    "Humare ghar achanak mehman aa gaye hain." to "Guests have arrived at our house suddenly.",
                    "Main unke liye chai aur nashta lene ja raha hoon." to "I am going to get tea and snacks for them.",
                    "Mujhe sach me nahi pata tha ki aap log aa rahe hain." to "I really didn't know that you guys were coming.",
                    "Main aapka hi intezaar kar raha tha." to "I was waiting for you only.",
                    "Maine aapke liye khass mithai mangwayi hai." to "I have ordered special sweets for you.",
                    "Hum padosiyo ke sath uthna baithna kam hi rakhte hain." to "We keep very little interaction with the neighbors.",
                    "Main drawing room saaf kar raha hoon." to "I am cleaning the drawing room.",
                    "Mujhe in ristedaro se milna pasand nahi hai." to "I don't like meeting these relatives.",
                    "Main aapko darwaze tak chhodne aata hoon." to "I will come to drop you till the door.",
                    "Hum thodi der aur baithna chahte the." to "We wanted to sit for a little longer.",
                    "Maine unka swagat bohot ache se kiya." to "I welcomed them very well.",
                    "Main guest room me naya bedsheet bicha raha hoon." to "I am spreading a new bedsheet in the guest room.",
                    "Mujhe samajh nahi aa raha main kya baat karun." to "I can't understand what I should talk about.",
                    "Maine khane me do teen sabjiyan banayi hain." to "I have made two or three vegetable dishes for the meal.",
                    "Hum log kal unke ghar milne jayenge." to "We will go to meet them at their house tomorrow.",
                    "Main mehmaano ke baccho ke liye chocolates laya hoon." to "I have brought chocolates for the guests' children.",
                    "Mujhe aur thoda waqt dijiye, taiyar hone ke liye." to "Give me a little more time to get ready.",
                    "Maine sharma ji ke pariwar ko bhi bulaya hai." to "I have invited Sharma ji's family too.",
                    "Main nahi chahta ki wo kal yahan rukein." to "I don't want them to stay here tomorrow.",
                    "Humne apne sare purane kisse yaad kiye." to "We remembered all our old stories.",
                    "Main unke theherne ka intezaam kar raha hoon." to "I am making arrangements for their stay.",
                    "Mujhe bheed bhad wale function pasand nahi aate." to "I don't like crowded functions.",
                    "Main mehmano ko khana serve kar raha hoon." to "I am serving food to the guests.",
                    "Hum apne ristedaro se tyohar par hi milte hain." to "We meet our relatives only on festivals.",
                    "Maine unhe apne naye ghar ka tour diya." to "I gave them a tour of my new house.",
                    "Main uncle ki baatein sun kar pak gaya hoon." to "I am bored of listening to uncle's talks.",
                    "Hum mehmano ko wapas khali hath nahi bhejte." to "We don't send guests back empty-handed."
                )
                t11_1stPerson.forEach { (hindi, eng) ->
                    dao.insertLessonSentence(LessonSentence(
                        lessonId = t11Id,
                        personTag = "1st Person",
                        promptHindi = hindi,
                        promptQuestion = "Say in English: $hindi",
                        expectedText = eng
                    ))
                }

                val t11_2ndPerson = listOf(
                    "Arey aaiye aaiye, andar aakar baithiye." to "Hey come, come inside and sit.",
                    "Aapne aane se pehle phone kyu nahi kiya?" to "Why didn't you call before coming?",
                    "Thoda aur lijiye na, aapne toh kuch khaya hi nahi." to "Please have some more, you hardly ate anything.",
                    "Aur sunao, bacche ki padhai kaisi chal rahi hai?" to "Tell me, how are the kid's studies going?",
                    "Aap chai lenge ya thanda?" to "Will you have tea or something cold?",
                    "Tum itne salon baad milne kyu aaye ho?" to "Why have you come to meet after so many years?",
                    "Aapko humara naya ghar kaisa laga?" to "How did you like our new house?",
                    "Tumhare uncle aur aunty kaise hain ab?" to "How are your uncle and aunt now?",
                    "Aap khana kha kar hi jayenge." to "You will go only after having a meal.",
                    "Aap log yahan aaram se rukiye, apna hi ghar samjhiye." to "You guys stay here comfortably, consider it your own house.",
                    "Tum sharma rahe ho, ek rasgulla aur lo!" to "You are feeling shy, take one more rasgulla!",
                    "Aapki beti toh bohot badi ho gayi hai!" to "Your daughter has grown up so much!",
                    "Tumhare padosi aaj kal kya kar rahe hain?" to "What are your neighbors doing nowadays?",
                    "Aap raat ka khana mere yahan hi khaiyega." to "Please have your dinner at my place only.",
                    "Tum in dino kahan rehte ho?" to "Where do you live these days?",
                    "Aap logon ko raste me koi dikkat toh nahi hui?" to "Did you guys face any problem on the way?",
                    "Tum apne papa par gaye ho bilkul." to "You have completely taken after your father.",
                    "Aapne apna business badha liya suna hai?" to "I have heard you have expanded your business?",
                    "Tum kal jaldi mat nikal jana." to "Don't leave early tomorrow.",
                    "Aapko thodi aur puri du?" to "Should I give you some more puris?",
                    "Tumhare ghar wale kaise hain sab?" to "How is everyone in your family?",
                    "Aap sofa par nahi, yahan aaram kursi par baithiye." to "Don't sit on the sofa, sit here on the armchair.",
                    "Tumhe pados wale Verma ji ke baare me pata chala?" to "Did you come to know about neighbor Verma ji?",
                    "Aapke bina party me maza nahi aaya tha." to "It was not fun in the party without you.",
                    "Tum aate kyu nahi kabhi humare ghar?" to "Why don't you ever come to our house?",
                    "Aap lijiye na, bacho ke aane ka wait mat kijiye." to "Please take it, don't wait for the kids to come.",
                    "Tum thoda lamba chhutti lekar aao agli baar." to "Come with a longer leave next time."
                )
                t11_2ndPerson.forEach { (hindi, eng) ->
                    dao.insertLessonSentence(LessonSentence(
                        lessonId = t11Id,
                        personTag = "2nd Person",
                        promptHindi = hindi,
                        promptQuestion = "Ask in English: $hindi",
                        expectedText = eng
                    ))
                }

                val t11_3rdPerson = listOf(
                    "Unke bache padhai me bohot hoishiyar hain." to "Their kids are very smart in studies.",
                    "Nidhi ne mehmano ke liye badhiya nashta banaya hai." to "Nidhi has made excellent snacks for the guests.",
                    "Pados wale Gupta ji ki beti ki shadi fix ho gayi." to "Neighbor Gupta ji's daughter's marriage got fixed.",
                    "Wo log bina bataye achanak aa dhamke." to "They crashed in suddenly without informing.",
                    "Mama ji is baar diwali par aayenge." to "Maternal uncle will come this time on Diwali.",
                    "Bua ji hamesha nakhre dikhati hain khane me." to "Aunt always shows tantrums in food.",
                    "Unhone do plate poori bhar kar khayi." to "They ate two plates fully loaded.",
                    "Naye padosi kisi se baat nahi karte." to "The new neighbors don't talk to anyone.",
                    "Wo humesha apne beton ki tareef karta rehta hai." to "He always keeps praising his sons.",
                    "Chacha ji humare liye mithai ka dabba laye hain." to "Paternal uncle has brought a box of sweets for us.",
                    "Unka bada beta abhi foreign me job kar raha hai." to "Their elder son is currently doing a job in a foreign country.",
                    "Wo log bas ek cup chai pi kar chale gaye." to "They left after drinking just one cup of tea.",
                    "Pados wali aunty humesha chugli karti hain." to "The neighborhood aunt always gossips.",
                    "Unke ghar me kalesh hota hi rehta hai." to "Brawls keep happening in their house.",
                    "Uski chachi ne bohot sundar saree pehni thi." to "His aunt was wearing a very beautiful saree.",
                    "Wo apne sath dher sare khilone laye the." to "They had brought a lot of toys with them.",
                    "Unka naya makaan bohot alishaan ban raha hai." to "Their new house is being built very grandly.",
                    "Sharma ji ne nai gaadi kharidi hai, suna hai?" to "Sharma ji has bought a new car, have you heard?",
                    "Mehmano ko naya sofa bohot pasand aaya." to "The guests liked the new sofa very much.",
                    "Wo apne devar ki shadi ka card dene aaye the." to "They came to give the wedding card of her brother-in-law.",
                    "Padosiyon ne aaj raat paath rakha hai ghar par." to "The neighbors have kept a prayer meeting at home tonight.",
                    "Bhabhi ji hamesha mehmaan-nawazi me aage rehti hain." to "Sister-in-law always stays ahead in hospitality.",
                    "Wo bina gajrela khaye gaye hi nahi." to "They didn't leave without eating the carrot pudding.",
                    "Unka chhota ladka kitna shaitan ho gaya hai." to "How naughty their younger boy has become.",
                    "Dadi aur nani ki aapas me khoob jamti hai." to "Grandma and maternal grandma get along very well with each other.",
                    "Unhone hume agli chuttiyo me unke shehar aane ko kaha hai." to "They have asked us to come to their city in the next holidays."
                )
                t11_3rdPerson.forEach { (hindi, eng) ->
                    dao.insertLessonSentence(LessonSentence(
                        lessonId = t11Id,
                        personTag = "3rd Person",
                        promptHindi = hindi,
                        promptQuestion = "Say about third person: $hindi",
                        expectedText = eng
                    ))
                }

                // Topic 12: Travel, Routes & Auto/Cab 🚗🗺️
                val t12Id = dao.insertSpeakingLesson(SpeakingLesson(
                    category = "Travel & Routes",
                    title = "Travel, Routes & Auto/Cab 🚗🗺️",
                    description = "Master phrases for trains, buses, autos, cabs, directions, tickets, and travel bargaining.",
                    level = "Intermediate",
                    iconName = "Navigation"
                )).toInt()

                val t12_1stPerson = listOf(
                    "Main apni train miss kar chuka hoon." to "I have missed my train.",
                    "Mujhe agla bus stand kahan milega?" to "Where will I find the next bus stand?",
                    "Main auto ka wait kar raha hoon." to "I am waiting for an auto.",
                    "Hum yahan pehli baar aaye hain." to "We have come here for the first time.",
                    "Maine do ticket book kar li hain." to "I have booked two tickets.",
                    "Mujhe window seat chahiye." to "I need a window seat.",
                    "Mera saman gaadi me chhoot gaya." to "My luggage got left in the vehicle.",
                    "Main station paidal jaunga." to "I will go to the station on foot.",
                    "Hum traffic me buri tarah fas gaye hain." to "We are badly stuck in traffic.",
                    "Mujhe ulti aa rahi hai is safar me." to "I am feeling nauseous on this journey.",
                    "Main cab wale ko location bhej raha hoon." to "I am sending the location to the cab driver.",
                    "Hum rasta bhatak gaye hain." to "We have lost our way.",
                    "Mujhe thodi der aaram karna hai yahan." to "I want to rest here for a while.",
                    "Maine abhi tak packing nahi ki hai." to "I haven't done the packing yet.",
                    "Main meter se hi jaunga." to "I will go by the meter only.",
                    "Mujhe agle chourahe par utarna hai." to "I have to get down at the next crossroad.",
                    "Mera ticket confirm nahi hua." to "My ticket didn't get confirmed.",
                    "Main bus me dhakke kha raha hoon." to "I am getting pushed around in the bus.",
                    "Mujhe dusri train pakadni padegi." to "I will have to catch another train.",
                    "Hum subah jaldi nikal jayenge." to "We will leave early in the morning.",
                    "Main map me rasta dekh raha hoon." to "I am looking at the route on the map.",
                    "Mujhe aage se left lena hai." to "I have to take a left from ahead.",
                    "Main is auto wale ko zyada paise nahi dunga." to "I will not give more money to this auto driver.",
                    "Maine kal ki flight book ki hai." to "I have booked tomorrow's flight.",
                    "Main apna rasta khud dhund lunga." to "I will find my way myself.",
                    "Mujhe station pahunchne me aadha ghanta lagega." to "It will take me half an hour to reach the station.",
                    "Main petrol pump par ruka hoon." to "I am stopped at the petrol pump."
                )
                t12_1stPerson.forEach { (hindi, eng) ->
                    dao.insertLessonSentence(LessonSentence(
                        lessonId = t12Id,
                        personTag = "1st Person",
                        promptHindi = hindi,
                        promptQuestion = "Say in English: $hindi",
                        expectedText = eng
                    ))
                }

                val t12_2ndPerson = listOf(
                    "Bhaiya meter se chaloge ya fix rate?" to "Brother, will you go by the meter or a fixed rate?",
                    "Station yahan se paidal kitni door hai?" to "How far is the station on foot from here?",
                    "Tumne train ka status check kiya kya?" to "Did you check the train status?",
                    "Aap aage se right mud jana." to "You turn right from ahead.",
                    "Tumhara saman kahan rakha hai?" to "Where is your luggage kept?",
                    "Bhaiya itne paise toh nahi lagenge yahan tak ke." to "Brother, it won't cost this much till here.",
                    "Tum flight ke liye late ho jaoge." to "You will be late for the flight.",
                    "Kya aap mujhe rasta bata sakte hain?" to "Can you tell me the way?",
                    "Tum yahan se seedhe chalte jao." to "You keep walking straight from here.",
                    "Aap kahan utarna chahte hain?" to "Where do you want to get down?",
                    "Tumne auto wale se theek se baat nahi ki." to "You didn't talk properly to the auto driver.",
                    "Aapka gaon yahan se kitna door hai?" to "How far is your village from here?",
                    "Tum apni seat par kyu nahi baithte?" to "Why don't you sit on your seat?",
                    "Bhaiya aage auto rok dena." to "Brother, stop the auto ahead.",
                    "Tum bina ticket kyu safar kar rahe ho?" to "Why are you traveling without a ticket?",
                    "Kya aapne online cab book kar li hai?" to "Have you booked the cab online?",
                    "Tumhara bag bohot bhari lag raha hai." to "Your bag is looking very heavy.",
                    "Bhaiya ye bus kahan jati hai?" to "Brother, where does this bus go?",
                    "Aap galat raste par aa gaye hain." to "You have come on the wrong path.",
                    "Tum boarding pass nikal kar rakho." to "You keep the boarding pass out.",
                    "Tum kal kitne baje nikal rahe ho?" to "At what time are you leaving tomorrow?",
                    "Aap raste me kahin rukenge kya?" to "Will you stop somewhere on the way?",
                    "Tum itna lamba safar akele kaise karoge?" to "How will you do such a long journey alone?",
                    "Bhaiya thoda tez chalo, meri train chhoot jayegi." to "Brother, drive a bit fast, I will miss my train.",
                    "Tumne apni ticket cancel kyu kar di?" to "Why did you cancel your ticket?",
                    "Aap us pul ke neeche aakar wait karo." to "You come and wait under that bridge.",
                    "Tum kahan kho gaye the is bheed me?" to "Where were you lost in this crowd?"
                )
                t12_2ndPerson.forEach { (hindi, eng) ->
                    dao.insertLessonSentence(LessonSentence(
                        lessonId = t12Id,
                        personTag = "2nd Person",
                        promptHindi = hindi,
                        promptQuestion = "Ask in English: $hindi",
                        expectedText = eng
                    ))
                }

                val t12_3rdPerson = listOf(
                    "Uski train do ghante late chal rahi hai." to "His train is running two hours late.",
                    "Auto wala bohot zyada paise maang raha hai." to "The auto driver is asking for too much money.",
                    "Wo rasta bhool gaya hai." to "He has forgotten the way.",
                    "Cab driver location par nahi pahuncha." to "The cab driver didn't reach the location.",
                    "Unki bus achanak raste me kharab ho gayi." to "Their bus suddenly broke down on the way.",
                    "Wo kal raat train se aayega." to "He will come by train tomorrow night.",
                    "Usne apna ticket waiting list me book kiya tha." to "He had booked his ticket on the waiting list.",
                    "Wo conductor se behas kar raha tha." to "He was arguing with the conductor.",
                    "Usko window seat nahi mili." to "He didn't get a window seat.",
                    "Driver ne galat turn le liya." to "The driver took a wrong turn.",
                    "Wo log paidal hi station nikal gaye." to "They left for the station on foot.",
                    "Uski flight aab udne wali hai." to "His flight is about to take off now.",
                    "Nidhi raste me so gayi thi." to "Nidhi had fallen asleep on the way.",
                    "Wo auto wale se jhagda kar rahi hai." to "She is fighting with the auto driver.",
                    "Train platform number teen par aayegi." to "The train will arrive at platform number three.",
                    "Usne hume galat address diya tha." to "He had given us the wrong address.",
                    "Wo poore safar me phone par baat karta raha." to "He kept talking on the phone throughout the journey.",
                    "Uska saman chori ho gaya train me." to "His luggage got stolen in the train.",
                    "Cab wala AC on nahi kar raha tha." to "The cab driver was not turning on the AC.",
                    "Wo traffic jam me do ghante fasa raha." to "He was stuck in a traffic jam for two hours.",
                    "Usko raste me ulti ho gayi." to "He vomited on the way.",
                    "Wo log aage wale chourahe par utrenge." to "They will get down at the crossroad ahead.",
                    "Train ne achanak horn bajaya." to "The train blew the horn suddenly.",
                    "Wo kal ki bus se wapas gaon ja raha hai." to "He is going back to the village by tomorrow's bus.",
                    "Usne taxi wale ko extra tip de di." to "He gave an extra tip to the taxi driver.",
                    "Wo station par humara intezaar kar raha hoga." to "He must be waiting for us at the station."
                )
                t12_3rdPerson.forEach { (hindi, eng) ->
                    dao.insertLessonSentence(LessonSentence(
                        lessonId = t12Id,
                        personTag = "3rd Person",
                        promptHindi = hindi,
                        promptQuestion = "Say about third person: $hindi",
                        expectedText = eng
                    ))
                }

                // Topic 13: Money, Banking & UPI 💸🏧
                val t13Id = dao.insertSpeakingLesson(SpeakingLesson(
                    category = "Banking",
                    title = "Money, Banking & UPI 💸🏧",
                    description = "Master phrases for UPI payments, ATM withdrawals, borrowing, balance, and loose change.",
                    level = "Intermediate",
                    iconName = "AccountBalance"
                )).toInt()

                val t13_1stPerson = listOf(
                    "Mera payment kal se atka hua hai." to "My payment is stuck since yesterday.",
                    "Main ATM se cash nikalne ja raha hoon." to "I am going to withdraw cash from the ATM.",
                    "Mera UPI payment fail ho gaya hai." to "My UPI payment has failed.",
                    "Mujhe tumse thode paise udhaar chahiye." to "I need to borrow some money from you.",
                    "Mere account me balance kam hai." to "The balance in my account is low.",
                    "Main apna ATM pin bhool gaya hoon." to "I have forgotten my ATM pin.",
                    "Mujhe 500 ke khulle chahiye." to "I need change for 500.",
                    "Main cash nahi rakhta, sirf online payment karta hoon." to "I don't keep cash, I only make online payments.",
                    "Maine dukaandar ko galti se double payment kar diya." to "I accidentally made a double payment to the shopkeeper.",
                    "Mera bank server abhi down bata raha hai." to "My bank server is showing down right now.",
                    "Hum kharcha aadha-aadha baant lenge." to "We will split the expenses half and half.",
                    "Main kal tak paise lauta dunga." to "I will return the money by tomorrow.",
                    "Mere paas chhutte paise nahi hain." to "I don't have loose change.",
                    "Mujhe aaj apni EMI bharni hai." to "I have to pay my EMI today.",
                    "Maine apna bank statement check kar liya hai." to "I have checked my bank statement.",
                    "Main udhaar nahi leta kisi se." to "I do not borrow money from anyone.",
                    "Mere account se paise kat gaye par use nahi mile." to "Money got deducted from my account but he didn't receive it.",
                    "Maine usko Google Pay par request bheji hai." to "I have sent him a request on Google Pay.",
                    "Main apna naya bank account khulwa raha hoon." to "I am getting my new bank account opened.",
                    "Mera ATM card block ho gaya hai." to "My ATM card has got blocked.",
                    "Mujhe kal bank branch jana padega." to "I will have to go to the bank branch tomorrow.",
                    "Maine sabhi bills ka payment kar diya hai." to "I have made the payment for all the bills.",
                    "Mujhe ek naya credit card banwana hai." to "I want to get a new credit card made.",
                    "Hum thode paise bachat ke liye alag rakhte hain." to "We keep some money aside for savings."
                )
                t13_1stPerson.forEach { (hindi, eng) ->
                    dao.insertLessonSentence(LessonSentence(
                        lessonId = t13Id,
                        personTag = "1st Person",
                        promptHindi = hindi,
                        promptQuestion = "Say in English: $hindi",
                        expectedText = eng
                    ))
                }

                val t13_2ndPerson = listOf(
                    "Tere paas 500 ke khulle hain kya?" to "Do you have change for 500?",
                    "Tum mujhe udhaar kab wapas karoge?" to "When will you return the borrowed money to me?",
                    "Kya aap yahan UPI accept karte hain?" to "Do you accept UPI here?",
                    "Tumne apna credit card bill bhara ya nahi?" to "Did you pay your credit card bill or not?",
                    "Apna account number mujhe message kar do." to "Message me your account number.",
                    "Kya aapke paas 100 ke chhutte hain?" to "Do you have change for 100?",
                    "Tum kitne paise nikal rahe ho ATM se?" to "How much money are you withdrawing from the ATM?",
                    "Tumhe kal tak salary mil jayegi." to "You will get your salary by tomorrow.",
                    "Aapne bank me apni KYC update karwa li?" to "Did you get your KYC updated in the bank?",
                    "Tum hamesha mujhse hi paise maangte ho." to "You always ask for money from me only.",
                    "Tumhara payment mujhe receive nahi hua abhi tak." to "I haven't received your payment yet.",
                    "Kya tum mere liye ye bill pay kar sakte ho?" to "Can you pay this bill for me?",
                    "Tum udhaar lekar hamesha bhool jate ho." to "You always forget after borrowing money.",
                    "Aap QR code yahan scan kar sakte hain." to "You can scan the QR code here.",
                    "Tumhe achanak itne cash ki kya zaroorat pad gayi?" to "Why did you suddenly need so much cash?",
                    "Tumhara card machine me kaam nahi kar raha hai." to "Your card is not working in the machine.",
                    "Apna OTP kisi ko bhi mat batana." to "Don't tell your OTP to anyone.",
                    "Tum har mahine thode paise bacha kyu nahi lete?" to "Why don't you save some money every month?",
                    "Aapka bank account kis bank me hai?" to "In which bank is your bank account?",
                    "Tumne mere account me kitne paise transfer kiye?" to "How much money did you transfer to my account?",
                    "Kya tum naya loan lene ki soch rahe ho?" to "Are you thinking of taking a new loan?",
                    "Tum apna account balance ek baar check kar lo." to "You check your account balance once.",
                    "Aap paasbook print karwane kab jayenge?" to "When will you go to get the passbook printed?"
                )
                t13_2ndPerson.forEach { (hindi, eng) ->
                    dao.insertLessonSentence(LessonSentence(
                        lessonId = t13Id,
                        personTag = "2nd Person",
                        promptHindi = hindi,
                        promptQuestion = "Ask in English: $hindi",
                        expectedText = eng
                    ))
                }

                val t13_3rdPerson = listOf(
                    "Uska UPI payment bar-bar fail ho raha hai." to "His UPI payment is failing repeatedly.",
                    "Wo kal bank manager se milne gaya tha." to "He had gone to meet the bank manager yesterday.",
                    "Nidhi ka account temporarily freeze ho gaya hai." to "Nidhi's account has been temporarily frozen.",
                    "Dukaandar ne khulle paise wapas nahi kiye." to "The shopkeeper didn't return the loose change.",
                    "Usne mujhe abhi tak udhaar nahi lautaya." to "He hasn't returned the borrowed money to me yet.",
                    "ATM machine me cash khatam ho gaya hai." to "The cash has run out in the ATM machine.",
                    "Bank ka server abhi down chal raha hai." to "The bank's server is running down right now.",
                    "Uska check kal bounce ho gaya." to "His check bounced yesterday.",
                    "Wo hamesha cash carry karta hai." to "He always carries cash.",
                    "Usne mujhse kal ek hazar rupaye udhaar maange." to "He asked to borrow a thousand rupees from me yesterday.",
                    "Uska bank account hack ho gaya tha." to "His bank account had been hacked.",
                    "Wo apni EMI time par nahi bharta." to "He doesn't pay his EMI on time.",
                    "Customer care ne uska phone nahi uthaya." to "Customer care didn't pick up his phone.",
                    "Wo roz UPI se hi sab payment karti hai." to "She makes all payments through UPI only every day.",
                    "Uska debit card jald hi expire hone wala hai." to "His debit card is going to expire soon.",
                    "Usne galti se kisi aur ko paise bhej diye." to "He mistakenly sent money to someone else.",
                    "Paise aate hi usne sab udhaar chuka diya." to "He paid off all the debt as soon as the money arrived.",
                    "Uska account balance minus me chala gaya hai." to "His account balance has gone into the minus.",
                    "Wo jaldi me ATM pin dalna bhool gaya." to "He forgot to enter the ATM pin in a hurry.",
                    "Unka bank theek char baje band ho jata hai." to "Their bank closes at exactly four o'clock.",
                    "Usne fraud call par apni bank details de di." to "He gave his bank details on a fraud call.",
                    "Uska naya home loan kal approve ho gaya hai." to "His new home loan has got approved yesterday.",
                    "Wo khulle paise na hone ki वजह se pareshan tha." to "He was worried due to not having loose change."
                )
                t13_3rdPerson.forEach { (hindi, eng) ->
                    dao.insertLessonSentence(LessonSentence(
                        lessonId = t13Id,
                        personTag = "3rd Person",
                        promptHindi = hindi,
                        promptQuestion = "Say about third person: $hindi",
                        expectedText = eng
                    ))
                }

                // Topic 14: Tech, Mobile & Network Problems 📱⚡
                val t14Id = dao.insertSpeakingLesson(SpeakingLesson(
                    category = "Tech & Mobile",
                    title = "Tech, Mobile & Network Problems 📱⚡",
                    description = "Master phrases for call drops, slow internet, phone hanging, battery drain, and passwords.",
                    level = "Intermediate",
                    iconName = "Smartphone"
                )).toInt()

                val t14_1stPerson = listOf(
                    "Meri aawaz cut rahi hai kya?" to "Is my voice breaking?",
                    "Mera phone bar-bar hang ho raha hai." to "My phone keeps hanging.",
                    "Meri battery jaldi drain ho rahi hai." to "My battery is draining quickly.",
                    "Mera net bohot slow chal raha hai." to "My internet is running very slow.",
                    "Main kal se network issue face kar raha hoon." to "I have been facing network issues since yesterday.",
                    "Mera call baar baar drop ho raha hai." to "My call is dropping again and again.",
                    "Mujhe tumhari aawaz theek se nahi aa rahi." to "I am not getting your voice properly.",
                    "Mera wifi connect nahi ho raha hai." to "My wifi is not connecting.",
                    "Maine apna phone raat bhar charge par lagaya tha." to "I had put my phone on charge all night.",
                    "Mera data khatam ho gaya hai." to "My data is exhausted.",
                    "Main hotspot on kar raha hoon." to "I am turning on the hotspot.",
                    "Mujhe ek zaruri email bhejna hai par net nahi hai." to "I have to send an important email but there is no internet.",
                    "Meri screen achanak se black ho gayi." to "My screen suddenly went black.",
                    "Mera storage full ho gaya hai." to "My storage is full.",
                    "Main app update nahi kar pa raha hoon." to "I am unable to update the app.",
                    "Mera phone bohot garm ho raha hai." to "My phone is getting very hot.",
                    "Maine teen baar call kiya par uthaya nahi." to "I called three times but it wasn't answered.",
                    "Mera bluetooth connect nahi ho raha." to "My bluetooth is not connecting.",
                    "Main apna password bhool gaya hoon." to "I have forgotten my password.",
                    "Mujhe OTP receive nahi ho raha hai." to "I am not receiving the OTP.",
                    "Mera charger kharab ho gaya hai." to "My charger is ruined.",
                    "Main earphone laga kar baat kar raha hoon." to "I am talking with earphones on.",
                    "Mera camera blur photo kheench raha hai." to "My camera is taking blurry photos.",
                    "Maine kal hi recharge karwaya tha." to "I had recharged just yesterday."
                )
                t14_1stPerson.forEach { (hindi, eng) ->
                    dao.insertLessonSentence(LessonSentence(
                        lessonId = t14Id,
                        personTag = "1st Person",
                        promptHindi = hindi,
                        promptQuestion = "Say in English: $hindi",
                        expectedText = eng
                    ))
                }

                val t14_2ndPerson = listOf(
                    "Tumhari aawaz cut rahi hai." to "Your voice is breaking.",
                    "Tumhara phone kyu switch off aa raha tha?" to "Why was your phone showing switched off?",
                    "Kya tum apna wifi password bataoge?" to "Will you tell me your wifi password?",
                    "Tumhara net slow chal raha hai kya?" to "Is your internet running slow?",
                    "Tumne mera call kyu kaata?" to "Why did you disconnect my call?",
                    "Apna hotspot thodi der ke liye on karna." to "Turn on your hotspot for a while.",
                    "Tumhari battery kitne percent hai?" to "What is your battery percentage?",
                    "Kya tum mujhe WhatsApp par location bhej sakte ho?" to "Can you send me the location on WhatsApp?",
                    "Tum apna phone reset kyu nahi kar lete?" to "Why don't you reset your phone?",
                    "Tum itni der se kisse baat kar rahe ho?" to "Who have you been talking to for so long?",
                    "Tumhara screen guard toot gaya hai." to "Your screen guard is broken.",
                    "Apna charger mujhe de do." to "Give your charger to me.",
                    "Tum video call par kyu nahi aate?" to "Why don't you come on video call?",
                    "Kya tumhari app crash ho rahi hai?" to "Is your app crashing?",
                    "Tum apne phone me itne games kyu rakhte ho?" to "Why do you keep so many games in your phone?",
                    "Tumhara phone hang kyu kar raha hai?" to "Why is your phone hanging?",
                    "Tumhe network me aakar baat karni chahiye." to "You should come in the network and talk.",
                    "Tum apna data on karna bhool gaye." to "You forgot to turn on your data.",
                    "Tum speaker par kyu sun rahe ho?" to "Why are you listening on the speaker?",
                    "Tumne notification mute kar rakhe hain kya?" to "Have you muted the notifications?",
                    "Apna phone silent se hatao." to "Take your phone off silent.",
                    "Tumhare phone ki brightness bohot zyada hai." to "Your phone's brightness is very high.",
                    "Tum charger plug me lagana bhool gaye." to "You forgot to plug the charger in."
                )
                t14_2ndPerson.forEach { (hindi, eng) ->
                    dao.insertLessonSentence(LessonSentence(
                        lessonId = t14Id,
                        personTag = "2nd Person",
                        promptHindi = hindi,
                        promptQuestion = "Ask in English: $hindi",
                        expectedText = eng
                    ))
                }

                val t14_3rdPerson = listOf(
                    "Uska phone pichle ek ghante se busy aa raha hai." to "His phone has been showing busy for the past hour.",
                    "Nidhi ka net pack khatam ho gaya hai." to "Nidhi's internet pack is exhausted.",
                    "Uska call baar baar drop ho raha tha." to "His call was dropping repeatedly.",
                    "Wo bina earphone ke gaane sun raha hai." to "He is listening to songs without earphones.",
                    "Uski battery bilkul dead ho chuki hai." to "His battery is completely dead.",
                    "Usne apna naya phone paani me gira diya." to "He dropped his new phone in the water.",
                    "Uska phone hamesha silent par rehta hai." to "His phone is always on silent.",
                    "Wo network dhoondhne ke liye chhat par gaya hai." to "He has gone to the roof to search for a network.",
                    "Usne kal hi apni screen theek karwayi thi." to "He had got his screen fixed just yesterday.",
                    "Uska wifi router kharab ho gaya hai." to "His wifi router is ruined.",
                    "Wo hamesha dusro ka hotspot mangta hai." to "He always asks for other people's hotspot.",
                    "Uska phone garm hokar band ho gaya." to "His phone got hot and switched off.",
                    "Usne galti se meri call recording on kar di." to "He accidentally turned on my call recording.",
                    "Wo poore din phone me laga rehta hai." to "He stays glued to his phone all day.",
                    "Uska WhatsApp chalna band ho gaya hai." to "His WhatsApp has stopped working.",
                    "Uske phone me storage ki problem hai." to "There is a storage problem in his phone.",
                    "Wo apna phone charge karna bhool gaya." to "He forgot to charge his phone.",
                    "Usne apna purana phone bech diya." to "He sold his old phone.",
                    "Uska camera theek se focus nahi kar raha hai." to "His camera is not focusing properly.",
                    "Uski screen par bohot sare scratch hain." to "There are many scratches on his screen.",
                    "Wo bina network wale area me hai." to "He is in an area without network.",
                    "Uska charger slow charge karta hai." to "His charger charges slow.",
                    "Usne message seen karke reply nahi kiya." to "He saw the message but didn't reply."
                )
                t14_3rdPerson.forEach { (hindi, eng) ->
                    dao.insertLessonSentence(LessonSentence(
                        lessonId = t14Id,
                        personTag = "3rd Person",
                        promptHindi = hindi,
                        promptQuestion = "Say about third person: $hindi",
                        expectedText = eng
                    ))
                }

                // Topic 15: Weather, Seasons & Rain 🌧️☀️
                val t15Id = dao.insertSpeakingLesson(SpeakingLesson(
                    category = "Weather",
                    title = "Weather, Seasons & Rain 🌧️☀️",
                    description = "Master phrases for humidity, heatwaves, winter cold, rain, umbrellas, fog, and breezes.",
                    level = "Beginner",
                    iconName = "WbSunny"
                )).toInt()

                val t15_1stPerson = listOf(
                    "Aaj bohot umas hai, hawa bilkul nahi chal rahi." to "It's very humid today, there's no breeze at all.",
                    "Bahar nikalne ka mann nahi kar raha, kadi dhoop hai." to "I don't feel like going out, there is a scorching sun.",
                    "Mujhe garmi me bohot pasina aata hai." to "I sweat a lot in the summer.",
                    "Main sardi se kaamp raha hoon." to "I am shivering with cold.",
                    "Maine baarish me poora keechad kar liya." to "I got covered in mud in the rain.",
                    "Mujhe thand lag rahi hai, main sweater pehan lunga." to "I am feeling cold, I will wear a sweater.",
                    "Main barish me bheeg gaya hoon." to "I have gotten wet in the rain.",
                    "Meri chhatri hawa se ulti ho gayi." to "My umbrella flipped inside out due to the wind.",
                    "Mujhe ye suhana mausam bohot pasand hai." to "I really like this pleasant weather.",
                    "Main dhoop sekne chhat par ja raha hoon." to "I am going to the roof to bask in the sun.",
                    "Mere kapde barish me sookh nahi rahe." to "My clothes are not drying in the rain.",
                    "Main garmi se tang aa gaya hoon." to "I am fed up with the heat.",
                    "Maine aaj AC poore din on rakha hai." to "I have kept the AC on all day today.",
                    "Mujhe barish ki khushbu pasand hai." to "I like the smell of the rain.",
                    "Mera kamra bohot thanda ho gaya hai." to "My room has become very cold.",
                    "Main fog ki वजह se aage kuch dekh nahi pa raha hoon." to "I am unable to see anything ahead due to the fog.",
                    "Mujhe sardi me aalas aata hai." to "I feel lazy in the winter.",
                    "Main barish rukne ka wait kar raha hoon." to "I am waiting for the rain to stop.",
                    "Mujhe aaj zyada thand mehsus ho rahi hai." to "I am feeling colder today.",
                    "Main keechad me phisal kar gir gaya." to "I slipped and fell in the mud.",
                    "Mere hath pair sardi se sunn ho gaye hain." to "My hands and feet have gone numb from the cold.",
                    "Main garmi me thande pani se nahaunga." to "I will bathe with cold water in the summer.",
                    "Mujhe lagta hai aaj barish hogi." to "I think it will rain today.",
                    "Main dhoop se bachne ke liye chashma pehan raha hoon." to "I am wearing sunglasses to protect myself from the sun."
                )
                t15_1stPerson.forEach { (hindi, eng) ->
                    dao.insertLessonSentence(LessonSentence(
                        lessonId = t15Id,
                        personTag = "1st Person",
                        promptHindi = hindi,
                        promptQuestion = "Say in English: $hindi",
                        expectedText = eng
                    ))
                }

                val t15_2ndPerson = listOf(
                    "Tum barish me kyu bheeg rahe ho?" to "Why are you getting wet in the rain?",
                    "Aaj bahar kadi dhoop hai, mat jao." to "It's scorching sun outside today, don't go.",
                    "Tum pasine se lathpath ho." to "You are drenched in sweat.",
                    "Tumhe itni umas me garmi nahi lag rahi kya?" to "Aren't you feeling hot in such humidity?",
                    "Tum sardi me bina jacket ke kyu ghoom rahe ho?" to "Why are you roaming without a jacket in the winter?",
                    "Tumhe barish ka mausam kaisa lagta hai?" to "How do you like the rainy season?",
                    "Tum apne joote keechad me gande mat karna." to "Don't dirty your shoes in the mud.",
                    "Apna chata sath lekar jana." to "Take your umbrella with you.",
                    "Tum sardi me ice cream kyu kha rahe ho?" to "Why are you eating ice cream in the winter?",
                    "Tumhe thand lag jayegi, sweater pehan lo." to "You will catch a cold, put on a sweater.",
                    "Kya tumhare wahan bhi barish ho rahi hai?" to "Is it raining at your place too?",
                    "Tum AC ka temperature thoda kam kar do." to "You lower the AC temperature a bit.",
                    "Tumne bahar ka suhana mausam dekha?" to "Did you see the pleasant weather outside?",
                    "Tum dhoop me mat khelo, kaale ho jaoge." to "Don't play in the sun, you will get tanned.",
                    "Tum barish me gaadi dhyan se chalana." to "You drive the car carefully in the rain.",
                    "Tumhe oas me chalna pasand hai?" to "Do you like walking in the dew?",
                    "Tum itni thand me bahar kyu khade ho?" to "Why are you standing outside in such cold?",
                    "Tum apna gala sardi se bacha kar rakhna." to "You keep your throat protected from the cold.",
                    "Tum dhoop sekne bahar kyu nahi aate?" to "Why don't you come outside to bask in the sun?",
                    "Tumhe is garmi me roz nahana chahiye." to "You should bathe daily in this summer.",
                    "Tum barish me bahar kyu nikal rahe ho?" to "Why are you going out in the rain?",
                    "Tumhare kapde barish me geele ho gaye." to "Your clothes got wet in the rain.",
                    "Tum umas me fan tez kar lo." to "Turn the fan up in the humidity."
                )
                t15_2ndPerson.forEach { (hindi, eng) ->
                    dao.insertLessonSentence(LessonSentence(
                        lessonId = t15Id,
                        personTag = "2nd Person",
                        promptHindi = hindi,
                        promptQuestion = "Ask in English: $hindi",
                        expectedText = eng
                    ))
                }

                val t15_3rdPerson = listOf(
                    "Aaj kal mausam achanak badal jata hai." to "Nowadays the weather changes suddenly.",
                    "Wo garmi se pareshan ho gaya hai." to "He is troubled by the heat.",
                    "Uska chata barish me toot gaya." to "His umbrella broke in the rain.",
                    "Wo sardi se kaamp raha tha." to "He was shivering with cold.",
                    "Aaj subah se bohot ghana kohra hai." to "There is a very dense fog since this morning.",
                    "Wo dhoop me nikalne se bachta hai." to "He avoids going out in the sun.",
                    "Padosi ki gaadi barish ke pani me doob gayi." to "The neighbor's car got submerged in the rainwater.",
                    "Wo barish me chhap-chhap karke chal raha hai." to "He is walking with a splash-splash sound in the rain.",
                    "Usne thand ki वजह se aag jalayi hai." to "He has lit a fire because of the cold.",
                    "Wo umas me bina AC ke nahi so pata." to "He cannot sleep without the AC in the humidity.",
                    "Uska poora aangan keechad se bhar gaya hai." to "His entire courtyard is filled with mud.",
                    "Hawa bohot tez chal rahi hai." to "The wind is blowing very fast.",
                    "Wo garmi me hamesha suti kapde pehanta hai." to "He always wears cotton clothes in the summer.",
                    "Kal raat bohot tez toofan aaya tha." to "A very strong storm had come last night.",
                    "Wo pasine me poora bheeg gaya tha." to "He was completely drenched in sweat.",
                    "Usne barish me pakode banaye hain." to "She has made fritters in the rain.",
                    "Wo thand se bachne ke liye rajai me ghusa hai." to "He has tucked into the quilt to escape the cold.",
                    "Uske kapde hawa me ud gaye." to "His clothes blew away in the wind.",
                    "Wo roz subah thandi hawa me walk par jata hai." to "He goes for a walk in the cool breeze every morning.",
                    "Aaj dhoop bilkul nahi nikli hai." to "The sun hasn't come out at all today.",
                    "Wo barish rukne ke baad hi jayega." to "He will go only after the rain stops.",
                    "Uska chehra dhoop me laal ho gaya hai." to "His face has turned red in the sun.",
                    "Wo garmi ki chhuttiyo me pahado par gaya hai." to "He has gone to the mountains during the summer vacation."
                )
                t15_3rdPerson.forEach { (hindi, eng) ->
                    dao.insertLessonSentence(LessonSentence(
                        lessonId = t15Id,
                        personTag = "3rd Person",
                        promptHindi = hindi,
                        promptQuestion = "Say about third person: $hindi",
                        expectedText = eng
                    ))
                }
            }

            private suspend fun populateGymSentencesIfEmpty(dao: TinklDao) {
                dao.insertGymSentence(SpeakingGymSentence(
                    level = 1,
                    category = "Shadowing",
                    promptHindi = "Say this in English:",
                    promptQuestion = "I am going to work.",
                    expectedSentence = "I am going to work."
                ))
                dao.insertGymSentence(SpeakingGymSentence(
                    level = 1,
                    category = "Shadowing",
                    promptHindi = "Say this in English:",
                    promptQuestion = "I need a cup of tea.",
                    expectedSentence = "I need a cup of tea."
                ))

                dao.insertGymSentence(SpeakingGymSentence(
                    level = 2,
                    category = "Q&A",
                    promptHindi = "Tinkl Question:",
                    promptQuestion = "Where are you going right now?",
                    expectedSentence = "I am going to the market."
                ))

                dao.insertGymSentence(SpeakingGymSentence(
                    level = 3,
                    category = "FollowUp",
                    promptHindi = "Follow-up Question:",
                    promptQuestion = "How do you go to your office?",
                    expectedSentence = "I go to office by bus every day."
                ))

                dao.insertGymSentence(SpeakingGymSentence(
                    level = 4,
                    category = "ThinkInEnglish",
                    promptHindi = "Think in English (Hindi situation): Aapko apne friend ko batana hai ki aap 10 minute late honge.",
                    promptQuestion = "Situation: Tell your friend you will be late.",
                    expectedSentence = "I will be ten minutes late.",
                    hintPattern = "I will be + [time] + late."
                ))

                dao.insertGymSentence(SpeakingGymSentence(
                    level = 5,
                    category = "1MinChallenge",
                    promptHindi = "1-Minute Speaking Challenge:",
                    promptQuestion = "Topic: Describe your daily routine from morning to evening.",
                    expectedSentence = "I wake up early in the morning. I brush my teeth and drink water. Then I go to office and return in the evening."
                ))
            }

            suspend fun populateDatabase(dao: TinklDao) {
                dao.insertQuestion(Question(
                    subject = "Math",
                    text = "What is 2 + 2?",
                    optionA = "3", optionB = "4", optionC = "5", optionD = "6",
                    correctOption = "B",
                    explanation = "Simple addition: 2 + 2 = 4."
                ))

                populateTopicLessonsIfEmpty(dao)
                populateGymSentencesIfEmpty(dao)
            }
        }
    }
}
