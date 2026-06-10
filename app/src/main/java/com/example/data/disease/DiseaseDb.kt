package com.example.data.disease

data class Disease(
    val id: String,
    val nameAr: String,
    val nameEn: String,
    val typeAr: String,
    val typeEn: String,
    val ageGroupAr: String,
    val ageGroupEn: String,
    val minAgeDays: Int,
    val maxAgeDays: Int,
    val symptomsAr: List<String>,
    val symptomsEn: List<String>,
    val causesAr: String,
    val causesEn: String,
    val diagnosisAr: String,
    val diagnosisEn: String,
    val treatmentAr: String,
    val treatmentEn: String,
    val preventionAr: String,
    val preventionEn: String
)

object DiseaseDb {
    val symptomsPoolAr = listOf(
        "إسهال أبيض كريمي",
        "انسداد مجمع الطير (Vent Pasting)",
        "التجمع والالتصاق بمصدر التدفئة",
        "الخمول والنوم المستمر",
        "فقدان الشهية والنفور من العلف",
        "صعوبة تنفس وغرغرة",
        "لهث وتنفس سريع بدون صوت غرغرة",
        "العطش الشديد للماء",
        "ضعف عام وعيون منغلقة",
        "إسهال مدمم أو برتقالي",
        "ريش متقصف وناشف ومبعثر",
        "شحوب العرف والدلايات",
        "انخفاض حاد في الوزن مع تراجع استهلاك العلف",
        "سعال وعطس وإفرازات أنفية",
        "سماع صوت رنين وغرغرة بالقصبة الهوائية",
        "انتفاخ العيون والوجه",
        "إسهال مائي مخضر",
        "أعراض عصبية (التواء الرقبة Torticollis)",
        "شلل الأرجل أو الأجنحة",
        "صوت حشرجة الصدر المستمر",
        "وجود غشاء ليفي أصفر على الكبد والقلب",
        "لهث شديد مع فتح الفم ورفع الأجنحة",
        "الاستلقاء التام على أرضية العنبر",
        "استهلاك مفرط للماء مع توقف الأكل",
        "انخفاض معدل التحويل بشكل مفاجئ"
    )

    val symptomsPoolEn = listOf(
        "White creamy diarrhea",
        "Venting paste / vent blockage",
        "Huddling near heaters / heat source",
        "Extreme sleepiness / lethargy",
        "Loss of appetite and feed refusal",
        "Difficulty in breathing / heavy gurgling",
        "Rapid gasping without gurgling sound",
        "Intense thirst / high water consumption",
        "General weakness with closed eyes",
        "Bloody or orange loose droppings",
        "Ruffled and dry feathers",
        "Pale combs and wattles",
        "Severe weight loss with drop in feed intake",
        "Coughing, sneezing and nasal discharge",
        "Tracheal rales and rattling noise",
        "Swollen eyes and facial edema",
        "Greenish watery diarrhea",
        "Nervous symptoms (twisted necks - torticollis)",
        "Paralysis of legs or wings",
        "Constant respiratory chest clicking",
        "Cardiac fibrin cover (yellow coating on liver/heart)",
        "Severe gasping, open beak and spread wings",
        "Lying flat on the litter floor",
        "Drastic drop in feed / excessive water intake",
        "Sudden drop in feed conversion metric"
    )

    val diseases = listOf(
        Disease(
            id = "salmonella",
            nameAr = "السالمونيلا (مرض الإسهال الأبيض / بولوورم)",
            nameEn = "Salmonellosis / Pullorum Disease",
            typeAr = "بكتيري",
            typeEn = "Bacterial",
            ageGroupAr = "عمر 1-7 أيام",
            ageGroupEn = "1-7 days",
            minAgeDays = 1,
            maxAgeDays = 7,
            symptomsAr = listOf("إسهال أبيض كريمي", "انسداد مجمع الطير (Vent Pasting)", "التجمع والالتصاق بمصدر التدفئة", "الخمول والنوم المستمر", "فقدان الشهية والنفور من العلف"),
            symptomsEn = listOf("White creamy diarrhea", "Venting paste / vent blockage", "Huddling near heaters / heat source", "Extreme sleepiness / lethargy", "Loss of appetite and feed refusal"),
            causesAr = "بكتيريا Salmonella pullorum التي تنتقل من الأمهات عبر البيض أو بسبب تلوث الفقاسات والمعدات في المزرعة المغلقة.",
            causesEn = "Salmonella pullorum bacteria transmitted vertically from breeder flock through egg or via hatchery and equipment contamination in closed house.",
            diagnosisAr = "البحث عن أعراض الموت الفجائي في الكتاكيت وتضخم الكبد مع بقع بيضاء. مخبرياً: العزل البكتيري والتأكيد المخبري.",
            diagnosisEn = "Finding high chick mortality during first week, enlarged liver with necrotic white foci. Lab diagnosis: Bacterial isolation and PCR identification.",
            treatmentAr = "استخدام المضادات الحيوية المناسبة مثل (إنروفلوكساسين Enrofloxacin أو كولستين Colistin) مع تقديم فيتامينات لرفع المناعة ومعوضات جفاف.",
            treatmentEn = "Broad-spectrum antibiotics (Enrofloxacin, Colistin, or Neomycin) for 3-5 days combined with electrolytes, vitamins, and probiotics to rebuild gut balance.",
            preventionAr = "شراء الكتاكيت من مصدر خالٍ من السالمونيلا، تعقيم العنبر المغلق جيداً قبل الدفعة، تصفية مياه الشرب وفحصها لصد البكتيريا.",
            preventionEn = "Source chicks from salmonella-certified parent stock. Strictly sanitize closed facility before batch entry. Chlorinate/acidify drinking water."
        ),
        Disease(
            id = "aspergillosis",
            nameAr = "التهاب الرئة الفطري (مرض القلاع / برودر نيومونيا)",
            nameEn = "Aspergillosis / Brooder Pneumonia",
            typeAr = "بيئي / فطري",
            typeEn = "Environmental / Fungal",
            ageGroupAr = "عمر 1-7 أيام",
            ageGroupEn = "1-7 days",
            minAgeDays = 1,
            maxAgeDays = 7,
            symptomsAr = listOf("لهث وتنفس سريع بدون صوت غرغرة", "العطش الشديد للماء", "الخمول والنوم المستمر", "ضعف عام وعيون منغلقة"),
            symptomsEn = listOf("Rapid gasping without gurgling sound", "Intense thirst / high water consumption", "Extreme sleepiness / lethargy", "General weakness with closed eyes"),
            causesAr = "نمو فطر Aspergillus fumigatus بسبب الفرشة الرطبة المتعفنة، سوء التهوية وتراكم الرطوبة، أو الأعلاف المخزنة بشكل خاطئ.",
            causesEn = "Inhalation of Aspergillus fumigatus spores growing on wet/moldy litter, poor ventilation with high initial relative humidity, or moldy feed.",
            diagnosisAr = "ملاحظة تنفس سريع وصوت صفير جاف. بعد التشريح: رؤية عقد صفراء برئتي وأكياس تنفس الطيور تشبه حبات السمسم.",
            diagnosisEn = "Based on clinical respiratory distress without rales. Necropsy: Yellowish nodular foci in lungs and air sacs resembling sesame seeds.",
            treatmentAr = "لا يوجد علاج دوائي فعال 100% للفطريات الرئوية. يرفع العلف الملوث فوراً، وتُطهر مياه الشرب بمضادات الفطريات كالنحاس أو نيستاتين.",
            treatmentEn = "No effective systemic treatment. Immediately replace moldy litter/feed, administer organic acids or copper sulfate in water, or Nystatin if prescribed.",
            preventionAr = "المحافظة التامة على جفاف الفرشة، واستخدام مستويات تهوية منخفضة طاردة للرطوبة دون إحداث تيار هوائي مباشر على الكتاكيت.",
            preventionEn = "Maintain litter strictly dry. Manage internal humidity in closed environment. Use dust-free, clean wood shavings as bedding."
        ),
        Disease(
            id = "infectious_bronchitis",
            nameAr = "التهاب الشعب الهوائية المعدي (IB)",
            nameEn = "Infectious Bronchitis (IB)",
            typeAr = "فيروسي",
            typeEn = "Viral",
            ageGroupAr = "عمر 8-21 يومًا",
            ageGroupEn = "8-21 days",
            minAgeDays = 8,
            maxAgeDays = 21,
            symptomsAr = listOf("سعال وعطس وإفرازات أنفية", "سماع صوت رنين وغرغرة بالقصبة الهوائية", "انتفاخ العيون والوجه", "الخمول والنوم المستمر"),
            symptomsEn = listOf("Coughing, sneezing and nasal discharge", "Tracheal rales and rattling noise", "Swollen eyes and facial edema", "Extreme sleepiness / lethargy"),
            causesAr = "فيروس كورونا الدواجن (IBV) مفرط الانتشار، ينتقل بالجو بين العنابر أو بسبب خلل في الأمن الحيوي وضعف العزل الفيزيائي للعنبر.",
            causesEn = "Highly contagious Coronavirus (IBV) spread via aerosol, direct bird contact, or breach in biosecurity/vehicle containment.",
            diagnosisAr = "رصد أعراض تنفسية حادة مع رنين. تشريحياً: احتقان شديد والتهاب رغامي وغرغرة هواء. الفحص المصلي ELISA أو السريع PCR.",
            diagnosisEn = "Acute respiratory distress with high infectivity. Necropsy: Serious/catarrhal exudation in trachea, congested lungs. Lab: ELISA or RT-PCR.",
            treatmentAr = "لا يوجد علاج نوعي للفيروس. يتم علاج المضاعفات البكتيرية الثانوية بواسطة مضادات لمقاومة الكولاي (مثل التايلوزين Tylosin أو الدوكسيسيكلين) وموسعات الشعب.",
            treatmentEn = "No antiviral treatment. Control secondary bacterial infections with antibiotics (e.g. Tylosin, Doxycycline) and use bronchodilators with vitamin C/A.",
            preventionAr = "الالتزام ببرنامج التحصين الصارم (مثل لقاحات Ma5 أو H120 في عمر مبكر رشاً أو تقطيراً)، وضبط تيار الهواء والتهوية المتزنة بالعنبر.",
            preventionEn = "Strict design of vaccination schedule (IB-Ma5 or H120 drops or spray at day 1/12). Control temperature drops in closed environment."
        ),
        Disease(
            id = "coccidiosis",
            nameAr = "مرض الكوكسيديا المعوي",
            nameEn = "Coccidiosis",
            typeAr = "طفيلي",
            typeEn = "Parasitic",
            ageGroupAr = "عمر 22-45 يومًا",
            ageGroupEn = "22-45 days",
            minAgeDays = 15,
            maxAgeDays = 45,
            symptomsAr = listOf("إسهال مدمم أو برتقالي", "ريش متقصف وناشف ومبعثر", "شحوب العرف والدلايات", "انخفاض حاد في الوزن مع تراجع استهلاك العلف", "ضعف عام وعيون منغلقة"),
            symptomsEn = listOf("Bloody or orange loose droppings", "Ruffled and dry feathers", "Pale combs and wattles", "Severe weight loss with drop in feed intake", "General weakness with closed eyes"),
            causesAr = "طفيل الأيميريا (Eimeria) أحادي الخلية، يتكاثر بشراسة بالفرشة الرطبة في درجات حرارة 25-28 مئوية ورطوبة عالية بالعنبر.",
            causesEn = "Protozoan parasite of the genus Eimeria. Oocysts sporulate in wet litter when temperature is high, creating massive gut infections.",
            diagnosisAr = "ملاحظة خروج براز برتقالي لزج أو مدمم. تشريحياً: انتفاخ الأمعاء أو الأعورين وامتلائهما بالدم والتجلطات المخاطية.",
            diagnosisEn = "Observation of blood-stained litter. Necropsy: Gross lesions of necrosis and hemorrhages in intestine or cecal pouches.",
            treatmentAr = "تقديم مضادات الكوكسيديا الفعالة كـ (تولترازوريل Toltrazuril أو أمبروليوم Amprolium) بمياه الشرب لمدة 48 ساعة متواصلة مع فيتامين K3.",
            treatmentEn = "Administer anticoccidials (Toltrazuril or Amprolium) directly in drinking water for 2-3 days, supplemented with Vitamin K3 to stop intestinal bleeding.",
            preventionAr = "المحافظة الصارمة على جفاف الفرشة، قلب الفرشة، تهوية مدروسة لطرد رطوبة الأرضية، إضافة مضادات الكوكسيديا الوقائية في العلف (اليونوفورات).",
            preventionEn = "Keep litter dry to halt sporulation. Utilize proactive coccidiostats in feed formulation. Flush and disinfect flooring between cycles."
        ),
        Disease(
            id = "colibacillosis",
            nameAr = "الكوليباسيلوز (عدوى البي كولاي بالدم)",
            nameEn = "Colibacillosis (E. coli Infection)",
            typeAr = "بكتيري",
            typeEn = "Bacterial",
            ageGroupAr = "عمر 8-45 يومًا",
            ageGroupEn = "8-45 days",
            minAgeDays = 8,
            maxAgeDays = 45,
            symptomsAr = listOf("سماع صوت رنين وغرغرة بالقصبة الهوائية", "وجود غشاء ليفي أصفر على الكبد والقلب", "الخمول والنوم المستمر", "ضعف عام وعيون منغلقة", "صوت حشرجة الصدر المستمر"),
            symptomsEn = listOf("Tracheal rales and rattling noise", "Cardiac fibrin cover (yellow coating on liver/heart)", "Extreme sleepiness / lethargy", "General weakness with closed eyes", "Constant respiratory chest clicking"),
            causesAr = "بكتيريا Escherichia coli الانتهازية، تنشط عند تلوث المياه، أو نتيجة تراكم غاز الأمونيا الخانق وتدهور نظام تبادل الهواء بالعنبر.",
            causesEn = "Opportunistic Escherichia coli infection triggered primarily by high ammonia gas, dusty air, poor bio-security, or contaminated drinking system.",
            diagnosisAr = "تشريح الدواجن النافقة ورصد غشاء أصفر سميك يغطي الكبد والقلب (Fibrinous Pericarditis / Perihepatitis) مع تضخم الطحال.",
            diagnosisEn = "Necropsy findings: Thick yellow fibrinous layer covering heart and liver, congested air sacs. Confirmation via microbial culture.",
            treatmentAr = "استخدام مضادات حيوية متخصصة ومصرحة مثل (أموكسيسيلين Amoxicillin، جنتامايسين، أو فوسفوميسين) بعد إجراء اختبار الحساسية.",
            treatmentEn = "Antibiotics such as Amoxicillin, Fosfomycin, or Neomycin in drinking water for 3-5 days. Always ensure proper dosage to limit resistance.",
            preventionAr = "ضبط مستويات الأمونيا دون 10-15 جزء بالمليون عبر مستويات تهوية مستمرة، تعقيم دوري لخطوط المياه بالأحماض العضوية وكلور الدواجن.",
            preventionEn = "Ensure ammonia levels stay below 10-15 ppm. Periodically flush water lines with organic acids and maintain strict atmospheric hygiene."
        ),
        Disease(
            id = "newcastle",
            nameAr = "مرض النيوكاسل (الشوطة الوبائية)",
            nameEn = "Newcastle Disease (ND)",
            typeAr = "فيروسي",
            typeEn = "Viral",
            ageGroupAr = "عمر 22-45 يومًا",
            ageGroupEn = "22-45 days",
            minAgeDays = 22,
            maxAgeDays = 45,
            symptomsAr = listOf("إسهال مائي مخضر", "أعراض عصبية (التواء الرقبة Torticollis)", "شلل الأرجل أو الأجنحة", "لهث وتنفس سريع بدون صوت غرغرة", "ضعف عام وعيون منغلقة"),
            symptomsEn = listOf("Greenish watery diarrhea", "Nervous symptoms (twisted necks - torticollis)", "Paralysis of legs or wings", "Rapid gasping without gurgling sound", "General weakness with closed eyes"),
            causesAr = "فيروس نيوكاسل (NDV) شديد الضراوة والسرعة، ينتقل عبر الطيور البرية والخلل التام بالأمن الحيوي الخارجي للمزرعة.",
            causesEn = "Highly virulent Avian Paramyxovirus-1 (NDV) transmitted from wild birds or severe failure in external secure farm access fencing.",
            diagnosisAr = "رصد موت متسارع وصاعق مع أعراض عصبية كالتواء الرأس. تشريحياً: بقع نزفية على قمم غدد المعدة الغدية (Proventriculus).",
            diagnosisEn = "Sudden extreme mortality with neurological twist. Post-mortem: Hemorphic pinpoints on the tip of proventriculus glands. Lab: HI or Real-time PCR.",
            treatmentAr = "لا يوجد علاج دوائي للفيروس. عند التفشي الحاد للضراوة الشديدة، يتم التحصين الاضطراري باللقاح الزيتي أو الهيتشنر فجأة مع رافع مناعة قوي جداً وجرعات موسعات شعب ومضاد ميكروبي رديف.",
            treatmentEn = "No specific cure. During mild outbreak, emergency vaccination with Clone 30/Lasota might be initiated with immounostimulants and tracheal relief.",
            preventionAr = "برنامج تطعيم صارم بأعمار (7، 14، 18، 28 يوم) باستخدام لقاحات لاسوتة وكلون وهيتشنر والتحصين الزيتي الميت المزدوج بكتف الطائر.",
            preventionEn = "Mandatory live/inactivated vaccine schedule (ND-LaSota, Clone 30). Strict gate sanitization, vehicle showers, and wire netting against sparrows."
        ),
        Disease(
            id = "heat_stress",
            nameAr = "الإجهاد الحراري الحاد",
            nameEn = "Acute Heat Stress",
            typeAr = "بيئي / إنتاجي",
            typeEn = "Environmental",
            ageGroupAr = "عمر 22-45 يومًا",
            ageGroupEn = "22-45 days",
            minAgeDays = 22,
            maxAgeDays = 45,
            symptomsAr = listOf("لهث شديد مع فتح الفم ورفع الأجنحة", "الاستلقاء التام على أرضية العنبر", "استهلاك مفرط للماء مع توقف الأكل", "انخفاض حاد في الوزن مع تراجع استهلاك العلف"),
            symptomsEn = listOf("Severe gasping, open beak and spread wings", "Lying flat on the litter floor", "Drastic drop in feed / excessive water intake", "Severe weight loss with drop in feed intake"),
            causesAr = "ارتفاع درجة الحرارة داخل العنبر فوق 32 مئوية مع رطوبة عالية، وخلل في ألواح التبريد التبخيري (Cooling Pads) أو توقف مراوح الشفط.",
            causesEn = "Ambient nursery temperature exceeding 31-33°C coupled with high relative humidity, failure of evaporative pad cycles, or ventilation breakdown.",
            diagnosisAr = "رصد الطيور تلهث بشدة مستلقية على الأرض بكثافة. تشريحياً: احتقان عضلات الصدر ونزيف بالرئتين وسيولة بالدم.",
            diagnosisEn = "Observation of posture: panting flat on bedding. Post-mortem signs include dark breast muscle, congested lungs, and watery blood fluid.",
            treatmentAr = "تشغيل التبريد الاحتياطي فوراً، وزيادة سرعة الرياح بالعنبر، وتقديم فيتامين C ومحلول البيكربونات وسكريات في مياه الشرب الباردة ميكانيكياً.",
            treatmentEn = "Restore cooling system immediately. Increase tunnel design wind speed (target 2.5 m/s). Add Vitamin C, Sodium Bicarbonate, and ice-cool water.",
            preventionAr = "صيانة دورية للمولدات ومضخات التبريد والمراوح، ضبط مستويات كثافة الطيور (التحميل بمتر مربع) لتأمين مسافة كافية لتبديد حرارة الأجسام.",
            preventionEn = "Continuous backup generator testing. Calibrate digital climate controller. Reduce stocking density under summer conditions."
        )
    )

    fun diagnose(selectedSymptoms: List<String>, ageDays: Int): List<Pair<Disease, Double>> {
        if (selectedSymptoms.isEmpty()) return emptyList()
        val results = mutableListOf<Pair<Disease, Double>>()
        for (disease in diseases) {
            // Check age group overlap
            if (ageDays < disease.minAgeDays || ageDays > disease.maxAgeDays) {
                // If completely out of normal bracket, reduce score but don't strictly exclude
            }

            // Calculate overlap between selected symptoms and disease symptoms (Ar & En)
            val selectedLowerEn = selectedSymptoms.map { it.lowercase().trim() }
            val diseaseSymptomsEnLower = disease.symptomsEn.map { it.lowercase().trim() }
            val diseaseSymptomsArLower = disease.symptomsAr.map { it.trim() }

            val matchCountEn = selectedLowerEn.count { it in diseaseSymptomsEnLower }
            val matchCountAr = selectedLowerEn.count { it in diseaseSymptomsArLower || it in selectedSymptoms } // fallback Ar

            val finalMatchCount = maxOf(matchCountEn, matchCountAr)
            if (finalMatchCount > 0) {
                // Base probability relative to match count
                val baseProb = (finalMatchCount.toDouble() / disease.symptomsEn.size.toDouble()) * 100.0
                // Age match factor: increase if age inside the bracket
                val ageFactor = if (ageDays >= disease.minAgeDays && ageDays <= disease.maxAgeDays) 1.1 else 0.7
                var finalProb = baseProb * ageFactor
                if (finalProb > 99.0) finalProb = 99.0
                if (finalProb < 5.0) finalProb = 5.0
                results.add(Pair(disease, Math.round(finalProb * 10.0) / 10.0))
            }
        }
        return results.sortedByDescending { it.second }
    }
}
