package com.activity.mining.mappers;

import com.activity.mining.Activity;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class CommandEventMapperTest {

    private static final String CSV_PATH = "./CommandIdToActivityMapping.csv";

    @Test
    public void loadTest() throws NoSuchFieldException, IllegalAccessException {
        CommandEventMapper mapper = new CommandEventMapper(CSV_PATH);
        Field mappingStructure = mapper.getClass().getDeclaredField("commandIdToActivityMapping");
        mappingStructure.setAccessible(true);
        HashMap<String, Activity> map = (HashMap<String, Activity>) mappingStructure.get(mapper);
        assertTrue(map.size()>0);
    }

}
