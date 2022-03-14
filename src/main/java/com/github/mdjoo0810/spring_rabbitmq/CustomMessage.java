package com.github.mdjoo0810.spring_rabbitmq;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CustomMessage {

    private String text;
    private int priority;
    private boolean secret;

    @Builder
    public CustomMessage(String text, int priority, boolean secret) {
        this.text = text;
        this.priority = priority;
        this.secret = secret;
    }
}
