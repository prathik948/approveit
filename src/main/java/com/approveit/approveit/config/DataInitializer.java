package com.approveit.approveit.config;

import com.approveit.approveit.enums.Role;
import com.approveit.approveit.model.User;
import com.approveit.approveit.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public DataInitializer(UserRepository userRepository,
                           ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(String... args) throws Exception {

        InputStream inputStream =
                new ClassPathResource("users.json").getInputStream();

        JsonNode root = objectMapper.readTree(inputStream);

        JsonNode users = root.path("users");

        for (JsonNode userNode : users) {

            String userId = userNode.path("id").asText();
            String name = userNode.path("name").asText();

            Set<Role> roles = new HashSet<>();

            for (JsonNode roleNode : userNode.path("roles")) {
                String roleName = roleNode.asText()
                        .toUpperCase(Locale.ROOT);

                roles.add(Role.valueOf(roleName));
            }

            // Avoid inserting the same user again
            if (!userRepository.existsById(userId)) {

                User user = new User(userId, name, roles);

                userRepository.save(user);

                System.out.println("User added: " + userId);
            }
        }

        inputStream.close();

        System.out.println("User initialization completed!");
    }
}
