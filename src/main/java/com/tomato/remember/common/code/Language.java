package com.tomato.remember.common.code;

public enum Language {
    KO("한국어", "ko"),
    EN("English", "en"),
    AF("Afrikaans", "af"),
    AK("Akan", "ak"),
    ZH("中文", "zh"),
    JA("日本語", "ja"),
    ES("Español", "es"),
    FR("Français", "fr"),
    DE("Deutsch", "de"),
    RU("Русский", "ru"),
    AR("العربية", "ar"),
    HI("हिन्दी", "hi"),
    PT("Português", "pt"),
    IT("Italiano", "it"),
    NL("Nederlands", "nl"),
    SV("Svenska", "sv"),
    NO("Norsk", "no"),
    DA("Dansk", "da"),
    FI("Suomi", "fi"),
    PL("Polski", "pl"),
    CS("Čeština", "cs"),
    SK("Slovenčina", "sk"),
    HU("Magyar", "hu"),
    RO("Română", "ro"),
    BG("Български", "bg"),
    HR("Hrvatski", "hr"),
    SR("Српски", "sr"),
    SL("Slovenščina", "sl"),
    ET("Eesti", "et"),
    LV("Latviešu", "lv"),
    LT("Lietuvių", "lt"),
    EL("Ελληνικά", "el"),
    TR("Türkçe", "tr"),
    HE("עברית", "he"),
    TH("ไทย", "th"),
    VI("Tiếng Việt", "vi"),
    ID("Bahasa Indonesia", "id"),
    MS("Bahasa Melayu", "ms"),
    TA("தமிழ்", "ta"),
    TE("తెలుగు", "te"),
    BN("বাংলা", "bn"),
    UR("اردو", "ur"),
    FA("فارسی", "fa"),
    SW("Kiswahili", "sw"),
    AM("አማርኛ", "am"),
    YO("Yorùbá", "yo"),
    ZU("isiZulu", "zu"),
    XH("isiXhosa", "xh");
    
    private final String displayName;
    private final String code;
    
    Language(String displayName, String code) {
        this.displayName = displayName;
        this.code = code;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getCode() {
        return code;
    }
    
    public static Language fromCode(String code) {
        for (Language language : values()) {
            if (language.code.equals(code)) {
                return language;
            }
        }
        return KO; // 기본값
    }
}