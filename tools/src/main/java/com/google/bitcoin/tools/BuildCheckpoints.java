/*
 * Copyright 2013 Google Inc.
 * Copyright 2014 Andreas Schildbach
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.bitcoin.tools;

import com.google.mediterraneancoin.core.*;
import com.google.mediterraneancoin.params.MainNetParams;
import com.google.mediterraneancoin.store.BlockStore;
import com.google.mediterraneancoin.store.MemoryBlockStore;
import com.google.mediterraneancoin.utils.BriefLogFormatter;
import com.google.mediterraneancoin.utils.Threading;
import com.google.common.base.Charsets;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.TreeMap;

import static com.google.common.base.Preconditions.checkState;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Downloads and verifies a full chain from your local peer, emitting checkpoints at each difficulty transition period
 * to a file which is then signed with your key.
 */
public class BuildCheckpoints {
 
	
	public static File findMostRecentCheckpointFile(File folder) {	
		
		long timestamp = 0;
	 
		File[] listOfFiles = folder.listFiles();	
		
		File result = null;
		
		for (int i = 0; i < listOfFiles.length; i++) {
			
			File f = listOfFiles[i];
			
			if (f.isDirectory() || !f.getName().startsWith("checkpoints_new_"))
				continue;
			
			if (f.lastModified() >= timestamp) {
				result = f;
				timestamp = f.lastModified();
			}
			
		}
		
		return result;
		
	}
	
 

    private static final NetworkParameters PARAMS = MainNetParams.get();
    private static final File PLAIN_CHECKPOINTS_FILE = new File("checkpoints");
    private static final File TEXTUAL_CHECKPOINTS_FILE = new File("checkpoints.txt");
 
    private static BlockStore store;
    private static PeerGroup peerGroup;
    
    public static void main(String[] args) throws Exception {
        BriefLogFormatter.init();
 
        // Sorted map of UNIX time of block to StoredBlock object.
        final TreeMap<Integer, StoredBlock> checkpoints = new TreeMap<Integer, StoredBlock>();        

        // Configure bitcoinj to fetch only headers, not save them to disk, connect to a local fully synced/validated
        // node and to save block headers that are on interval boundaries, as long as they are <1 hour old.
        store = new MemoryBlockStore(PARAMS);
        
        long now = new Date().getTime() / 1000;
        
        File checkpointsFile =  findMostRecentCheckpointFile(new File("/opt/checkpoints/"));
        		//new File("/opt/checkpoints");
        
        System.out.println("checkpoint file: " + checkpointsFile);
        
        if (checkpointsFile.exists()) {
                // Replace path to the file here.
            FileInputStream stream = new FileInputStream(checkpointsFile);
            CheckpointManager.checkpoint(PARAMS, stream, store, now);
        }
        
        
        final BlockChain chain = new BlockChain(PARAMS, store);
        peerGroup = new PeerGroup(PARAMS, chain);
        //peerGroup.addAddress(/*InetAddress.getLocalHost()*/  InetAddress.getByName("192.168.0.50") );
        
        peerGroup.addAddress(/*InetAddress.getLocalHost()*/  InetAddress.getByName("node1.mediterraneancoin.org") );
        peerGroup.addAddress(/*InetAddress.getLocalHost()*/  InetAddress.getByName("node2.mediterraneancoin.org") );
        peerGroup.addAddress(/*InetAddress.getLocalHost()*/  InetAddress.getByName("node3.mediterraneancoin.org") );
        peerGroup.addAddress(/*InetAddress.getLocalHost()*/  InetAddress.getByName("node4.mediterraneancoin.org") );
 
        /*
        // Sorted map of block height to StoredBlock object.
        final TreeMap<Integer, StoredBlock> checkpoints = new TreeMap<Integer, StoredBlock>();

        // Configure bitcoinj to fetch only headers, not save them to disk, connect to a local fully synced/validated
        // node and to save block headers that are on interval boundaries, as long as they are <1 month old.
        final BlockStore store = new MemoryBlockStore(PARAMS);
        final BlockChain chain = new BlockChain(PARAMS, store);
        final PeerGroup peerGroup = new PeerGroup(PARAMS, chain);
        peerGroup.addAddress(InetAddress.getLocalHost());
        long now = new Date().getTime() / 1000;
        */
 
        peerGroup.setFastCatchupTimeSecs(now);

        final long oneHourAgo = now - 3600;//(86400 * 1);
        
        
        //boolean chainExistedAlready = chainFile.exists();
        //blockStore = new SPVBlockStore(params, chainFile);
        

        
        

        chain.addListener(new AbstractBlockChainListener() {
            @Override
            public void notifyNewBestBlock(StoredBlock block) throws VerificationException {
                int height = block.getHeight();

                if (height % PARAMS.getInterval() == 0 && block.getHeader().getTimeSeconds() <= oneHourAgo) {

                //if (height % PARAMS.getInterval() == 0 && block.getHeader().getTimeSeconds() <= oneMonthAgo) {

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
                        } catch (Exception ex) {
                        Logger.getLogger(BuildCheckpoints.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    
                } else {
                	if (height % 100 == 0)
                		System.out.println("height: " + height );
                }
            }
        }, Threading.SAME_THREAD);

        peerGroup.startAsync();
        peerGroup.awaitRunning();
        peerGroup.downloadBlockChain();

        

        peerGroup.stopAndWait();
        store.close();
        
        checkpointsFile =  findMostRecentCheckpointFile(new File("/opt/checkpoints/"));

        // Sanity check the created file.
        CheckpointManager manager = new CheckpointManager(PARAMS, new FileInputStream(checkpointsFile));
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
    
    public static void writeCheckpointFile(File fileName , TreeMap<Integer, StoredBlock> checkpoints) throws NoSuchAlgorithmException, IOException, Exception {
    	
    	

        checkState(checkpoints.size() > 0);

        // Write checkpoint data out.

        //final FileOutputStream fileOutputStream = new FileOutputStream(fileName /*"/opt/checkpoints_new"*/, false);
        
        writeBinaryCheckpoints(checkpoints, PLAIN_CHECKPOINTS_FILE);
        writeTextualCheckpoints(checkpoints, TEXTUAL_CHECKPOINTS_FILE);

        peerGroup.stopAsync();
        peerGroup.awaitTerminated();
        store.close();

        // Sanity check the created files.
        sanityCheck(PLAIN_CHECKPOINTS_FILE, checkpoints.size());
        sanityCheck(TEXTUAL_CHECKPOINTS_FILE, checkpoints.size());
    }

    private static void writeBinaryCheckpoints(TreeMap<Integer, StoredBlock> checkpoints, File file) throws Exception {
        final FileOutputStream fileOutputStream = new FileOutputStream(file, false);
 
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
        System.out.println("Checkpoints written to '" + file.getCanonicalPath() + "'.");
    }

    private static void writeTextualCheckpoints(TreeMap<Integer, StoredBlock> checkpoints, File file) throws IOException {
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), Charsets.US_ASCII));
        writer.println("TXT CHECKPOINTS 1");
        writer.println("0"); // Number of signatures to read. Do this later.
        writer.println(checkpoints.size());
        ByteBuffer buffer = ByteBuffer.allocate(StoredBlock.COMPACT_SERIALIZED_SIZE);
        for (StoredBlock block : checkpoints.values()) {
            block.serializeCompact(buffer);
            writer.println(CheckpointManager.BASE64.encode(buffer.array()));
            buffer.position(0);
        }
        writer.close();
        System.out.println("Checkpoints written to '" + file.getCanonicalPath() + "'.");
    }

    private static void sanityCheck(File file, int expectedSize) throws IOException {
        CheckpointManager manager = new CheckpointManager(PARAMS, new FileInputStream(file));
        checkState(manager.numCheckpoints() == expectedSize);

        if (PARAMS.getId().equals(NetworkParameters.ID_MAINNET)) {
            StoredBlock test = manager.getCheckpointBefore(1390500000); // Thu Jan 23 19:00:00 CET 2014
            checkState(test.getHeight() == 280224);
            checkState(test.getHeader().getHashAsString()
                    .equals("00000000000000000b5d59a15f831e1c45cb688a4db6b0a60054d49a9997fa34"));
        } else if (PARAMS.getId().equals(NetworkParameters.ID_TESTNET)) {
            StoredBlock test = manager.getCheckpointBefore(1390500000); // Thu Jan 23 19:00:00 CET 2014
            checkState(test.getHeight() == 167328);
            checkState(test.getHeader().getHashAsString()
                    .equals("0000000000035ae7d5025c2538067fe7adb1cf5d5d9c31b024137d9090ed13a9"));
        }
 
    }
}
