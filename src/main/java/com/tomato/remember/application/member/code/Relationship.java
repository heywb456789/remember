package com.tomato.remember.application.member.code;

public enum Relationship {
    FATHER("아버지"),
    MOTHER("어머니"),
    GRANDFATHER("할아버지"),
    GRANDMOTHER("할머니"),
    HUSBAND("남편"),
    WIFE("아내"),
    SON("아들"),
    DAUGHTER("딸"),
    BROTHER("형/동생"),
    SISTER("누나/언니/여동생"),
    UNCLE("삼촌/외삼촌"),
    AUNT("이모/고모"),
    NEPHEW("조카(남)"),
    NIECE("조카(여)"),
    COUSIN("사촌"),
    FRIEND("친구"),
    OTHER("기타");
    
    private final String displayName;
    
    Relationship(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}