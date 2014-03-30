package com.google.bitcoin.examples;

import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.kits.WalletAppKit;
import com.google.bitcoin.params.MainNetParams;
import com.google.bitcoin.params.RegTestParams;
import com.google.bitcoin.params.TestNet3Params;
import com.google.bitcoin.utils.BriefLogFormatter;
import java.io.File;

/**
 *
 * @author test2
 */
public class BloomFilterTest {
    
    @SuppressWarnings("unused")
	private static WalletAppKit kit;    
    

    public static void main(String[] args) throws Exception {
        // This line makes the log output more compact and easily read, especially when using the JDK log adapter.
        BriefLogFormatter.init();
        
        NetworkParameters params;
        String filePrefix;
        
        params = MainNetParams.get();
        
        filePrefix = "bloomfilter-test-service";
     
        kit = new WalletAppKit(params, new File("."), filePrefix);
        
        kit.start();
        
        
        try {
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException ignored) {}        
        
    }
    
}
