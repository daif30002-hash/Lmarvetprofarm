package com.example.data.api

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiApi {
    private const val TAG = "GeminiApi"
    private const val MODEL_NAME = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun consultVeterinarian(
        ageInDays: Int,
        symptoms: List<String>,
        offlineDiagnosis: String,
        additionalQuery: String? = null
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "خطأ: لم يتم ضبط مفتاح Gemini API Key في لوحة الأسرار (Secrets panel). الرجاء إدخاله لتفعيل الاستشارة الذكية."
        }

        // Construct a highly detailed system instructions guiding the AI to output professional veterinarian advice on behalf of Dr. Daifallah Al-Hasani's platform
        val systemInstruction = """
            أنت المساعد الذكي المتطور لمنصة لومار فيت برو فارم (Lmar Vet ProFarm) المطورة تحت إشراف د. ضيف الله الحسني.
            مهمتك هي تقديم استشارات بيطرية دقيقة واحترافية لمربي الدجاج اللاحم في الأنظمة المغلقة (Closed Poultry Farms).
            التواصل يجب أن يكون باللغة العربية بأسلوب علمي، رصين، واحترافي، مع تشجيع المستخدم وتقديم بروتوكولات حيوية دقيقة وتوجيهات للأمن الحيوي.
            تذكر دائماً أن تؤكد أن هذه استشارة أولية ذكية بإمكانيات الذكاء الاصطناعي ويجب التواصل مع الطبيب البيطري المتخصص د. ضيف الله الحسني عند الحالات الطارئة.
        """.trimIndent()

        val prompt = if (additionalQuery.isNullOrEmpty()) {
            """
                الرجاء تقييم الحالة الصحية التالية للقطيع:
                - عمر الدجاج: $ageInDays يوم.
                - الأعراض التي رصدها المربي الحقلية: ${symptoms.joinToString("، ")}.
                - التشخيص الأقلي والمحتمل المقترح من النظام المغلق: $offlineDiagnosis.
                
                الرجاء توفير تحليل طبي سريع يتضمن:
                1. تقييم مدى خطورة الحالة على بقية الطيور داخل العنبر المغلق.
                2. التفسير البيطري العلمي للأعراض المذكورة في هذا العمر وكيف يتداخل هذا مع النظام البيئي المغلق (التهوية، الحرارة، رطوبة الفرشة، الأمونيا).
                3. بروتوكول العلاج المقترح (العلاجي والوقائي والطبيعي مثل الزيوت العطرية أو الأحماض العضوية).
                4. إجراءات الأمن الحيوي (Biosecurity) العاجلة الموصى بها داخل العنبر المغلق للحد من الانتشار السريع.
                
                اكتب الإجابة بتنظيم ممتاز مع استخدام نقاط واضحة وعناوين بارزة.
            """.trimIndent()
        } else {
            """
                استشارة إضافية لقطيع دواجن لاحم في نظام مغلق (عمر الطير: $ageInDays يوم):
                الأعراض: ${symptoms.joinToString("، ")}
                التشخيص المقترح سابقاً: $offlineDiagnosis
                
                سؤال المربي:
                $additionalQuery
                
                أجب باحترافية بيطرية مطورة وبلغة عربية سليمة.
            """.trimIndent()
        }

        try {
            val requestBodyJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                })
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", systemInstruction)
                        })
                    })
                })
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = requestBodyJson.toString().toRequestBody(mediaType)

            val url = "$BASE_URL?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    Log.e(TAG, "Request failed: ${response.code} - $errBody")
                    return@withContext "عذراً، واجهنا خطأ أثناء التواصل مع خوادم الاستشارة الطبية الذكية. رمز الخطأ: ${response.code}"
                }

                val responseString = response.body?.string() ?: ""
                val jsonResponse = JSONObject(responseString)
                val candidates = jsonResponse.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val candidate = candidates.getJSONObject(0)
                    val content = candidate.optJSONObject("content")
                    if (content != null) {
                        val parts = content.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            return@withContext parts.getJSONObject(0).optString("text", "لم تتوفر استجابة نصية.")
                        }
                    }
                }
                return@withContext "لم يرد أي تشخيص علمي كافٍ من الذكاء الاصطناعي."
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error matching Gemini: ", e)
            return@withContext "حدث خطأ غير متوقع: ${e.localizedMessage}. تأكد من اتصال هاتفك بالإنترنت وقوة الإشارة."
        }
    }

    suspend fun predictFutureDiseases(
        ageInDays: Int,
        breed: String,
        systemType: String,
        environmentalSummary: String,
        risksFoundText: String
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "خطأ: لم يتم ضبط مفتاح Gemini API Key في لوحة الأسرار (Secrets panel). الرجاء إدخاله لتفعيل التنبؤ الذكي بالأمراض والمخاطر."
        }

        val systemInstruction = """
            أنت المساعد الذكي المتطور لمنصة لومار فيت برو فارم (Lmar Vet ProFarm) المطورة تحت إشراف د. ضيف الله الحسني.
            مهمتك هي تقديم تنبؤات وبائية بيطرية مستقبلية استباقية دقيقة واحترافية لمربي الدجاج اللاحم في الأنظمة المغلقة والتقليدية، بناءً على المؤشرات البيئية والتراكمية المدخلة ومطابقتها بأحدث أنماط التعلم الآلي والقرارات البيطرية المعتمدة.
            التواصل يجب أن يكون باللغة العربية بأسلوب علمي، رصين، واحترافي، مع صياغة خطة وقائية استباقية متكاملة لصد الخطر المحتمل بالأمن الحيوي، وصيانة التهوية ومكافحة تدهور معامل التحويل FCR.
        """.trimIndent()

        val prompt = """
            الرجاء دراسة وتحليل المخاطر الوبائية المستقبلية المتوقعة للقطيع التالي وإعداد تقرير وقائي استباقي تفصيلي:
            - عمر الطيور الحالي: $ageInDays يوم.
            - السلالة المرباة: $breed.
            - نوع نظام الرعاية والحظيرة: ${if (systemType == "CLOSED") "نظام مغلق ومحكم (Closed Enclosure)" else "نظام عادي مفتوح"}.
            - ملخص المعايير البيئية والتراكمية للدفعة مؤخراً: $environmentalSummary.
            - توقعات الخطر والنسب الإحصائية الحالية المحسوبة بموجب المؤشرات الميدانية:
            $risksFoundText
            
            المطلوب إصدار تقرير طبي واستباقي متكامل يتضمن:
            1. الفحص العلمي الدقيق للتكامل البيئي في العنبر (كيف تتداخل قراءة الحرارة والرطوبة والأمونيا الحالية مع تفعيل خطر الأمراض المتوقعة).
            2. تقييم مآلات الدورة المالي والفني (سلوك FCR ومخاطر تفشي النفوق في غضون 7-10 أيام إن لم يُتخذ الإجراء).
            3. جدول إجراءات عملية استباقية عاجلة (خطوات تقنية ملموسة للعلاجات الوقائية، التبريد، غسيل خطوط المياه بالأحماض العضوية، ضبط كثافة التحميل، التطهير البيئي).
            4. نقاط إرشادية حول الأمن الحيوي الوقائي بإشراف د. ضيف الله الحسني.
            
            اكتب التقرير بشكل علمي دقيق للغاية وبأعلى درجات التنظيم والجمالية مستخدماً العناوين الجانبية والنقط المريحة للقراءة.
        """.trimIndent()

        try {
            val requestBodyJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                })
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", systemInstruction)
                        })
                    })
                })
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = requestBodyJson.toString().toRequestBody(mediaType)

            val url = "$BASE_URL?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    Log.e(TAG, "Prediction failed: ${response.code} - $errBody")
                    return@withContext "عذراً، واجهنا خطأ أثناء التواصل مع خوادم التنبؤ الاستباقي. رمز الخطأ: ${response.code}"
                }

                val responseString = response.body?.string() ?: ""
                val jsonResponse = JSONObject(responseString)
                val candidates = jsonResponse.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val candidate = candidates.getJSONObject(0)
                    val content = candidate.optJSONObject("content")
                    if (content != null) {
                        val parts = content.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            return@withContext parts.getJSONObject(0).optString("text", "لم تتوفر استجابة تنبؤية نصية.")
                        }
                    }
                }
                return@withContext "لم يرد أي تقرير استباقي كافٍ من الذكاء الاصطناعي."
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in predictFutureDiseases: ", e)
            return@withContext "حدث خطأ غير متوقع: ${e.localizedMessage}. يرجى التحقق من اتصال شبكة الإنترنت."
        }
    }
}
