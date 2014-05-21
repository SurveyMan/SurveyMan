package system;

import interstitial.BackendType;

public class UnknownBackendException extends Exception {
    public UnknownBackendException(BackendType bt) {
        super(String.format("Unknown backend type: %s", bt.toString()));
    }
}
