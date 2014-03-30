package com.google.bitcoin.examples;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import com.google.bitcoin.core.BlockChain;
import com.google.bitcoin.core.CheckpointManager;
import com.google.bitcoin.core.DownloadListener;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.PeerAddress;
import com.google.bitcoin.core.PeerEventListener;
import com.google.bitcoin.core.PeerGroup;
import com.google.bitcoin.core.Wallet;
import com.google.bitcoin.kits.WalletAppKit;
import com.google.bitcoin.net.discovery.DnsDiscovery;
import com.google.bitcoin.params.MainNetParams;
import com.google.bitcoin.store.BlockStoreException;
import com.google.bitcoin.store.SPVBlockStore;
import com.google.bitcoin.store.WalletProtobufSerializer;
import com.google.bitcoin.utils.BriefLogFormatter;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.Service.State;

public class BloomFilterTest2 extends AbstractIdleService  {
	
    protected final String filePrefix;
    protected final NetworkParameters params;
    protected volatile BlockChain vChain;
    protected volatile SPVBlockStore vStore;
    protected volatile Wallet vWallet;
    protected volatile PeerGroup vPeerGroup;

    protected final File directory;
    protected volatile File vWalletFile;

    protected boolean useAutoSave = true;
    protected PeerAddress[] peerAddresses;
    protected PeerEventListener downloadListener;
    protected boolean autoStop = true;
    protected InputStream checkpoints;
    protected boolean blockingStartup = true;
    protected String userAgent, version;
	

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
        // This line makes the log output more compact and easily read, especially when using the JDK log adapter.
        BriefLogFormatter.init();
        
        NetworkParameters params;
        String filePrefix;
        
        params = MainNetParams.get();
        
        filePrefix = "bloomfilter-test2-service";
        
        BloomFilterTest2 kit;
        
 
     
        kit = new BloomFilterTest2(params, new File("."), filePrefix);
        
        kit.start();
        
        
        try {
            Thread.sleep(3600 * 1000L);
        } catch (InterruptedException ignored) {}   		
		

	}
	
    public BloomFilterTest2(NetworkParameters params, File directory, String filePrefix) {
        this.params = checkNotNull(params);
        this.directory = checkNotNull(directory);
        this.filePrefix = checkNotNull(filePrefix);
    }	


    @Override
    protected void startUp() throws Exception {
        // Runs in a separate thread.
        if (!directory.exists()) {
            if (!directory.mkdir()) {
                throw new IOException("Could not create named directory.");
            }
        }
        FileInputStream walletStream = null;
        try {
            File chainFile = new File(directory, filePrefix + ".spvchain");
            boolean chainFileExists = chainFile.exists();
            vWalletFile = new File(directory, filePrefix + ".wallet");
            boolean shouldReplayWallet = vWalletFile.exists() && !chainFileExists;

            vStore = new SPVBlockStore(params, chainFile);
            
            
            File checkpointsFile = new File("checkpoints");
            
            if (checkpointsFile.exists()) {
                    // Replace path to the file here.
            	checkpoints = new FileInputStream(checkpointsFile);
                //CheckpointManager.checkpoint(params, stream, store, wallet.getEarliestKeyCreationTime());
            }               
            
            
            if (!chainFileExists && checkpoints != null) {
                // Ugly hack! We have to create the wallet once here to learn the earliest key time, and then throw it
                // away. The reason is that wallet extensions might need access to peergroups/chains/etc so we have to
                // create the wallet later, but we need to know the time early here before we create the BlockChain
                // object.
                long time = Long.MAX_VALUE;
                if (vWalletFile.exists()) {
                    Wallet wallet = new Wallet(params);
                    FileInputStream stream = new FileInputStream(vWalletFile);
                    new WalletProtobufSerializer().readWallet(WalletProtobufSerializer.parseToProto(stream), wallet);
                    time = wallet.getEarliestKeyCreationTime();
                    
                    System.out.println("earliest key creation time: " + new Date(time));
                    
                }
                CheckpointManager.checkpoint(params, checkpoints, vStore, time);
            }
            vChain = new BlockChain(params, vStore);
            vPeerGroup = createPeerGroup();
            
            
            // https://code.google.com/p/bitcoinj/source/browse/SpeedingUpChainSync.wiki?repo=wiki
            	
            vPeerGroup.setFastCatchupTimeSecs( System.currentTimeMillis() - 1000 * 60 );
            
            
            
            if (this.userAgent != null)
                vPeerGroup.setUserAgent(userAgent, version);
            if (vWalletFile.exists()) {
                walletStream = new FileInputStream(vWalletFile);
                vWallet = new Wallet(params);
                addWalletExtensions(); // All extensions must be present before we deserialize
                new WalletProtobufSerializer().readWallet(WalletProtobufSerializer.parseToProto(walletStream), vWallet);
                if (shouldReplayWallet)
                    vWallet.clearTransactions(0);
            } else {
                vWallet = new Wallet(params);
                vWallet.addKey(new ECKey());
                addWalletExtensions();
            }
            if (useAutoSave) vWallet.autosaveToFile(vWalletFile, 1, TimeUnit.SECONDS, null);
            // Set up peer addresses or discovery first, so if wallet extensions try to broadcast a transaction
            // before we're actually connected the broadcast waits for an appropriate number of connections.
            if (peerAddresses != null) {
                for (PeerAddress addr : peerAddresses) vPeerGroup.addAddress(addr);
                peerAddresses = null;
            } else {
                vPeerGroup.addPeerDiscovery(new DnsDiscovery(params));
            }
            vChain.addWallet(vWallet);
            vPeerGroup.addWallet(vWallet);
            onSetupCompleted();

            if (blockingStartup) {
                vPeerGroup.startAndWait();
                // Make sure we shut down cleanly.
                installShutdownHook();
                // TODO: Be able to use the provided download listener when doing a blocking startup.
                final DownloadListener listener = new DownloadListener();
                vPeerGroup.startBlockChainDownload(listener);
                listener.await();
            } else {
                Futures.addCallback(vPeerGroup.start(), new FutureCallback<State>() {
                    @Override
                    public void onSuccess(State result) {
                        final PeerEventListener l = downloadListener == null ? new DownloadListener() : downloadListener;
                        vPeerGroup.startBlockChainDownload(l);
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        throw new RuntimeException(t);
                    }
                });
            }
        } catch (BlockStoreException e) {
            throw new IOException(e);
        } finally {
            if (walletStream != null) walletStream.close();
        }
    }
    
    protected PeerGroup createPeerGroup() {
        return new PeerGroup(params, vChain);
    }

    /**
     * This method is invoked on a background thread after all objects are initialised, but before the peer group
     * or block chain download is started. You can tweak the objects configuration here.
     */
    protected void onSetupCompleted() { }    

	@Override
	protected void shutDown() throws Exception {
        // Runs in a separate thread.
        try {
            vPeerGroup.stopAndWait();
            vWallet.saveToFile(vWalletFile);
            vStore.close();

            vPeerGroup = null;
            vWallet = null;
            vStore = null;
            vChain = null;
        } catch (BlockStoreException e) {
            throw new IOException(e);
        }
		
	}
	
    @SuppressWarnings("unused")
	private void installShutdownHook() {
        if (autoStop) Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override public void run() {
                try {
                	BloomFilterTest2.this.stopAndWait();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }	
    
    /**
     * <p>Override this to load all wallet extensions if any are necessary.</p>
     *
     * <p>When this is called, chain(), store(), and peerGroup() will return the created objects, however they are not
     * initialized/started</p>
     */
    protected void addWalletExtensions() throws Exception { }    

}
