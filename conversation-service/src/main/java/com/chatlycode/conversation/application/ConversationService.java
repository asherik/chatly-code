package com.chatlycode.conversation.application;

import com.chatlycode.conversation.domain.ConversationMessage;
import com.chatlycode.conversation.domain.MessageAuthor;
import com.chatlycode.shared.id.Ids;
import com.chatlycode.shared.time.ClockProvider;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class ConversationService {

    private final ClockProvider clock;
    private final ConcurrentMap<String, List<ConversationMessage>> messages = new ConcurrentHashMap<>();

    public ConversationService(ClockProvider clock) {
        this.clock = clock;
    }

    public ConversationMessage append(String conversationId, MessageAuthor author, String content) {
        if (conversationId == null || conversationId.isBlank()) {
            throw new IllegalArgumentException("Conversation id must not be blank");
        }
        ConversationMessage message = new ConversationMessage(Ids.newId("msg"), conversationId, author, content, clock.now());
        messages.computeIfAbsent(conversationId, ignored -> new CopyOnWriteArrayList<>()).add(message);
        return message;
    }

    public List<ConversationMessage> history(String conversationId) {
        return List.copyOf(messages.getOrDefault(conversationId, List.of()));
    }
}
