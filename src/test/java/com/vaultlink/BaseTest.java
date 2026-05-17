package com.vaultlink;

import com.vaultlink.entity.Category;
import com.vaultlink.entity.Document;
import com.vaultlink.entity.User;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@SpringBootTest(classes = com.vaultlink.vaultlink.VaultlinkApplication.class)
@Transactional
public abstract class BaseTest {

    protected User createTestUser() {
        User user = new User();
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail("test@vaultlink.com");
        user.setPassword("hashedpassword123");
        user.setIsActive(true);
        user.setCreatedAt(LocalDateTime.now());
        return user;
    }

    protected Document createTestDocument(User user, LocalDate expiryDate) {
        Document doc = new Document();
        doc.setTitle("Test Document");
        doc.setDescription("Test Description");
        doc.setIssueDate(LocalDate.now().minusYears(1));
        doc.setExpiryDate(expiryDate);
        doc.setIsActive(true);
        doc.setUser(user);
        doc.setCreatedAt(LocalDateTime.now());
        return doc;
    }

    protected Category createTestCategory() {
        Category cat = new Category();
        cat.setName("Test Category");
        cat.setDescription("Test Category Description");
        cat.setCreatedAt(LocalDateTime.now());
        return cat;
    }
}
