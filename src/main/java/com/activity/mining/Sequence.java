package com.activity.mining;

import java.io.Serializable;

public record Sequence(String sessionId, String sequencer, String sequence) implements Serializable {

    public int length(){
        return sequence.length();
    }

}
