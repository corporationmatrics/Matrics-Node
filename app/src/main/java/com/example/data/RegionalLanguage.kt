package com.example.data

/**
 * Regional Language Support for Indian Vernacular Speech Recognition,
 * Hinglish/Tanglish/Tenglish/Kanglish/Bengali/Marathi tokenization, and Offline STT.
 */
enum class RegionalLanguage(
    val code: String,
    val displayName: String,
    val nativeLabel: String,
    val speechTag: String,
    val samplePhrases: List<String>
) {
    HINGLISH(
        code = "hi_en",
        displayName = "Hinglish / Hindi",
        nativeLabel = "हिंग्लिश / हिंदी",
        speechTag = "hi-IN",
        samplePhrases = listOf(
            "DMart se 2kg basmati chawal 140 mein aur 500g Amul butter 250 liya UPI se",
            "Blinkit se 1 dabba dahi 45 aur 6 ande 60 rupees me order kiya",
            "Subah chai aur samosa 50 rupay cash diya"
        )
    ),
    TAMIL(
        code = "ta_IN",
        displayName = "Tamil / Tanglish",
        nativeLabel = "தமிழ் / Tanglish",
        speechTag = "ta-IN",
        samplePhrases = listOf(
            "Annanagar Nilgiris la 2 kilo ponni arisi 130 roobai ku vangunen GPay pannen",
            "Saravana Stores la 1 packet nandini paal 32 roobai cash",
            "Kaalai tiffin idli vadai 80 roobai paid by UPI"
        )
    ),
    TELUGU(
        code = "te_IN",
        displayName = "Telugu / Tenglish",
        nativeLabel = "తెలుగు / Tenglish",
        speechTag = "te-IN",
        samplePhrases = listOf(
            "Ratnadeep lo 1 kg sonamasoori biyyam 75 rupayalu and 500ml Vijaya paalu 34 PhonePe chesa",
            "Kirana shop lo 2 kg chekkara 90 rupees cash ichanu",
            "Morning tiffins dosa coffee 70 rupayalu paid on UPI"
        )
    ),
    KANNADA(
        code = "kn_IN",
        displayName = "Kannada / Kanglish",
        nativeLabel = "ಕನ್ನಡ / Kanglish",
        speechTag = "kn-IN",
        samplePhrases = listOf(
            "Nandini milk parlor nalli 1 litre haalu 44 roopayi mathu 500g mosaru 35 UPI madide",
            "More supermarket nalli 5 kg sona masoori akki 290 rupaayi paid by Card",
            "Majjige mathu tiffin 60 rupaayi cash kotte"
        )
    ),
    BENGALI(
        code = "bn_IN",
        displayName = "Bengali / Banglish",
        nativeLabel = "বাংলা / Banglish",
        speechTag = "bn-IN",
        samplePhrases = listOf(
            "Local bazar theke 2 kilo miniket chal 110 taka r 1 litre mustard oil 145 taka nilam UPI te",
            "Mishti dokan theke 500g rasgulla 150 taka cash dilam",
            "Morning cha aar luchi alur dom 65 taka GPay korlam"
        )
    ),
    MARATHI(
        code = "mr_IN",
        displayName = "Marathi",
        nativeLabel = "मराठी",
        speechTag = "mr-IN",
        samplePhrases = listOf(
            "D-Mart madhun 5 kilo kolam tandool 280 rupaye aani 1 litre amul doodh 66 ghetla UPI ne",
            "Kirana dukanat 2 kilo saakhar 88 rupaye aani tel 140 rupaye dilo",
            "Sakalcha chaha aani poha 45 rupaye cash dile"
        )
    ),
    ENGLISH(
        code = "en_IN",
        displayName = "English (India)",
        nativeLabel = "English",
        speechTag = "en-IN",
        samplePhrases = listOf(
            "Bought 2kg Basmati rice at 140 and 500g butter at 250 from DMart on UPI",
            "Ordered 12 organic eggs at 95 and Greek yogurt at 80 from Blinkit paid by Card",
            "Electricity bill 1850 rupees paid by Net Banking"
        )
    );

    companion object {
        fun fromCode(code: String): RegionalLanguage {
            return values().firstOrNull { it.code.equals(code, ignoreCase = true) } ?: HINGLISH
        }
    }
}
