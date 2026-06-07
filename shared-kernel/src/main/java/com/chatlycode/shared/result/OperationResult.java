package com.chatlycode.shared.result;

import java.util.Objects;

public sealed interface OperationResult<T> permits OperationResult.Success, OperationResult.Failure {

    boolean isSuccess();

    record Success<T>(T value) implements OperationResult<T> {
        public Success {
            Objects.requireNonNull(value, "value");
        }

        @Override
        public boolean isSuccess() {
            return true;
        }
    }

    record Failure<T>(String message) implements OperationResult<T> {
        public Failure {
            if (message == null || message.isBlank()) {
                throw new IllegalArgumentException("Failure message must not be blank");
            }
        }

        @Override
        public boolean isSuccess() {
            return false;
        }
    }

    static <T> OperationResult<T> success(T value) {
        return new Success<>(value);
    }

    static <T> OperationResult<T> failure(String message) {
        return new Failure<>(message);
    }
}
