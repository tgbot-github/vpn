package com.hivpn.server.repository;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.hivpn.server.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Repository
@RequiredArgsConstructor
public class UserRepository {
    private final FirebaseDatabase firebaseDatabase;
    private static final String USERS_PATH = "users";

    public CompletableFuture<User> save(User user) {
        CompletableFuture<User> future = new CompletableFuture<>();
        DatabaseReference usersRef = firebaseDatabase.getReference(USERS_PATH);
        DatabaseReference userRef = usersRef.child(user.getId());
        
        log.info("Saving user to Firebase: {}", user);
        
        userRef.setValue(user, (error, ref) -> {
            if (error != null) {
                log.error("Error saving user: {}", error.getMessage());
                future.completeExceptionally(error.toException());
            } else {
                log.info("User saved successfully: {}", user);
                future.complete(user);
            }
        });
        
        return future;
    }

    public CompletableFuture<Optional<User>> findByTelegramId(Long telegramId) {
        CompletableFuture<Optional<User>> future = new CompletableFuture<>();
        DatabaseReference usersRef = firebaseDatabase.getReference(USERS_PATH);
        
        log.info("Searching for user with telegramId: {}", telegramId);
        
        usersRef.orderByChild("telegramId")
                .equalTo(telegramId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            for (DataSnapshot child : snapshot.getChildren()) {
                                User user = child.getValue(User.class);
                                log.info("Found user by telegramId: {}", user);
                                future.complete(Optional.ofNullable(user));
                                return;
                            }
                        }
                        log.info("No user found with telegramId: {}", telegramId);
                        future.complete(Optional.empty());
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        log.error("Error getting user by telegramId: {}", error.getMessage());
                        future.completeExceptionally(error.toException());
                    }
                });
        
        return future;
    }

    public CompletableFuture<Optional<User>> findByAccessToken(String accessToken) {
        CompletableFuture<Optional<User>> future = new CompletableFuture<>();
        DatabaseReference usersRef = firebaseDatabase.getReference(USERS_PATH);
        
        log.info("Searching for user with accessToken: {}", accessToken);
        
        usersRef.orderByChild("accessToken")
                .equalTo(accessToken)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            for (DataSnapshot child : snapshot.getChildren()) {
                                User user = child.getValue(User.class);
                                log.info("Found user by accessToken: {}", user);
                                future.complete(Optional.ofNullable(user));
                                return;
                            }
                        }
                        log.info("No user found with accessToken: {}", accessToken);
                        future.complete(Optional.empty());
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        log.error("Error getting user by accessToken: {}", error.getMessage());
                        future.completeExceptionally(error.toException());
                    }
                });
        
        return future;
    }

    public CompletableFuture<Void> deleteUser(User user) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        
        try {
            if (user.getId() == null) {
                future.completeExceptionally(new IllegalArgumentException("User ID is null"));
                return future;
            }

            DatabaseReference usersRef = firebaseDatabase.getReference(USERS_PATH);
            usersRef.child(user.getId()).removeValue((error, ref) -> {
                if (error != null) {
                    log.error("Error deleting user: {}", error.getMessage());
                    future.completeExceptionally(error.toException());
                } else {
                    log.info("User deleted successfully");
                    future.complete(null);
                }
            });
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        
        return future;
    }
} 