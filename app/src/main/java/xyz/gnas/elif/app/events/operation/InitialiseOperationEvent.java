package xyz.gnas.elif.app.events.operation;

import xyz.gnas.elif.core.models.Operation;

public class InitialiseOperationEvent {
    private Operation operation;

    public Operation getOperation() {
        return operation;
    }

    public InitialiseOperationEvent(Operation operation) {
        this.operation = operation;
    }
}
