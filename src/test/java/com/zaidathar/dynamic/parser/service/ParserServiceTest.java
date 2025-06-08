package com.zaidathar.dynamic.parser;
import com.zaidathar.dynamic.parser.config.MappingConfigLoader;
import com.zaidathar.dynamic.parser.dto.UserData;
import com.zaidathar.dynamic.parser.dto.UserProfile;
import com.zaidathar.dynamic.parser.engine.MappingEngine;
import com.zaidathar.dynamic.parser.service.ParserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
public class ParserServiceTest {
    private ParserService service;

    @BeforeEach
    public void setup() throws Exception {
        MappingConfigLoader loader = new MappingConfigLoader();
        loader.init();
        service = new ParserService(loader, new MappingEngine());
    }

    @Test
    public void testConversion() throws Exception {
        UserData userData = new UserData();
        userData.setName("John");
        userData.setAge(30);
        userData.setProfile( "http://example.com/image.jpg");
        userData.setContact( "123456");

        UserProfile profile = (UserProfile) service.convert(userData, "UserData->UserProfile", UserProfile.class);

        assertEquals("Mr. John", profile.getFullName());
        assertEquals(30, profile.getUserAge());
        assertEquals("http://example.com/image.jpg", profile.getProfilePic());
        assertEquals("123456", profile.getContact());
        assertEquals( 360,profile.getAgeInMonths());
    }
}
