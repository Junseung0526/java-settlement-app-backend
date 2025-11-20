package com.nppang.backend.service;

import com.google.firebase.database.*;
import com.nppang.backend.dto.SignUpRequest;
import com.nppang.backend.entity.AppUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private FirebaseDatabase firebaseDatabase;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private DatabaseReference usersRef;

    @Mock
    private DatabaseReference lastUserIdRef;

    @Mock
    private DatabaseReference metadataRef;

    @Mock
    private DataSnapshot nicknameSnapshot;

    @Mock
    private DataSnapshot usernameSnapshot;

    @Mock
    private MutableData mutableData;

    @BeforeEach
    void setUp() {
        // Mock the database reference chain
        when(firebaseDatabase.getReference("users")).thenReturn(usersRef);
        when(usersRef.orderByChild(anyString())).thenReturn(usersRef);
        when(usersRef.equalTo(anyString())).thenReturn(usersRef);
    }

    @Test
    void registerUser_Success() throws ExecutionException, InterruptedException {
        // Given
        SignUpRequest request = new SignUpRequest();
        request.setUsername("testuser");
        request.setNickname("testnick");
        request.setPassword("password");

        // 1. Mock nickname check (not exists)
        when(nicknameSnapshot.exists()).thenReturn(false);
        doAnswer(invocation -> {
            ValueEventListener listener = invocation.getArgument(0);
            listener.onDataChange(nicknameSnapshot);
            return null;
        }).when(usersRef).addListenerForSingleValueEvent(any(ValueEventListener.class));

        // 2. Mock username check (not exists)
        when(usernameSnapshot.exists()).thenReturn(false);
        // Chain the mocks for the two separate listener calls
        doAnswer(invocation -> { // First call for nickname
            ValueEventListener listener = invocation.getArgument(0);
            listener.onDataChange(nicknameSnapshot);
            return null;
        }).doAnswer(invocation -> { // Second call for username
            ValueEventListener listener = invocation.getArgument(0);
            listener.onDataChange(usernameSnapshot);
            return null;
        }).when(usersRef).addListenerForSingleValueEvent(any(ValueEventListener.class));


        // 3. Mock transaction for ID generation
        when(firebaseDatabase.getReference("metadata")).thenReturn(metadataRef);
        when(metadataRef.child("lastUserId")).thenReturn(lastUserIdRef);
        when(mutableData.getValue(Long.class)).thenReturn(1L);
        doAnswer(invocation -> {
            Transaction.Handler handler = invocation.getArgument(0);
            handler.doTransaction(mutableData);
            // Simulate successful transaction completion
            DataSnapshot completionSnapshot = mock(DataSnapshot.class);
            when(completionSnapshot.getValue(Long.class)).thenReturn(2L);
            handler.onComplete(null, true, completionSnapshot);
            return null;
        }).when(lastUserIdRef).runTransaction(any(Transaction.Handler.class));

        // 4. Mock password encoding
        when(passwordEncoder.encode("password")).thenReturn("encodedPassword");

        // 5. Mock user creation setValue call
        DatabaseReference newUserRef = mock(DatabaseReference.class);
        when(usersRef.child("2")).thenReturn(newUserRef);

        // When
        CompletableFuture<AppUser> future = userService.registerUser(request);
        AppUser result = future.get();

        // Then
        assertNotNull(result);
        assertEquals("2", result.getId());
        assertEquals("testuser", result.getUsername());
        assertEquals("testnick", result.getNickname());
        assertEquals("encodedPassword", result.getPassword());

        ArgumentCaptor<AppUser> userCaptor = ArgumentCaptor.forClass(AppUser.class);
        verify(newUserRef).setValue(userCaptor.capture(), any());
        assertEquals("testnick", userCaptor.getValue().getNickname());
    }

    @Test
    void registerUser_FailsWhenNicknameIsTaken() {
        // Given
        SignUpRequest request = new SignUpRequest();
        request.setUsername("testuser");
        request.setNickname("existingNick");
        request.setPassword("password");

        // Mock nickname check (exists)
        when(nicknameSnapshot.exists()).thenReturn(true);
        doAnswer(invocation -> {
            ValueEventListener listener = invocation.getArgument(0);
            listener.onDataChange(nicknameSnapshot);
            return null;
        }).when(usersRef).addListenerForSingleValueEvent(any(ValueEventListener.class));

        // When
        CompletableFuture<AppUser> future = userService.registerUser(request);

        // Then
        ExecutionException exception = assertThrows(ExecutionException.class, future::get);
        assertTrue(exception.getCause() instanceof RuntimeException);
        assertEquals("Nickname is already taken.", exception.getCause().getMessage());
    }

    @Test
    void registerUser_FailsWhenUsernameIsTaken() {
        // Given
        SignUpRequest request = new SignUpRequest();
        request.setUsername("existingUser");
        request.setNickname("newNick");
        request.setPassword("password");

        // Mock nickname check (not exists)
        when(nicknameSnapshot.exists()).thenReturn(false);
        // Mock username check (exists)
        when(usernameSnapshot.exists()).thenReturn(true);

        // Chain the mocks for the two separate listener calls
        doAnswer(invocation -> { // First call for nickname
            ValueEventListener listener = invocation.getArgument(0);
            listener.onDataChange(nicknameSnapshot);
            return null;
        }).doAnswer(invocation -> { // Second call for username
            ValueEventListener listener = invocation.getArgument(0);
            listener.onDataChange(usernameSnapshot);
            return null;
        }).when(usersRef).addListenerForSingleValueEvent(any(ValueEventListener.class));

        // When
        CompletableFuture<AppUser> future = userService.registerUser(request);

        // Then
        ExecutionException exception = assertThrows(ExecutionException.class, future::get);
        assertTrue(exception.getCause() instanceof RuntimeException);
        assertEquals("Username is already taken.", exception.getCause().getMessage());
    }
}
