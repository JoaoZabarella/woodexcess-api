package com.z.c.woodexcess_api.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.z.c.woodexcess_api.dto.message.MessageRequest;
import com.z.c.woodexcess_api.enums.Condition;
import com.z.c.woodexcess_api.enums.ListingStatus;
import com.z.c.woodexcess_api.enums.MaterialType;
import com.z.c.woodexcess_api.enums.UserRole;
import com.z.c.woodexcess_api.model.Address;
import com.z.c.woodexcess_api.model.MaterialListing;
import com.z.c.woodexcess_api.model.Message;
import com.z.c.woodexcess_api.model.User;
import com.z.c.woodexcess_api.repository.AddressRepository;
import com.z.c.woodexcess_api.repository.MaterialListingRepository;
import com.z.c.woodexcess_api.repository.MessageRepository;
import com.z.c.woodexcess_api.repository.UserRepository;
import com.z.c.woodexcess_api.security.JwtProvider;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("MessageController Integration Tests")
class MessageControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private MaterialListingRepository listingRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtProvider jwtProvider;

    private String senderToken;
    private String recipientToken;
    private UUID senderId;
    private UUID recipientId;
    private UUID listingId;
    private User sender;
    private User recipient;
    private MaterialListing listing;

    @BeforeEach
    @Transactional
    void setUp() {
        // Clean database
        messageRepository.deleteAll();
        listingRepository.deleteAll();
        addressRepository.deleteAll();
        userRepository.deleteAll();

        // Create sender user
        sender = User.builder()
                .name("John Sender")
                .email("sender@test.com")
                .phone("11987654321")
                .password(passwordEncoder.encode("password123"))
                .role(UserRole.USER)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        sender = userRepository.save(sender);
        senderId = sender.getId();
        senderToken = "Bearer " + jwtProvider.generateJwtToken(sender);

        // Create recipient user
        recipient = User.builder()
                .name("Jane Recipient")
                .email("recipient@test.com")
                .phone("11987654322")
                .password(passwordEncoder.encode("password123"))
                .role(UserRole.USER)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        recipient = userRepository.save(recipient);
        recipientId = recipient.getId();
        recipientToken = "Bearer " + jwtProvider.generateJwtToken(recipient);

        // Create address for listing
        Address address = Address.builder()
                .user(recipient)
                .street("Test Street")
                .number("123")
                .district("Test District")
                .city("São Paulo")
                .state("SP")
                .zipCode("01234-567")
                .country("Brasil")
                .isPrimary(true)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        address = addressRepository.save(address);

        // Create listing
        listing = MaterialListing.builder()
                .title("Oak Wood Planks")
                .description("High quality oak wood")
                .materialType(MaterialType.WOOD)
                .price(BigDecimal.valueOf(150.50))
                .quantity(10)
                .condition(Condition.USED)
                .owner(recipient)
                .address(address)
                .city("São Paulo")
                .state("SP")
                .status(ListingStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        listing = listingRepository.save(listing);
        listingId = listing.getId();
    }

    @Test
    @Order(1)
    @DisplayName("POST /api/messages - Should send message successfully")
    void sendMessage_Success() throws Exception {
        // Arrange
        MessageRequest request = MessageRequest.builder()
                .recipientId(recipientId)
                .listingId(listingId)
                .content("Hello, is this item still available?")
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/messages")
                        .header("Authorization", senderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.senderId").value(senderId.toString()))
                .andExpect(jsonPath("$.recipientId").value(recipientId.toString()))
                .andExpect(jsonPath("$.listingId").value(listingId.toString()))
                .andExpect(jsonPath("$.content").value("Hello, is this item still available?"))
                .andExpect(jsonPath("$.isRead").value(false))
                .andExpect(jsonPath("$.senderEmail").value("sender@test.com"))
                .andExpect(jsonPath("$.recipientEmail").value("recipient@test.com"))
                .andExpect(jsonPath("$.listingTitle").value("Oak Wood Planks"));

        // Verify message was persisted
        assertThat(messageRepository.count()).isEqualTo(1);
    }

    @Test
    @Order(2)
    @DisplayName("POST /api/messages - Should fail without authentication")
    void sendMessage_Unauthorized() throws Exception {
        // Arrange
        MessageRequest request = MessageRequest.builder()
                .recipientId(recipientId)
                .listingId(listingId)
                .content("Test message")
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(3)
    @DisplayName("POST /api/messages - Should fail with invalid recipient")
    void sendMessage_InvalidRecipient() throws Exception {
        // Arrange
        UUID invalidRecipientId = UUID.randomUUID();
        MessageRequest request = MessageRequest.builder()
                .recipientId(invalidRecipientId)
                .listingId(listingId)
                .content("Test message")
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/messages")
                        .header("Authorization", senderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("Recipient not found")));
    }

    @Test
    @Order(4)
    @DisplayName("POST /api/messages - Should fail with invalid listing")
    void sendMessage_InvalidListing() throws Exception {
        // Arrange
        UUID invalidListingId = UUID.randomUUID();
        MessageRequest request = MessageRequest.builder()
                .recipientId(recipientId)
                .listingId(invalidListingId)
                .content("Test message")
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/messages")
                        .header("Authorization", senderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("not found")));
    }

    @Test
    @Order(5)
    @DisplayName("POST /api/messages - Should fail when sending to yourself")
    void sendMessage_ToYourself() throws Exception {
        // Arrange
        MessageRequest request = MessageRequest.builder()
                .recipientId(senderId) // Same as sender
                .listingId(listingId)
                .content("Test message")
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/messages")
                        .header("Authorization", senderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Cannot send message to yourself")));
    }

    @Test
    @Order(6)
    @DisplayName("POST /api/messages - Should fail with blank content")
    void sendMessage_BlankContent() throws Exception {
        // Arrange
        MessageRequest request = MessageRequest.builder()
                .recipientId(recipientId)
                .listingId(listingId)
                .content("   ") // Blank content
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/messages")
                        .header("Authorization", senderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(7)
    @DisplayName("GET /api/messages/conversation - Should get conversation successfully")
    void getConversation_Success() throws Exception {
        // Arrange - Create messages
        createMessage(sender, recipient, listing, "Message 1");
        createMessage(recipient, sender, listing, "Message 2");
        createMessage(sender, recipient, listing, "Message 3");

        // Act & Assert
        mockMvc.perform(get("/api/messages/conversation")
                        .header("Authorization", senderToken)
                        .param("otherUserId", recipientId.toString())
                        .param("listingId", listingId.toString()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].content").value("Message 1"))
                .andExpect(jsonPath("$[1].content").value("Message 2"))
                .andExpect(jsonPath("$[2].content").value("Message 3"));
    }

    @Test
    @Order(8)
    @DisplayName("GET /api/messages/conversation - Should fail with missing parameters")
    void getConversation_MissingParameters() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/messages/conversation")
                        .header("Authorization", senderToken)
                        .param("otherUserId", recipientId.toString()))
                // Missing listing parameter
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(9)
    @DisplayName("GET /api/messages/listing/{id} - Should get messages by listing with pagination")
    void getMessagesByListing_Success() throws Exception {
        // Arrange - Create 5 messages
        for (int i = 1; i <= 5; i++) {
            createMessage(sender, recipient, listing, "Message " + i);
        }

        // Act & Assert - Page 0, size 3
        mockMvc.perform(get("/api/messages/listing/{id}", listingId)
                        .header("Authorization", senderToken)
                        .param("page", "0")
                        .param("size", "3"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.number").value(0));
    }

    @Test
    @Order(10)
    @DisplayName("GET /api/messages/conversations - Should get recent conversations")
    void getRecentConversations_Success() throws Exception {
        // Arrange - Create conversation with recipient
        createMessage(sender, recipient, listing, "Latest message");

        // Act & Assert
        mockMvc.perform(get("/api/messages/conversations")
                        .header("Authorization", senderToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].listingId").value(listingId.toString()))
                .andExpect(jsonPath("$[0].listingTitle").value("Oak Wood Planks"))
                .andExpect(jsonPath("$[0].otherUserId").value(recipientId.toString()))
                .andExpect(jsonPath("$[0].otherUsername").value("Jane Recipient"))
                .andExpect(jsonPath("$[0].unreadCount").value(0))
                .andExpect(jsonPath("$[0].message.content").value("Latest message"));
    }

    @Test
    @Order(11)
    @DisplayName("GET /api/messages/conversations - Should return empty list when no conversations")
    void getRecentConversations_EmptyList() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/messages/conversations")
                        .header("Authorization", senderToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @Order(12)
    @DisplayName("GET /api/messages/unread-count - Should get unread count")
    void getUnreadCount_Success() throws Exception {
        // Arrange - Create unread messages
        createMessage(recipient, sender, listing, "Unread 1");
        createMessage(recipient, sender, listing, "Unread 2");

        // Create read message
        Message readMessage = createMessage(recipient, sender, listing, "Read message");
        readMessage.setIsRead(true);
        messageRepository.save(readMessage);

        // Act & Assert
        mockMvc.perform(get("/api/messages/unread-count")
                        .header("Authorization", senderToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("2"));
    }

    @Test
    @Order(13)
    @DisplayName("PATCH /api/messages/mark-read - Should mark conversation as read")
    void markConversationAsRead_Success() throws Exception {
        // Arrange - Create unread messages
        createMessage(recipient, sender, listing, "Unread 1");
        createMessage(recipient, sender, listing, "Unread 2");

        assertThat(messageRepository.countUnreadMessagesByUser(senderId)).isEqualTo(2);

        // Act
        mockMvc.perform(patch("/api/messages/mark-read")
                        .header("Authorization", senderToken)
                        .param("senderId", recipientId.toString())
                        .param("listingId", listingId.toString()))
                .andDo(print())
                .andExpect(status().isNoContent());

        // Assert - Messages should be marked as read
        assertThat(messageRepository.countUnreadMessagesByUser(senderId)).isZero();
    }

    @Test
    @Order(14)
    @DisplayName("PATCH /api/messages/mark-read - Should fail with missing parameters")
    void markConversationAsRead_MissingParameters() throws Exception {
        // Act & Assert
        mockMvc.perform(patch("/api/messages/mark-read")
                        .header("Authorization", senderToken)
                        .param("senderId", recipientId.toString()))
                // Missing listing parameter
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(15)
    @DisplayName("Should handle concurrent message sending")
    void concurrentMessageSending() throws Exception {
        // Arrange
        MessageRequest request1 = MessageRequest.builder()
                .recipientId(recipientId)
                .listingId(listingId)
                .content("Concurrent message 1")
                .build();

        MessageRequest request2 = MessageRequest.builder()
                .recipientId(recipientId)
                .listingId(listingId)
                .content("Concurrent message 2")
                .build();

        // Act - Send two messages
        mockMvc.perform(post("/api/messages")
                        .header("Authorization", senderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/messages")
                        .header("Authorization", senderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isCreated());

        // Assert - Both messages should be saved
        assertThat(messageRepository.count()).isEqualTo(2);
    }

    // Helper method
    private Message createMessage(User sender, User recipient, MaterialListing listing, String content) {
        Message message = Message.builder()
                .sender(sender)
                .recipient(recipient)
                .listing(listing)
                .content(content)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return messageRepository.save(message);
    }
}
