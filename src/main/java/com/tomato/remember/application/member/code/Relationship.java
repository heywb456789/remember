package com.tomato.remember.application.member.code;

public enum Relationship {
    SELF("본인"),
    SPOUSE("배우자"),
    FATHER("부"),
    MOTHER("모"),
    CHILD("자"),
    DAUGHTER_IN_LAW("자부"),
    SON_IN_LAW("사위"),
    SPOUSE_FATHER("배우자부"),
    SPOUSE_MOTHER("배우자모"),
    SIBLING("형제/자매"),
    GRANDCHILD("손"),
    GREAT_GRANDCHILD("증손"),
    GRANDFATHER("조부"),
    GRANDMOTHER("조모"),
    GREAT_GRANDFATHER("증조부"),
    GREAT_GRANDMOTHER("증조모"),
    COHABITANT("동거인"),
    OTHER("기타");
    
    private final String displayName;
    
    Relationship(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}