package eu.siacs.conversations.crypto.sasl;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

import eu.siacs.conversations.entities.Account;

public class ScramSha256Plus extends ScramPlusMechanism {

    public static final String MECHANISM = "SCRAM-SHA-256-PLUS";

    public ScramSha256Plus(final Account account, final ChannelBinding channelBinding) {
        super(account, channelBinding);
    }

    @Override
    protected HashFunction getHMac(final byte[] key) {
        return (key == null || key.length == 0)
                ? Hashing.hmacSha256(EMPTY_KEY)
                : Hashing.hmacSha256(key);
    }

    @Override
    protected HashFunction getDigest() {
        return Hashing.sha256();
    }

    @Override
    public int getPriority() {
        return 40 + ChannelBinding.priority(this.channelBinding);
    }

    @Override
    public String getMechanism() {
        return MECHANISM;
    }
}
