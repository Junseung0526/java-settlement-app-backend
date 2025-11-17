package com.nppang.backend.service;

import com.google.firebase.database.DatabaseReference;
import com.nppang.backend.dto.CreateInvitationRequest;
import com.nppang.backend.entity.AppUser;
import com.nppang.backend.entity.Invitation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvitationServiceTest {

    @InjectMocks
    private InvitationService invitationService;

    @Mock
    private UserService userService;

    @Mock
    private FirebaseDatabase firebaseDatabase;

    @Mock
    private DatabaseReference invitationsRef;

    @BeforeEach
    void setUp() {
        when(firebaseDatabase.getReference("invitations")).thenReturn(invitationsRef);
    }

    @Test
    void createInvitation_Success() throws ExecutionException, InterruptedException {
        // Given
        CreateInvitationRequest request = new CreateInvitationRequest();
        request.setGroupId("test-group");
        request.setInviterId("inviter-id");
        request.setInviteeNickname("invitee-nick");

        AppUser invitee = new AppUser();
        invitee.setId("invitee-id");
        invitee.setNickname("invitee-nick");

        when(userService.findUserByNickname("invitee-nick")).thenReturn(CompletableFuture.completedFuture(invitee));

        DatabaseReference newInvitationRef = mock(DatabaseReference.class);
        when(invitationsRef.child(anyString())).thenReturn(newInvitationRef);

        // When
        CompletableFuture<Invitation> future = invitationService.createInvitation(request);
        Invitation result = future.get();

        // Then
        assertNotNull(result);
        assertEquals("test-group", result.getGroupId());
        assertEquals("inviter-id", result.getInviterId());
        assertEquals("invitee-id", result.getInviteeId()); // Assert the ID is used

        ArgumentCaptor<Invitation> invitationCaptor = ArgumentCaptor.forClass(Invitation.class);
        verify(newInvitationRef).setValue(invitationCaptor.capture(), any());
        assertEquals("invitee-id", invitationCaptor.getValue().getInviteeId());
    }

    @Test
    void createInvitation_FailsWhenUserNotFound() {
        // Given
        CreateInvitationRequest request = new CreateInvitationRequest();
        request.setInviteeNickname("non-existent-nick");

        when(userService.findUserByNickname("non-existent-nick")).thenReturn(CompletableFuture.completedFuture(null));

        // When
        CompletableFuture<Invitation> future = invitationService.createInvitation(request);

        // Then
        ExecutionException exception = assertThrows(ExecutionException.class, future::get);
        assertTrue(exception.getCause() instanceof RuntimeException);
        assertEquals("User with nickname 'non-existent-nick' not found.", exception.getCause().getMessage());
    }
}
