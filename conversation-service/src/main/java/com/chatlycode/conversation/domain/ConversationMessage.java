package com.chatlycode.conversation.domain;

import java.time.Instant;

public record ConversationMessage(String id, String conversationId, MessageAuthor author, String content, Instant createdAt) {
}
