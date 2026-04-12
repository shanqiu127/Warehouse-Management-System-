package org.example.back.llm.exception;

public class LlmBudgetExceededException extends RuntimeException {

    public LlmBudgetExceededException(String message) {
        super(message);
    }
}