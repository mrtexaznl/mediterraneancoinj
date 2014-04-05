package com.google.bitcoin.tools;

import com.google.bitcoin.core.*;
import com.google.bitcoin.params.MainNetParams;
import com.google.bitcoin.store.BlockStore;
import com.google.bitcoin.store.MemoryBlockStore;
import com.google.bitcoin.utils.BriefLogFormatter;
import com.google.bitcoin.utils.Threading;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.TreeMap;

import static com.google.common.base.Preconditions.checkState;

/**
 * Downloads and verifies a full chain from your local peer, emitting checkpoints at each difficulty transition period
 * to a file which is then signed with your key.
 */
public class BuildCheckpoints {
    public static void main(String[] args) throws Exception {
        BriefLogFormatter.init();
        final NetworkParameters params = MainNetParams.get();


        // Sorted map of UNIX time of block to StoredBlock object.
        final TreeMap<Integer, StoredBlock> checkpoints = new TreeMap<Integer, StoredBlock>();        

        // Configure bitcoinj to fetch only headers, not save them to disk, connect to a local fully synced/validated
        // node and to save block headers that are on interval boundaries, as long as they are <1 month old.
        final BlockStore store = new MemoryBlockStore(params);
        
        long now = new Date().getTime() / 1000;
        
        File checkpointsFile = new File("/opt/checkpoints");
        
        if (checkpointsFile.exists()) {
                // Replace path to the file here.
            FileInputStream stream = new FileInputStream(checkpointsFile);
            CheckpointManager.checkpoint(params, stream, store, now);
        }                
        
        
        final BlockChain chain = new BlockChain(params, store);
        final PeerGroup peerGroup = new PeerGroup(params, chain);
        peerGroup.addAddress(/*InetAddress.getLocalHost()*/  InetAddress.getByName("192.168.0.50") );
        
        peerGroup.addAddress(/*InetAddress.getLocalHost()*/  InetAddress.getByName("node1.mediterraneancoin.org") );
        peerGroup.addAddress(/*InetAddress.getLocalHost()*/  InetAddress.getByName("node2.mediterraneancoin.org") );
        peerGroup.addAddress(/*InetAddress.getLocalHost()*/  InetAddress.getByName("node3.mediterraneancoin.org") );
        peerGroup.addAddress(/*InetAddress.getLocalHost()*/  InetAddress.getByName("node4.mediterraneancoin.org") );
        
        peerGroup.setFastCatchupTimeSecs(now);

        final long oneMonthAgo = now - 3600;//(86400 * 1);
        
        
        //boolean chainExistedAlready = chainFile.exists();
        //blockStore = new SPVBlockStore(params, chainFile);
        

        
        

        chain.addListener(new AbstractBlockChainListener() {
            @Override
            public void notifyNewBestBlock(StoredBlock block) throws VerificationException {
                int height = block.getHeight();
                if (height % params.getInterval() == 0 && block.getHeader().getTimeSeconds() <= oneMonthAgo) {
                    System.out.println(String.format("Checkpointing block %s at height %d",
                            block.getHeader().getHash(), block.getHeight()));
                    checkpoints.put(height, block);
                    
                    try {
						writeCheckpointFile(new File("/opt/checkpoints_new_" + height), checkpoints);
					} catch (NoSuchAlgorithmException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
                    
                } else {
                	if (height % 100 == 0)
                		System.out.println("height: " + height );
                }
            }
        }, Threading.SAME_THREAD);

        peerGroup.startAndWait();
        peerGroup.downloadBlockChain();

        

        peerGroup.stopAndWait();
        store.close();

        // Sanity check the created file.
        CheckpointManager manager = new CheckpointManager(params, new FileInputStream("/opt/checkpoints"));
        checkState(manager.numCheckpoints() == checkpoints.size());
        StoredBlock test = manager.getCheckpointBefore(/*1348310800*/ System.currentTimeMillis() - 1000 * 3600 * 24L * 2);  // Just after block 200,000
        
        System.out.println();
        System.out.println();
        
        System.out.println(test);
        
        System.out.println();
        System.out.println();
        
        System.out.println(test.getHeight());
        
        System.out.println();
        System.out.println();
        
        System.out.println(test.getHeader().getHashAsString());
        
        
        //checkState(test.getHeight() == 199584);
        //checkState(test.getHeader().getHashAsString().equals("000000000000002e00a243fe9aa49c78f573091d17372c2ae0ae5e0f24f55b52"));
    }
    
    public static void writeCheckpointFile(File fileName , TreeMap<Integer, StoredBlock> checkpoints) throws NoSuchAlgorithmException, IOException {
    	
    	

        checkState(checkpoints.size() > 0);

        // Write checkpoint data out.
        final FileOutputStream fileOutputStream = new FileOutputStream(fileName /*"/opt/checkpoints_new"*/, false);
        
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        
        final DigestOutputStream digestOutputStream = new DigestOutputStream(fileOutputStream, digest);
        
        digestOutputStream.on(false);
        
        final DataOutputStream dataOutputStream = new DataOutputStream(digestOutputStream);
        
        dataOutputStream.writeBytes("CHECKPOINTS 1");
        dataOutputStream.writeInt(0);  // Number of signatures to read. Do this later.
        digestOutputStream.on(true);
        dataOutputStream.writeInt(checkpoints.size());
        ByteBuffer buffer = ByteBuffer.allocate(StoredBlock.COMPACT_SERIALIZED_SIZE);
        for (StoredBlock block : checkpoints.values()) {
            block.serializeCompact(buffer);
            dataOutputStream.write(buffer.array());
            buffer.position(0);
        }
        dataOutputStream.close();
        Sha256Hash checkpointsHash = new Sha256Hash(digest.digest());
        System.out.println("Hash of checkpoints data is " + checkpointsHash);
        digestOutputStream.close();
        fileOutputStream.close();    	
    	
    }
}
