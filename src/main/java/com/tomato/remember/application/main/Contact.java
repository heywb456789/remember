package com.tomato.remember.application.main;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Contact {
    private String name;
    private String image;
    private String key;       // 영문명 (rohmoohyun, kimgeuntae)
    // 생성자, getter/setter
}