package fr.poubone.att2.client.sync;

import java.util.function.BooleanSupplier;

final class PartySyncRetry {
    static void publish(PartySyncRetry.Attempt attempt, PartySyncRetry.Delay delay, BooleanSupplier currentSession) throws Exception {
        if (currentSession.getAsBoolean()) {
            try {
                attempt.run();
            } catch (PartySyncHttp.ApiException failure) {
                if (failure.status != 429) {
                    throw failure;
                }

                delay.waitFor(failure.retryAfterMs);
                if (currentSession.getAsBoolean()) {
                    attempt.run();
                }
            }
        }
    }

    @FunctionalInterface
    interface Attempt {
        void run() throws Exception;
    }

    @FunctionalInterface
    interface Delay {
        void waitFor(long var1) throws InterruptedException;
    }
}
