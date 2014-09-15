package com.google.mediterraneancoin.wallet;

import com.google.mediterraneancoin.core.ECKey;
import com.google.mediterraneancoin.wallet.KeyChainEventListener;

import java.util.List;

public class AbstractKeyChainEventListener implements KeyChainEventListener {
    @Override
    public void onKeysAdded(List<ECKey> keys) {
    }
}
