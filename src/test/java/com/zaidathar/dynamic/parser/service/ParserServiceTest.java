package com.zaidathar.dynamic.parser.service;

import com.zaidathar.dynamic.parser.config.MappingConfigLoader;
import com.zaidathar.dynamic.parser.config.MappingRule;
import com.zaidathar.dynamic.parser.dto.UserData;
import com.zaidathar.dynamic.parser.dto.UserProfile;
import com.zaidathar.dynamic.parser.engine.MappingEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ParserServiceTest {
    @InjectMocks
    private ParserService service;

    @Mock
    private MappingEngine mappingEngine;

    @Mock
    private MappingConfigLoader mockMappingConfigLoader;

    private MappingRule testMappingRule;

    @BeforeEach
    public void setup() {
        Map<String, String> fields = new HashMap<>();
        fields.put("fullName", "transform:fullNameWithPrefix");
        fields.put("userAge", "age");
        fields.put("ageInMonths", "transform:ageInMonths");
        fields.put("profilePic", "profile");
        fields.put("contact", "contact");

        testMappingRule = new MappingRule(fields);

        when(mockMappingConfigLoader.getRule("UserData->UserProfile")).thenReturn(testMappingRule);
    }


    @Test
    public void testConversion() throws Exception {
        UserData userData = new UserData();
        userData.setName("John");
        userData.setAge(30);
        userData.setProfile( "http://example.com/image.jpg");
        userData.setContact( "123456");

        UserProfile userProfile = new UserProfile();
        userProfile.setFullName("Mr. John");
        userProfile.setUserAge(30);
        userProfile.setAgeInMonths(360);
        userProfile.setContact("123456");
        userProfile.setProfilePic("http://example.com/image.jpg");
        when(mappingEngine.map(any(),any(),any())).thenReturn(userProfile);

        UserProfile profile = (UserProfile) service.convert(userData, "UserData->UserProfile", UserProfile.class);

        assertEquals("Mr. John", profile.getFullName());
        assertEquals(30, profile.getUserAge());
        assertEquals("http://example.com/image.jpg", profile.getProfilePic());
        assertEquals("123456", profile.getContact());
        assertEquals( 360,profile.getAgeInMonths());

        verify(mockMappingConfigLoader, times(1)).getRule("UserData->UserProfile");
        verify(mappingEngine,times(1)).map(any(),any(),any());
    }
}
