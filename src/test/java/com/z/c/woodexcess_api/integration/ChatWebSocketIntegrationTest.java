package com.z.c.woodexcess_api.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.z.c.woodexcess_api.config.websocket.WebSocketRateLimitInterceptor;
import com.z.c.woodexcess_api.dto.message.ChatMessageDTO;
import com.z.c.woodexcess_api.model.enums.Condition;
import com.z.c.woodexcess_api.model.enums.ListingStatus;
import com.z.c.woodexcess_api.model.enums.MaterialType;
import com.z.c.woodexcess_api.model.enums.UserRole;
import com.z.c.woodexcess_api.model.Address;
import com.z.c.woodexcess_api.model.MaterialListing;
import com.z.c.woodexcess_api.model.User;
import com.z.c.woodexcess_api.repository.AddressRepository;
import com.z.c.woodexcess_api.repository.MaterialListingRepository;
import com.z.c.woodexcess_api.repository.MessageRepository;
import com.z.c.woodexcess_api.repository.UserRepository;
import com.z.c.woodexcess_api.security.JwtProvider;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("ChatWebSocket Integration Tests")
class ChatWebSocketIntegrationTest {

    private static final int CONNECTION_TIMEOUT_SECONDS = 5;
    private static final int MESSAGE_TIMEOUT_SECONDS = 3;
    private static final int SUBSCRIPTION_SETUP_MILLIS = 1000;
    private static final int MESSAGE_DELAY_MILLIS = 200;
    private static final int DISCONNECT_WAIT_MILLIS = 500;

    @LocalServerPort
    private int port;

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


    @Autowired
    private WebSocketRateLimitInterceptor rateLimitInterceptor;

    private WebSocketStompClient stompClient;
    private String wsUrl;
    private String senderToken;
    private String recipientToken;
    private UUID senderId;
    private UUID recipientId;
    private UUID listingId;

    private StompHeaders createStompHeaders(String token) {
        StompHeaders stompHeaders = new StompHeaders();
        if (token != null && !token.isEmpty()) {
            stompHeaders.add("Authorization", "Bearer " + token);
        }
        return stompHeaders;
    }

    @BeforeEach
    void setUp() {

        rateLimitInterceptor.clearBuckets();

        messageRepository.deleteAll();
        listingRepository.deleteAll();
        addressRepository.deleteAll();
        userRepository.deleteAll();

        wsUrl = String.format("ws://localhost:%d/ws", port);

        StandardWebSocketClient webSocketClient = new StandardWebSocketClient();
        SockJsClient sockJsClient = new SockJsClient(List.of(new WebSocketTransport(webSocketClient)));

        stompClient = new WebSocketStompClient(sockJsClient);

        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        converter.setObjectMapper(objectMapper);
        stompClient.setMessageConverter(converter);

        User sender = User.builder()
                .name("WebSocket Sender")
                .email("wssender@test.com")
                .phone("11987654321")
                .password(passwordEncoder.encode("password123"))
                .role(UserRole.USER)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        sender = userRepository.save(sender);
        senderId = sender.getId();
        senderToken = jwtProvider.generateToken(sender.getEmail());

        User recipient = User.builder()
                .name("WebSocket Recipient")
                .email("wsrecipient@test.com")
                .phone("11987654322")
                .password(passwordEncoder.encode("password123"))
                .role(UserRole.USER)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        recipient = userRepository.save(recipient);
        recipientId = recipient.getId();
        recipientToken = jwtProvider.generateToken(recipient.getEmail());

        Address address = Address.builder()
                .user(recipient)
                .street("WebSocket Test Street")
                .number("456")
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

        MaterialListing listing = MaterialListing.builder()
                .title("WebSocket Test Listing")
                .description("Test description")
                .materialType(MaterialType.WOOD)
                .price(BigDecimal.valueOf(100.00))
                .quantity(5)
                .condition(Condition.NEW)
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

    @AfterEach
    void tearDown() {
        if (stompClient != null) {
            stompClient.stop();
        }
    }

    @Test
    @Order(1)
    @DisplayName("Should connect to WebSocket with valid JWT token")
    void connectWebSocket_Success() throws Exception {
        BlockingQueue<String> connectionStatus = new ArrayBlockingQueue<>(1);

        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
        headers.add("Authorization", "Bearer " + senderToken);

        StompSession session = stompClient.connectAsync(
                wsUrl,
                headers,
                createStompHeaders(senderToken),
                new StompSessionHandlerAdapter() {
                    @Override
                    public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                        if (!connectionStatus.offer("CONNECTED")) {
                            throw new IllegalStateException("Failed to add connection status to queue");
                        }
                    }

                    @Override
                    public void handleException(StompSession session, StompCommand command,
                                                StompHeaders headers, byte[] payload, Throwable exception) {
                        if (!connectionStatus.offer("ERROR")) {
                            throw new IllegalStateException("Failed to add error status to queue");
                        }
                    }
                }
        ).get(CONNECTION_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        String status = connectionStatus.poll(MESSAGE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertThat(status).isEqualTo("CONNECTED");
        assertThat(session.isConnected()).isTrue();

        session.disconnect();
    }

    @Test
    @Order(2)
    @DisplayName("Should fail to connect without JWT token")
    void connectWebSocket_Unauthorized() throws Exception {
        BlockingQueue<String> connectionStatus = new ArrayBlockingQueue<>(1);
        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();

        try {
            stompClient.connectAsync(
                    wsUrl,
                    headers,
                    createStompHeaders(null),
                    new StompSessionHandlerAdapter() {
                        @Override
                        public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                            if (!connectionStatus.offer("CONNECTED")) {
                                throw new IllegalStateException("Failed to add connection status to queue");
                            }
                        }

                        @Override
                        public void handleTransportError(StompSession session, Throwable exception) {
                            if (!connectionStatus.offer("TRANSPORT_ERROR")) {
                                throw new IllegalStateException("Failed to add error status to queue");
                            }
                        }
                    }
            ).get(CONNECTION_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            String status = connectionStatus.poll(MESSAGE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            assertThat(status).isIn("TRANSPORT_ERROR", null);
        } catch (Exception e) {
            assertThat(e).isNotNull();
        }
    }

    @Test
    @Order(3)
    @DisplayName("Should send and receive message via WebSocket")
    void sendMessage_Success() throws Exception {
        BlockingQueue<ChatMessageDTO> receivedMessages = new ArrayBlockingQueue<>(10);

        WebSocketHttpHeaders senderHeaders = new WebSocketHttpHeaders();
        senderHeaders.add("Authorization", "Bearer " + senderToken);

        WebSocketHttpHeaders recipientHeaders = new WebSocketHttpHeaders();
        recipientHeaders.add("Authorization", "Bearer " + recipientToken);

        StompSession recipientSession = stompClient.connectAsync(
                wsUrl,
                recipientHeaders,
                createStompHeaders(recipientToken),
                new StompSessionHandlerAdapter() {
                }
        ).get(CONNECTION_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        recipientSession.subscribe("/user/queue/messages", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return ChatMessageDTO.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                if (!receivedMessages.offer((ChatMessageDTO) payload)) {
                    throw new IllegalStateException("Failed to add message to queue");
                }
            }
        });

        StompSession senderSession = stompClient.connectAsync(
                wsUrl,
                senderHeaders,
                createStompHeaders(senderToken),
                new StompSessionHandlerAdapter() {
                }
        ).get(CONNECTION_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        senderSession.subscribe("/user/queue/messages", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return ChatMessageDTO.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                if (!receivedMessages.offer((ChatMessageDTO) payload)) {
                    throw new IllegalStateException("Failed to add message to queue");
                }
            }
        });

        Thread.sleep(SUBSCRIPTION_SETUP_MILLIS);

        ChatMessageDTO message = ChatMessageDTO.builder()
                .id(null)
                .senderId(senderId)
                .recipientId(recipientId)
                .listingId(listingId)
                .content("Hello via WebSocket!")
                .senderName(null)
                .listingTitle(null)
                .timestamp(LocalDateTime.now())
                .build();

        senderSession.send("/app/chat.send", message);

        ChatMessageDTO received1 = receivedMessages.poll(CONNECTION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        ChatMessageDTO received2 = receivedMessages.poll(CONNECTION_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        assertThat(received1).isNotNull();
        assertThat(received2).isNotNull();

        boolean foundMessage = (received1.content().equals("Hello via WebSocket!") ||
                received2.content().equals("Hello via WebSocket!"));
        assertThat(foundMessage).isTrue();

        assertThat(messageRepository.count()).isGreaterThan(0);

        senderSession.disconnect();
        recipientSession.disconnect();
    }

    @Test
    @Order(4)
    @DisplayName("Should handle multiple messages in sequence")
    void sendMultipleMessages_Success() throws Exception {
        BlockingQueue<ChatMessageDTO> receivedMessages = new ArrayBlockingQueue<>(10);

        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
        headers.add("Authorization", "Bearer " + senderToken);

        StompSession session = stompClient.connectAsync(
                wsUrl,
                headers,
                createStompHeaders(senderToken),
                new StompSessionHandlerAdapter() {
                }
        ).get(CONNECTION_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        session.subscribe("/user/queue/messages", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return ChatMessageDTO.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                if (!receivedMessages.offer((ChatMessageDTO) payload)) {
                    throw new IllegalStateException("Failed to add message to queue");
                }
            }
        });

        Thread.sleep(DISCONNECT_WAIT_MILLIS);

        for (int i = 1; i <= 3; i++) {
            ChatMessageDTO message = ChatMessageDTO.builder()
                    .id(null)
                    .senderId(senderId)
                    .recipientId(recipientId)
                    .listingId(listingId)
                    .content("Message " + i)
                    .senderName(null)
                    .listingTitle(null)
                    .timestamp(LocalDateTime.now())
                    .build();

            session.send("/app/chat.send", message);
            Thread.sleep(MESSAGE_DELAY_MILLIS);
        }

        int receivedCount = 0;
        for (int i = 0; i < 3; i++) {
            ChatMessageDTO received = receivedMessages.poll(2, TimeUnit.SECONDS);
            if (received != null) {
                receivedCount++;
            }
        }

        assertThat(receivedCount).isGreaterThanOrEqualTo(3);
        assertThat(messageRepository.count()).isGreaterThanOrEqualTo(3);

        session.disconnect();
    }

    @Test
    @Order(5)
    @DisplayName("Should handle typing notification")
    void typingNotification_Success() throws Exception {
        BlockingQueue<Object> receivedNotifications = new ArrayBlockingQueue<>(10);

        WebSocketHttpHeaders senderHeaders = new WebSocketHttpHeaders();
        senderHeaders.add("Authorization", "Bearer " + senderToken);

        WebSocketHttpHeaders recipientHeaders = new WebSocketHttpHeaders();
        recipientHeaders.add("Authorization", "Bearer " + recipientToken);

        StompSession recipientSession = stompClient.connectAsync(
                wsUrl,
                recipientHeaders,
                createStompHeaders(recipientToken),
                new StompSessionHandlerAdapter() {
                }
        ).get(CONNECTION_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        recipientSession.subscribe("/user/queue/typing", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Object.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                if (!receivedNotifications.offer(payload)) {
                    throw new IllegalStateException("Failed to add notification to queue");
                }
            }
        });

        StompSession senderSession = stompClient.connectAsync(
                wsUrl,
                senderHeaders,
                createStompHeaders(senderToken),
                new StompSessionHandlerAdapter() {
                }
        ).get(CONNECTION_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        Thread.sleep(SUBSCRIPTION_SETUP_MILLIS);

        ChatMessageDTO typingNotification = ChatMessageDTO.builder()
                .id(null)
                .senderId(senderId)
                .recipientId(recipientId)
                .listingId(listingId)
                .content("")
                .senderName(null)
                .listingTitle(null)
                .timestamp(LocalDateTime.now())
                .build();

        senderSession.send("/app/chat.typing", typingNotification);

        Object received = receivedNotifications.poll(MESSAGE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertThat(received).isNotNull();

        senderSession.disconnect();
        recipientSession.disconnect();
    }

    @Test
    @Order(6)
    @DisplayName("Should handle disconnection gracefully")
    void disconnect_Success() throws Exception {
        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
        headers.add("Authorization", "Bearer " + senderToken);

        StompSession session = stompClient.connectAsync(
                wsUrl,
                headers,
                createStompHeaders(senderToken),
                new StompSessionHandlerAdapter() {
                }
        ).get(CONNECTION_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        assertThat(session.isConnected()).isTrue();

        session.disconnect();
        Thread.sleep(DISCONNECT_WAIT_MILLIS);

        assertThat(session.isConnected()).isFalse();
    }

    @Test
    @Order(7)
    @DisplayName("Should block WebSocket messages after rate limit exceeded")
    void testWebSocketRateLimit() throws Exception {

        rateLimitInterceptor.clearBuckets();
        messageRepository.deleteAll();

        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
        headers.add("Authorization", "Bearer " + senderToken);

        StompSession session = stompClient.connectAsync(
                wsUrl,
                headers,
                createStompHeaders(senderToken),
                new StompSessionHandlerAdapter() {}
        ).get(CONNECTION_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        BlockingQueue<ChatMessageDTO> receivedMessages = new LinkedBlockingQueue<>();

        session.subscribe("/user/queue/messages", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return ChatMessageDTO.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                receivedMessages.offer((ChatMessageDTO) payload);
            }
        });

        Thread.sleep(SUBSCRIPTION_SETUP_MILLIS);


        for (int i = 0; i < 30; i++) {
            ChatMessageDTO message = ChatMessageDTO.builder()
                    .senderId(senderId)
                    .recipientId(recipientId)
                    .listingId(listingId)
                    .content("Rate limit test " + i)
                    .timestamp(LocalDateTime.now())
                    .build();

            session.send("/app/chat.send", message);
            Thread.sleep(50);
        }

        Thread.sleep(3000);

        long savedMessagesAfter30 = messageRepository.count();
        assertThat(savedMessagesAfter30).isEqualTo(30L);


        ChatMessageDTO blockedMessage = ChatMessageDTO.builder()
                .senderId(senderId)
                .recipientId(recipientId)
                .listingId(listingId)
                .content("This should be rate limited")
                .timestamp(LocalDateTime.now())
                .build();

        session.send("/app/chat.send", blockedMessage);
        Thread.sleep(2000);


        long finalSavedMessages = messageRepository.count();
        assertThat(finalSavedMessages).isEqualTo(30L);

        session.disconnect();
    }
}
