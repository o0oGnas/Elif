package xyz.gnas.elif.app.events.operation;

import xyz.gnas.elif.app.models.Operation;

public class InitialiseOperationEvent extends OperationEvent {
    public InitialiseOperationEvent(Operation operation) {
        super(operation);
    }
}
