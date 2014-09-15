package com.google.mediterraneancoin.core;

import com.google.mediterraneancoin.core.NetworkParameters;
import com.google.mediterraneancoin.store.BlockStoreException;
import com.google.mediterraneancoin.store.FullPrunedBlockStore;
import com.google.mediterraneancoin.store.MemoryFullPrunedBlockStore;

/**
 * A MemoryStore implementation of the FullPrunedBlockStoreTest
 */
public class MemoryFullPrunedBlockChainTest extends AbstractFullPrunedBlockChainTest
{
    @Override
    public FullPrunedBlockStore createStore(NetworkParameters params, int blockCount) throws BlockStoreException
    {
        return new MemoryFullPrunedBlockStore(params, blockCount);
    }

    @Override
    public void resetStore(FullPrunedBlockStore store) throws BlockStoreException
    {
        //No-op for memory store, because it's not persistent
    }
}
